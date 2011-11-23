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
            }
        }
    });

}
