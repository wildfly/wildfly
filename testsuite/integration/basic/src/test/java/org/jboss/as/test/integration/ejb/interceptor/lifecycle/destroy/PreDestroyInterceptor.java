/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.lifecycle.destroy;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.interceptor.InvocationContext;

/**
 * @author Stuart Douglas, Ondrej Chaloupka
 */
public class PreDestroyInterceptor {

    public static boolean preDestroy = false;
    public static boolean postConstruct = false;
    public static boolean preDestroyInvocationTargetNull = false;
    public static boolean postConstructInvocationTargetNull = false;

    @PostConstruct
    @SuppressWarnings("unused")
    private void postConstruct(InvocationContext ctx) throws Exception {
        if(ctx.getTarget() == null) {
            postConstructInvocationTargetNull = true;
        }
        postConstruct = true;
        ctx.proceed();
    }

    @PreDestroy
    @SuppressWarnings("unused")
    private void preDestroy(InvocationContext ctx) throws Exception {
        if(ctx.getTarget() == null) {
            preDestroyInvocationTargetNull = true;
        }
        preDestroy = true;
        ctx.proceed();
    }


}
