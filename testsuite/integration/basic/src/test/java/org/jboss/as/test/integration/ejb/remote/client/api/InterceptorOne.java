/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.client.api;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * User: jpai
 */
public class InterceptorOne {

    public static final String MESSAGE_SEPARATOR = ",";

    @AroundInvoke
    public Object aroundInvoke(final InvocationContext context) throws Exception {
        final Object[] methodParams = context.getParameters();
        if (methodParams.length == 1 && methodParams[0] instanceof String) {
            final String message = (String) methodParams[0];
            final String interceptedMessage = message + MESSAGE_SEPARATOR + this.getClass().getSimpleName();
            context.setParameters(new Object[] {interceptedMessage});
        }
        return context.proceed();
    }
}
