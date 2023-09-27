/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.lifecycle.chains;

import org.junit.Assert;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;

/**
 * @author Stuart Douglas
 */
@Stateless
@Interceptors(LifecycleInterceptorWithProceed.class)
public class InterceptedWithProceedSLSB {

    private static boolean postConstructCalled = false;

    public void doStuff() {

    }

    /**
     * This method should be called, after proceed is called from the interceptor, in the same call stack
     * as the interceptors post construct method. (See 'Multiple Callback Interceptor Methods for a Life Cycle
     * Callback Event' in the interceptors specification.
     */
    @PostConstruct
    public void postConstruct() {
        Assert.assertTrue(LifecycleInterceptorWithProceed.postConstruct);
        Assert.assertFalse(LifecycleInterceptorWithProceed.postConstructFinished);
        postConstructCalled = true;
    }

    public static boolean isPostConstructCalled() {
        return postConstructCalled;
    }
}
