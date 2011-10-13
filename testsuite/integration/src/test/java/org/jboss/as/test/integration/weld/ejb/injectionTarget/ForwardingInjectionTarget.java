/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.weld.ejb.injectionTarget;

import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

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
