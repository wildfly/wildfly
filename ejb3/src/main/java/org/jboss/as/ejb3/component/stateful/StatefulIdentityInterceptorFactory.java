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
package org.jboss.as.ejb3.component.stateful;

import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.ejb.client.SessionID;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

/**
 * Interceptor for equals / hashCode for SFSB's
 *
 * @author Stuart Douglas
 */
public class StatefulIdentityInterceptorFactory implements InterceptorFactory {

    public static final StatefulIdentityInterceptorFactory INSTANCE = new StatefulIdentityInterceptorFactory();

    private StatefulIdentityInterceptorFactory() {
    }

    @Override
    public Interceptor create(final InterceptorFactoryContext context) {
        AtomicReference<SessionID> sessionIdReference = (AtomicReference<SessionID>) context.getContextData().get(StatefulSessionComponent.SESSION_ID_REFERENCE_KEY);
        final ComponentView componentView = (ComponentView) context.getContextData().get(ComponentView.class);
        return new StatefulIdentityInterceptor(componentView, sessionIdReference);

    }

    private class StatefulIdentityInterceptor implements Interceptor {

        private final ComponentView componentView;
        private final AtomicReference<SessionID> sessionIdReference;

        public StatefulIdentityInterceptor(final ComponentView componentView, final AtomicReference<SessionID> sessionIdReference) {
            this.componentView = componentView;
            this.sessionIdReference = sessionIdReference;
        }

        @Override
        public Object processInvocation(final InterceptorContext context) throws Exception {
            if (context.getMethod().getName().equals("equals")
                    || context.getMethod().getName().equals("isIdentical")) {
                final Object other = context.getParameters()[0];
                final Class<?> proxyType = componentView.getProxyClass();
                if( proxyType.isAssignableFrom(other.getClass())) {
                    //now we know that this is an ejb for the correct component view
                    //as digging out the session id from the proxy object is not really
                    //a viable option, we invoke equals() for the other instance with a
                    //SessionIdHolder as the other side
                    return other.equals(new SessionIdHolder(sessionIdReference.get()));
                } else if(other instanceof SessionIdHolder) {
                    return sessionIdReference.get().equals( ((SessionIdHolder)other).sessionId);
                } else {
                    return false;
                }
            } else if (context.getMethod().getName().equals("hashCode")) {
                //use the identity of the component view as a hash code
                return sessionIdReference.get().hashCode();
            } else {
                return context.proceed();
            }
        }



    }

    private static class SessionIdHolder {
        private final SessionID sessionId;

        public SessionIdHolder(final SessionID sessionId) {
            this.sessionId = sessionId;
        }
    }
}
