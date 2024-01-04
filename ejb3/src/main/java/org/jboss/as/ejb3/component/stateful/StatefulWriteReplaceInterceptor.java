/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.stateful;

import org.jboss.ejb.client.SessionID;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

/**
 * Interceptor that handles the writeReplace method for a SFSB
 *
 * @author Stuart Douglas
 */
public class StatefulWriteReplaceInterceptor implements Interceptor {

    private final String serviceName;

    public StatefulWriteReplaceInterceptor(final String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        SessionID sessionId = context.getPrivateData(SessionID.class);
        return new StatefulSerializedProxy(serviceName, sessionId);
    }

    public static class Factory implements InterceptorFactory {

        private final StatefulWriteReplaceInterceptor interceptor;

        public Factory(final String serviceName) {
            interceptor = new StatefulWriteReplaceInterceptor(serviceName);
        }

        @Override
        public Interceptor create(final InterceptorFactoryContext context) {
            return interceptor;
        }
    }
}
