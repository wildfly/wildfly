/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.aroundconstruct.simple;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.InvocationContext;

/**
 * @author Dmitrii Tikhomirov
 */
public class AroundConstructInterceptorWithObjectReturnType {

    @AroundConstruct
    private Object aroundConstrct(InvocationContext ctx) throws Exception {
        if(ctx.getTarget() != null) {
            throw new RuntimeException("target is not null");
        }
        Object result;
        result = ctx.proceed();
        ((AroundConstructInterceptorWithObjectReturnTypeSLSB)ctx.getTarget()).append("AroundConstruct");
        return result;
    }

    @PostConstruct
    private void postConstruct(InvocationContext ctx) throws Exception {
        ((AroundConstructInterceptorWithObjectReturnTypeSLSB)ctx.getTarget()).append("PostConstruct");
        ctx.proceed();
    }

}

