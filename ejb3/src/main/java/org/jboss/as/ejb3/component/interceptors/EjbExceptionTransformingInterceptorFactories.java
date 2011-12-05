/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.ejb3.component.interceptors;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRequiredException;
import javax.ejb.EJBTransactionRolledbackException;
import javax.ejb.NoSuchEJBException;
import javax.ejb.NoSuchEntityException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.TransactionRequiredLocalException;
import javax.ejb.TransactionRolledbackLocalException;
import javax.transaction.TransactionRequiredException;
import javax.transaction.TransactionRolledbackException;

import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * An interceptor that transforms EJB 3.0 business interface exceptions to EJB 2.x exceptions when required.
 * <p/>
 * This allows us to keep the actual
 *
 * @author Stuart Douglas
 */
public class EjbExceptionTransformingInterceptorFactories {

    /**
     * We need to return a CreateException to the client.
     * <p/>
     * Rather than forcing all create exceptions everywhere to propagate, and generally making a mess, we stash
     * the exception here, and then re-throw it from the exception transforming interceptor.
     */
    private static final ThreadLocal<CreateException> CREATE_EXCEPTION = new ThreadLocal<CreateException>();

    public static final InterceptorFactory REMOTE_INSTANCE = new ImmediateInterceptorFactory(new Interceptor() {
        @Override
        public Object processInvocation(final InterceptorContext context) throws Exception {
            try {
                return context.proceed();
            } catch (EJBTransactionRequiredException e) {
                throw new TransactionRequiredException(e.getMessage());
            } catch (EJBTransactionRolledbackException e) {
                throw new TransactionRolledbackException(e.getMessage());
            } catch (NoSuchEJBException e) {
                throw new NoSuchObjectException(e.getMessage());
            } catch (NoSuchEntityException e) {
                throw new NoSuchObjectException(e.getMessage());
            } catch (EJBException e) {
                //as the create exception is not propagated the init method interceptor just stashes it in a ThreadLocal
                CreateException createException = popCreateException();
                if (createException != null) {
                    throw createException;
                }
                throw new RemoteException("Invocation failed", e);
            }
        }
    });

    public static final InterceptorFactory LOCAL_INSTANCE = new ImmediateInterceptorFactory(new Interceptor() {
        @Override
        public Object processInvocation(final InterceptorContext context) throws Exception {
            try {
                return context.proceed();
            } catch (EJBTransactionRequiredException e) {
                throw new TransactionRequiredLocalException(e.getMessage());
            } catch (EJBTransactionRolledbackException e) {
                throw new TransactionRolledbackLocalException(e.getMessage());
            } catch (NoSuchEJBException e) {
                throw new NoSuchObjectLocalException(e.getMessage());
            } catch (NoSuchEntityException e) {
                throw new NoSuchObjectLocalException(e.getMessage());
            } catch (EJBException e) {
                CreateException createException = popCreateException();
                if (createException != null) {
                    throw createException;
                }
                throw e;
            }
        }
    });

    public static void setCreateException(CreateException exception) {
        CREATE_EXCEPTION.set(exception);
    }

    public static CreateException popCreateException() {
        try {
            return CREATE_EXCEPTION.get();
        } finally {
            CREATE_EXCEPTION.remove();
        }
    }

}
