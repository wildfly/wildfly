/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.ordering.lifecycle;

import java.io.Serializable;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@CdiIntercepted
public class CdiInterceptor implements Serializable {

    private static final long serialVersionUID = -5949866804898740300L;

    @PostConstruct
    void postConstruct(InvocationContext ctx) {
        try {
            ActionSequence.addAction(CdiInterceptor.class.getSimpleName());
            ctx.proceed();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    void preDestroy(InvocationContext ctx) {
        try {
            ActionSequence.addAction(CdiInterceptor.class.getSimpleName());
            ctx.proceed();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
