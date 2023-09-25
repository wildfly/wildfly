/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.interceptors;

import org.jboss.as.ejb3.context.CurrentInvocationContext;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class CurrentInvocationContextInterceptor implements Interceptor {

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new CurrentInvocationContextInterceptor());

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        CurrentInvocationContext.push(context);
        try {
            return context.proceed();
        } finally {
            CurrentInvocationContext.pop();
        }
    }

}
