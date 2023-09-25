/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.ejb3.dd;

import jakarta.interceptor.InvocationContext;

/**
 * User: jpai
 */
public class DDBasedInterceptor {

    private boolean postConstructInvoked;

    private void onConstruct(InvocationContext ctx)  throws Exception {
        this.postConstructInvoked = true;
        ctx.proceed();
    }

    public Object onInvoke(InvocationContext invocationContext) throws Exception {
        if (!this.postConstructInvoked) {
            throw new IllegalStateException("PostConstruct method on " + this.getClass().getName() + " interceptor was not invoked");
        }
        return this.getClass().getName() + "#" + invocationContext.proceed();
    }

}
