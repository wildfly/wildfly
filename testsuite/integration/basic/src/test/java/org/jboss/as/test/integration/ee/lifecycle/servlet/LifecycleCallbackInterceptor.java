/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.lifecycle.servlet;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@LifecycleCallbackBinding
@Priority(Interceptor.Priority.APPLICATION)
public class LifecycleCallbackInterceptor {

    private static volatile int postConstructInvocations = 0;

    @PostConstruct
    public Object postConstruct(InvocationContext ctx) throws Exception {
        postConstructInvocations++;
        return ctx.proceed();
    }

    @PreDestroy
    public Object preDestroy(InvocationContext ctx) throws Exception {
        InfoClient.notify("preDestroyNotify");
        return ctx.proceed();
    }

    public static int getPostConstructIncations() {
        return postConstructInvocations;
    }
}
