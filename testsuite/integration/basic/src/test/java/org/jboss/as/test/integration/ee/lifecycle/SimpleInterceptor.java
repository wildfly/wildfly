/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.InvocationContext;

/**
 * User: jpai
 */
public class SimpleInterceptor {

    @PostConstruct
    private void onConstruct(final InvocationContext invocationContext) throws Exception {
        if (invocationContext.getMethod() != null) {
            throw new RuntimeException("InvocationContext#getMethod() is expected to return null on lifecycle callback methods");
        }
        invocationContext.proceed();
    }
}
