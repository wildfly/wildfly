/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.singleton;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactoryContext;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * An {@link org.jboss.invocation.InterceptorFactory} which returns a new instance of {@link ContainerManagedConcurrencyInterceptor} on each
 * invocation to {@link #create(org.jboss.invocation.InterceptorFactoryContext)}. This {@link org.jboss.invocation.InterceptorFactory} can be used
 * for handling container managed concurrency invocations on a {@link SingletonComponent}
 * <p/>
 * User: Jaikiran Pai
 */
class ContainerManagedConcurrencyInterceptorFactory extends ComponentInterceptorFactory {

    private final Map<Method, Method> viewMethodToComponentMethodMap;

    public ContainerManagedConcurrencyInterceptorFactory(Map<Method, Method> viewMethodToComponentMethodMap) {

        this.viewMethodToComponentMethodMap = viewMethodToComponentMethodMap;
    }

    @Override
    protected Interceptor create(final Component component, final InterceptorFactoryContext context) {
        return new ContainerManagedConcurrencyInterceptor((SingletonComponent) component, viewMethodToComponentMethodMap);
    }
}
