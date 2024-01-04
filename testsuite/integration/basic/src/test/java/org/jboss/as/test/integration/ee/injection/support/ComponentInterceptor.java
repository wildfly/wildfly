/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Priority(Interceptor.Priority.APPLICATION + 10)
@Interceptor
@ComponentInterceptorBinding
public class ComponentInterceptor {

    private static final List<Interception> interceptions = Collections.synchronizedList(new ArrayList<Interception>());

    @AroundInvoke
    public Object alwaysReturnThis(InvocationContext ctx) throws Exception {
        interceptions.add(new Interception(ctx.getMethod().getName(), ctx.getTarget().getClass().getName()));
        return ctx.proceed();
    }

    public static void resetInterceptions() {
        interceptions.clear();
    }

    public static List<Interception> getInterceptions() {
        return interceptions;
    }

    public static class Interception {

        private final String methodName;

        private final String targetClassName;

        public Interception(String methodName, String targetClassName) {
            super();
            this.methodName = methodName;
            this.targetClassName = targetClassName;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getTargetClassName() {
            return targetClassName;
        }

    }

}
