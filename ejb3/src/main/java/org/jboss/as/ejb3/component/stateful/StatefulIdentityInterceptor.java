/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.stateful;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.ejb.client.SessionID;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * Interceptor for equals / hashCode for SFSB's
 *
 * @author Stuart Douglas
 */
public class StatefulIdentityInterceptor implements Interceptor {

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new StatefulIdentityInterceptor());

    private StatefulIdentityInterceptor() {
    }

        @Override
        public Object processInvocation(final InterceptorContext context) throws Exception {
            if (context.getMethod().getName().equals("equals")
                    || context.getMethod().getName().equals("isIdentical")) {
                final Object other = context.getParameters()[0];
                final ComponentView componentView = context.getPrivateData(ComponentView.class);
                final Class<?> proxyType = componentView.getProxyClass();
                final SessionID sessionId = context.getPrivateData(SessionID.class);
                if( proxyType.isAssignableFrom(other.getClass())) {
                    //now we know that this is a Jakarta Enterprise Beans bean for the correct component view
                    //as digging out the session id from the proxy object is not really
                    //a viable option, we invoke equals() for the other instance with a
                    //SessionIdHolder as the other side
                    return other.equals(new SessionIdHolder(sessionId));
                } else if(other instanceof SessionIdHolder) {
                    return sessionId.equals( ((SessionIdHolder)other).sessionId);
                } else {
                    return false;
                }
            } else if (context.getMethod().getName().equals("hashCode")) {
                final SessionID sessionId = context.getPrivateData(SessionID.class);
                //use the identity of the component view as a hash code
                return sessionId.hashCode();
            } else {
                return context.proceed();
            }
        }

    private static class SessionIdHolder {
        private final SessionID sessionId;

        public SessionIdHolder(final SessionID sessionId) {
            this.sessionId = sessionId;
        }
    }
}
