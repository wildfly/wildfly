/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.support.websocket;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public class OnMessageClientInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        String s = (String) ctx.getParameters()[0];
        ctx.setParameters(new String[] { s + " World" });
        return ctx.proceed();
    }
}
