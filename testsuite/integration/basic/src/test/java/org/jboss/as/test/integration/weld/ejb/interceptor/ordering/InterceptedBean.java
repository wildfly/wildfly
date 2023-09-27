/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.ordering;

import java.io.Serializable;
import java.util.List;

import jakarta.ejb.Stateful;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptors;
import jakarta.interceptor.InvocationContext;

@Stateful
@ApplicationScoped
@CdiIntercepted
@Interceptors(LegacyInterceptor.class)
public class InterceptedBean implements Serializable {

    private static final long serialVersionUID = -4444919869290540443L;

    public void ping(List<String> list) {
        list.add("InterceptedBean");
    }

    @AroundInvoke
    Object aroundInvoke(InvocationContext ctx) throws Exception {
        Object[] parameters = ctx.getParameters();
        @SuppressWarnings("unchecked")
        List<String> sequence = (List<String>) parameters[0];
        sequence.add("TargetClassInterceptor");
        return ctx.proceed();
    }
}
