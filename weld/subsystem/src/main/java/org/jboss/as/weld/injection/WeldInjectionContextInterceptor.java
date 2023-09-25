/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.injection;

import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

/**
 * Interceptor that attaches all the nessesary information for weld injection to the interceptor context
 *
 * @author Stuart Douglas
 */
public class WeldInjectionContextInterceptor implements Interceptor {

    private final WeldComponentService weldComponentService;

    public WeldInjectionContextInterceptor(final WeldComponentService weldComponentService) {
        this.weldComponentService = weldComponentService;
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        context.putPrivateData(WeldInjectionContext.class, weldComponentService.createInjectionContext());
        return context.proceed();
    }
}
