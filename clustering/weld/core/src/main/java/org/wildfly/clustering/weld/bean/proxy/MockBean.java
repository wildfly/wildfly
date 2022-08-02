/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
