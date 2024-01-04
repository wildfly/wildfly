/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.injection;

import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * Class that performs Jakarta Contexts and Dependency Injection and calls initializer methods after resource injection
 * has been run
 *
 * @author Stuart Douglas
 */
public class WeldInjectionInterceptor implements Interceptor {

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new WeldInjectionInterceptor());

    private WeldInjectionInterceptor() {

    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        WeldInjectionContext injectionContext = context.getPrivateData(WeldInjectionContext.class);
        ManagedReference reference = (ManagedReference) context.getPrivateData(ComponentInstance.class).getInstanceData(BasicComponentInstance.INSTANCE_KEY);
        if (reference != null) {
            injectionContext.inject(reference.getInstance());
        }
        return context.proceed();
    }
}
