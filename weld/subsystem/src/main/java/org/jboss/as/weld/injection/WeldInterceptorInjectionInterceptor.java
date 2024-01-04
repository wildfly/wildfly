/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.injection;

import java.util.Set;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

/**
 * Class that performs Jakarta Contexts and Dependency Injection injection and calls initializer methods on interceptor classes after resource injection
 * has been run
 *
 * @author Stuart Douglas
 */
public class WeldInterceptorInjectionInterceptor implements Interceptor {

    private final Set<Class<?>> interceptorClasses;

    public WeldInterceptorInjectionInterceptor(final Set<Class<?>> interceptorClasses) {
        this.interceptorClasses = interceptorClasses;
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        WeldInjectionContext injectionContext = context.getPrivateData(WeldInjectionContext.class);
        final ComponentInstance componentInstance = context.getPrivateData(ComponentInstance.class);
        //now inject the interceptors
        for (final Class<?> interceptorClass : interceptorClasses) {
            final ManagedReference instance = (ManagedReference) componentInstance.getInstanceData(interceptorClass);
            if (instance != null) {
                injectionContext.injectInterceptor(instance.getInstance());
            }
        }
        return context.proceed();
    }
}
