/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.container.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

import org.jboss.logging.Logger;

/**
 * Simple interceptor, which adds its classname in front of the result of {@link InvocationContext#proceed()}. Result of the
 * proceed() stays untouched in case the {@link InvocationContext#getContextData()} contains classname of this interceptor under
 * the {@link FlowTrackingBean#CONTEXT_DATA_KEY} key.
 *
 * @author Jaikiran Pai
 */
public class NonContainerInterceptor {

    private static final Logger logger = Logger.getLogger(NonContainerInterceptor.class);

    @AroundInvoke
    public Object someMethod(InvocationContext invocationContext) throws Exception {
        logger.trace("Invoked non-container interceptor!!!");
        final String skipInterceptor = (String) invocationContext.getContextData().get(FlowTrackingBean.CONTEXT_DATA_KEY);
        if (skipInterceptor != null && this.getClass().getName().equals(skipInterceptor)) {
            return invocationContext.proceed();
        }
        return this.getClass().getName() + " " + invocationContext.proceed();
    }
}
