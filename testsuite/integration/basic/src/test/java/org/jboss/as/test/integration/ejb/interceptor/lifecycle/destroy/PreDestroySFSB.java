/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.lifecycle.destroy;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;
import jakarta.interceptor.Interceptors;

/**
 * @author Stuart Douglas
 */
@Stateful
@Interceptors(PreDestroyInterceptor.class)
public class PreDestroySFSB {

    public static boolean preDestroyCalled = false;
    public static boolean postConstructCalled = false;

    public void doStuff() {

    }

    @Remove
    public void remove() {

    }

    @PostConstruct
    @SuppressWarnings("unused")
    private void postConstruct() {
        postConstructCalled = true;
    }

    @PreDestroy
    @SuppressWarnings("unused")
    private void preDestroy() {
        preDestroyCalled = true;
    }
}
