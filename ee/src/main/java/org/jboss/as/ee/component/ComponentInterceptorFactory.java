/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.component;

import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

/**
 * A factory to create interceptors that are tied to a component instance.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class ComponentInterceptorFactory implements InterceptorFactory {
    @Override
    public Interceptor create(InterceptorFactoryContext context) {
        // TODO: a contract with AbstractComponentDescription
        Component component = (Component) context.getContextData().get(Component.class);
        return create(component, context);
    }

    protected abstract Interceptor create(Component component, InterceptorFactoryContext context);
}
