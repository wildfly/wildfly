/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.lifecycle.chains;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;

/**
 * @author Stuart Douglas
 */
@Stateless
@Interceptors(LifecycleInterceptorNoProceed.class)
public class InterceptedNoProceedSLSB {

    private static boolean postConstructCalled = false;

    public void doStuff() {

    }

    /**
     * This method should not be called, as proceed() is not called from the interceptors
     * post construct method.
     */
    @PostConstruct
    public void postContruct() {
        postConstructCalled = true;
    }

    public static boolean isPostConstructCalled() {
        return postConstructCalled;
    }
}
