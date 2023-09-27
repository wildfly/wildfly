/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.component;

import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

import java.util.Map;

/**
 * A factory to create interceptors per ComponentInstance instance.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class ComponentInstanceInterceptorFactory implements InterceptorFactory {
    private final Object KEY = new Object();

    @Override
    public final Interceptor create(InterceptorFactoryContext context) {
        final Map<Object, Object> contextData = context.getContextData();
        Interceptor interceptor = (Interceptor) contextData.get(KEY);
        if (interceptor == null) {
            final Component component = (Component) context.getContextData().get(Component.class);
            contextData.put(KEY, interceptor = create(component, context));
        }
        return interceptor;
    }

    protected abstract Interceptor create(Component component, InterceptorFactoryContext context);
}
