/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.session;

import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * Interceptor that handles the writeReplace method for a stateless and singleton session beans
 *
 * @author Stuart Douglas
 */
public class StatelessWriteReplaceInterceptor implements Interceptor {

    private final String serviceName;

    public StatelessWriteReplaceInterceptor(final String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        return new StatelessSerializedProxy(serviceName);
    }

    public static InterceptorFactory factory(final String serviceName) {
        return new ImmediateInterceptorFactory(new StatelessWriteReplaceInterceptor(serviceName));
    }
}
