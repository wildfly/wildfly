/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.method;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * @author OndrejChaloupka
 */
public class AroundInvokeInterceptor {

    Object interceptDD(InvocationContext ctx) throws Exception {
        return "InterceptedDD:" + ctx.proceed().toString();
    }

    // this won't be called because of definition in ejb-jar.xml
    @AroundInvoke
    Object intercept(InvocationContext ctx) throws Exception {
        return "Intercepted:" + ctx.proceed().toString();
    }
}
