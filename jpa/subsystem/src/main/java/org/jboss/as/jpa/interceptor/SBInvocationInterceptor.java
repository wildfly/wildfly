/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.interceptor;

import org.jboss.as.jpa.container.NonTxEmCloser;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * Session bean Invocation interceptor.
 * Used for stateless session bean invocations to allow NonTxEmCloser to close the
 * underlying entity manager after a non-transactional invocation.
 *
 * @author Scott Marlow
 */
public class SBInvocationInterceptor implements Interceptor {

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new SBInvocationInterceptor());

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {

        NonTxEmCloser.pushCall();
        try {
            return context.proceed();   // call the next interceptor or target
        } finally {
            NonTxEmCloser.popCall();
        }
    }

    private SBInvocationInterceptor() {
    }
}
