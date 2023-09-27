/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.invocationmetrics;

import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.interceptors.AbstractEJBInterceptor;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class ExecutionTimeInterceptor extends AbstractEJBInterceptor {
    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new ExecutionTimeInterceptor());

    private ExecutionTimeInterceptor() {
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        final EJBComponent component = getComponent(context, EJBComponent.class);
        if (!component.isStatisticsEnabled())
            return context.proceed();
        final Long startWaitTime = (Long) context.getPrivateData(WaitTimeInterceptor.START_WAIT_TIME);
        final long waitTime = startWaitTime != null && startWaitTime != 0L ? System.currentTimeMillis() - startWaitTime : 0L;
        component.getInvocationMetrics().startInvocation();
        final long start = System.currentTimeMillis();
        try {
            return context.proceed();
        } finally {
            final long executionTime = System.currentTimeMillis() - start;
            component.getInvocationMetrics().finishInvocation(context.getMethod(), waitTime, executionTime);
        }
    }
}
