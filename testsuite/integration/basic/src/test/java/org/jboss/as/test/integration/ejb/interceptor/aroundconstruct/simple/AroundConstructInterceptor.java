/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.aroundconstruct.simple;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.InvocationContext;

/**
 * @author Stuart Douglas
 */
public class AroundConstructInterceptor {

    @AroundConstruct
    private void aroundConstrct(InvocationContext ctx) throws Exception {
        if(ctx.getTarget() != null) {
            throw new RuntimeException("target is not null");
        }
        ctx.proceed();
        ((AroundConstructSLSB)ctx.getTarget()).append("AroundConstruct");
    }

    @PostConstruct
    private void postConstruct(InvocationContext ctx) throws Exception {
        ((AroundConstructSLSB)ctx.getTarget()).append("PostConstruct");
        ctx.proceed();
    }

}
