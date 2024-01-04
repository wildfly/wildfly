/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.injectiontarget;

import java.util.Set;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InjectionTarget;

public abstract class ForwardingInjectionTarget<T> implements InjectionTarget<T> {

    public abstract InjectionTarget<T> getDelegate();

    @Override
    public T produce(CreationalContext<T> ctx) {
        return getDelegate().produce(ctx);
    }

    @Override
    public void dispose(T instance) {
        getDelegate().dispose(instance);
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return getDelegate().getInjectionPoints();
    }

    @Override
    public void inject(T instance, CreationalContext<T> ctx) {
        getDelegate().inject(instance, ctx);
    }

    @Override
    public void postConstruct(T instance) {
        getDelegate().postConstruct(instance);
    }

    @Override
    public void preDestroy(T instance) {
        getDelegate().preDestroy(instance);
    }
}
