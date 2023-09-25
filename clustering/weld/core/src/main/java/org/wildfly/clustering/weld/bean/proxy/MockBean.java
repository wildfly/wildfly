/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.bean.proxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;

/**
 * {@link Bean} instance for use with {@link org.jboss.weld.bean.proxy.TargetBeanInstance#TargetBeanInstance(Bean, Object)} constructor.
 * @author Paul Ferraro
 */
public class MockBean<T> implements Bean<T> {

    private final Type type;

    public MockBean(Type type) {
        this.type = type;
    }

    @Override
    public T create(CreationalContext<T> creationalContext) {
        return null;
    }

    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext) {
    }

    @Override
    public Set<Type> getTypes() {
        return Set.of(this.type);
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return null;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return null;
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public Class<?> getBeanClass() {
        return null;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return null;
    }

    public boolean isNullable() {
        return false;
    }
}
