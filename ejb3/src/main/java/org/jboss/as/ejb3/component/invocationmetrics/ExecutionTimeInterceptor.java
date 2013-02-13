/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
