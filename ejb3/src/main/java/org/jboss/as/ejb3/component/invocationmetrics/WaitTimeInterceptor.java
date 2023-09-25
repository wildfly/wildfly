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
public class WaitTimeInterceptor extends AbstractEJBInterceptor {
    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new WaitTimeInterceptor());

    static final Object START_WAIT_TIME = new Object();

    private WaitTimeInterceptor() {
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        final EJBComponent component = getComponent(context, EJBComponent.class);
        if (component.isStatisticsEnabled()) {
            context.putPrivateData(START_WAIT_TIME, System.currentTimeMillis());
        }
        return context.proceed();
    }
}
