/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.order;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class AbstractCustomInterceptor {
    private String tag;

    protected AbstractCustomInterceptor(String tag) {
        this.tag = tag;
    }

    @AroundInvoke
    public Object invoke(InvocationContext ctx) throws Exception {
        return tag + ctx.proceed();
    }
}
