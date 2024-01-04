/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.lifecycle.chains;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.interceptor.InvocationContext;

/**
 * @author Stuart Douglas
 */
public class LifecycleInterceptorWithProceed {

    public static boolean postConstruct = false;
    public static boolean postConstructFinished = false;
    public static boolean preDestroy = false;

    @PostConstruct
    private void postConstruct(InvocationContext ctx) throws Exception {
        postConstruct = true;
        ctx.proceed();
        postConstructFinished = true;
    }

    @PreDestroy
    private void preDestroy(InvocationContext ctx) throws Exception {
        preDestroy = true;
        ctx.proceed();
    }


}
