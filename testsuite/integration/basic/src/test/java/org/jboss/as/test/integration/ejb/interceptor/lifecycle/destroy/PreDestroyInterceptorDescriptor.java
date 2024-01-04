/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.lifecycle.destroy;

import jakarta.interceptor.InvocationContext;

/**
 * @author Ondrej Chaloupka
 */
public class PreDestroyInterceptorDescriptor {

    public static boolean preDestroy = false;
    public static boolean postConstruct = false;
    public static boolean preDestroyInvocationTargetNull = false;
    public static boolean postConstructInvocationTargetNull = false;

    @SuppressWarnings("unused")
    private void postConstruct(InvocationContext ctx) throws Exception {
        if(ctx.getTarget() == null) {
            postConstructInvocationTargetNull = true;
        }
        postConstruct = true;
        ctx.proceed();
    }

    @SuppressWarnings("unused")
    private void preDestroy(InvocationContext ctx) throws Exception {
        if(ctx.getTarget() == null) {
            preDestroyInvocationTargetNull = true;
        }
        preDestroy = true;
        ctx.proceed();
    }


}
