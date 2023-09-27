/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.ejb3.dd;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * User: jpai
 */
public class SimpleInterceptor {

    private boolean postConstructInvoked;

    @PostConstruct
    private void onConstruct(InvocationContext invocationContext) throws Exception {
        this.postConstructInvoked = true;
        invocationContext.proceed();
    }

    @AroundInvoke
    public Object onInvoke(InvocationContext ctx) throws Exception {
        if (!this.postConstructInvoked) {
            throw new IllegalStateException("PostConstruct method on " + this.getClass().getName() + " interceptor was not invoked");
        }
        return getClass().getName() + "#" + ctx.proceed();
    }
}
