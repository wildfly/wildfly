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
package org.jboss.as.ejb3.component.entity.interceptors;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

import javax.ejb.EJBException;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.Handle;
import javax.ejb.RemoveException;
import java.rmi.RemoteException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Interceptor that handles the {@link javax.ejb.EJBLocalObject#isIdentical(javax.ejb.EJBLocalObject)}
 * && {@link javax.ejb.EJBObject#isIdentical(javax.ejb.EJBObject)} methods
 *
 * @author Stuart Douglas
 */
public class EntityIsIdenticalInterceptorFactory implements InterceptorFactory {

    private final Object primaryKeyContextKey;

    public EntityIsIdenticalInterceptorFactory(Object primaryKeyContextKey) {
        this.primaryKeyContextKey = primaryKeyContextKey;
    }

    @Override
    public Interceptor create(final InterceptorFactoryContext context) {
        AtomicReference<Object> sessionIdReference = (AtomicReference<Object>) context.getContextData().get(this.primaryKeyContextKey);
        final ComponentView componentView = (ComponentView) context.getContextData().get(ComponentView.class);
        return new EntityIsIdenticalInterceptor(componentView, sessionIdReference);

    }

    private class EntityIsIdenticalInterceptor implements Interceptor {

        private final ComponentView componentView;
        final AtomicReference<Object> primaryKeyReference;

        public EntityIsIdenticalInterceptor(final ComponentView componentView, final AtomicReference<Object> primaryKeyReference) {
            this.componentView = componentView;
            this.primaryKeyReference = primaryKeyReference;
        }

        @Override
        public Object processInvocation(final InterceptorContext context) throws Exception {
            final Object other = context.getParameters()[0];
            final Class<?> proxyType = componentView.getProxyClass();
            if (proxyType.isAssignableFrom(other.getClass())) {
                //now we know that this is an ejb for the correct component view
                //as digging out the session id from the proxy object is not really
                //a viable option, we invoke equals() for the other instance with a
                //SessionIdHolder as the other side
                if(other instanceof EJBLocalObject) {
                    return ((EJBLocalObject)other).isIdentical(new SessionIdHolder(primaryKeyReference.get()));
                } else if(other instanceof EJBObject) {
                    return ((EJBObject)other).isIdentical(new SessionIdHolder(primaryKeyReference.get()));
                } else {
                    throw new RuntimeException(getClass() + " was attached to a view that is not an EJBObject or a EJBLocalObject");
                }
            } else if (other instanceof SessionIdHolder) {
                return primaryKeyReference.get().equals(((SessionIdHolder) other).primaryKey);
            } else {
                return false;
            }
        }
    }


    private static class SessionIdHolder implements EJBLocalObject, EJBObject{
        private final Object primaryKey;

        public SessionIdHolder(final Object primaryKey) {
            this.primaryKey = primaryKey;
        }

        @Override
        public EJBLocalHome getEJBLocalHome() throws EJBException {
            return null;
        }

        @Override
        public EJBHome getEJBHome() throws RemoteException {
            return null;
        }

        @Override
        public Object getPrimaryKey() throws EJBException {
            return null;
        }

        @Override
        public void remove() throws RemoveException, EJBException {

        }

        @Override
        public Handle getHandle() throws RemoteException {
            return null;
        }

        @Override
        public boolean isIdentical(final EJBObject ejbo) throws RemoteException {
            return false;
        }

        @Override
        public boolean isIdentical(final EJBLocalObject obj) throws EJBException {
            return false;
        }
    }

}
