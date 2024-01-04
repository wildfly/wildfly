/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.ordering;

import java.io.Serializable;
import java.util.List;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@CdiIntercepted
public class CdiInterceptor implements Serializable {

    private static final long serialVersionUID = -5949866804898740300L;

    @AroundInvoke
    Object aroundInvoke(InvocationContext ctx) throws Exception {
        Object[] parameters = ctx.getParameters();
        @SuppressWarnings("unchecked")
        List<String> sequence = (List<String>) parameters[0];
        sequence.add("CdiInterceptor");
        return ctx.proceed();
    }
}
