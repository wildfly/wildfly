/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.msc;

import java.util.Collection;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceListener.Inheritance;
import org.jboss.msc.value.Value;

/**
 * @author Paul Ferraro
 */
public class DelegatingServiceBuilder<T> implements ServiceBuilder<T> {
    private final ServiceBuilder<T> builder;
    private final ServiceControllerFactory factory;

    public DelegatingServiceBuilder(ServiceBuilder<T> builder, ServiceControllerFactory factory) {
        this.builder = builder;
        this.factory = factory;
    }

    @Override
    public ServiceBuilder<T> addAliases(ServiceName... aliases) {
        this.builder.addAliases(aliases);
        return this;
    }

    @Override
    public ServiceBuilder<T> setInitialMode(Mode mode) {
        this.builder.setInitialMode(mode);
        return this;
    }

    @Override
    public ServiceBuilder<T> addDependencies(ServiceName... dependencies) {
        this.builder.addDependencies(dependencies);
        return this;
    }

    @Override
    public ServiceBuilder<T> addDependencies(ServiceBuilder.DependencyType dependencyType, ServiceName... dependencies) {
        this.builder.addDependencies(dependencyType, dependencies);
        return this;
    }

    @Override
    public ServiceBuilder<T> addDependencies(Iterable<ServiceName> dependencies) {
        this.builder.addDependencies(dependencies);
        return this;
    }

    @Override
    public ServiceBuilder<T> addDependencies(ServiceBuilder.DependencyType dependencyType, Iterable<ServiceName> dependencies) {
        this.builder.addDependencies(dependencyType, dependencies);
        return this;
    }

    @Override
    public ServiceBuilder<T> addDependency(ServiceName dependency) {
        this.builder.addDependency(dependency);
        return this;
    }

    @Override
    public ServiceBuilder<T> addDependency(ServiceBuilder.DependencyType dependencyType, ServiceName dependency) {
        this.builder.addDependency(dependencyType, dependency);
        return this;
    }

    @Override
    public ServiceBuilder<T> addDependency(ServiceName dependency, Injector<Object> target) {
        this.builder.addDependency(dependency, target);
        return this;
    }

    @Override
    public ServiceBuilder<T> addDependency(ServiceBuilder.DependencyType dependencyType, ServiceName dependency, Injector<Object> target) {
        this.builder.addDependency(dependencyType, dependency, target);
        return this;
    }

    @Override
    public <I> ServiceBuilder<T> addDependency(ServiceName dependency, Class<I> type, Injector<I> target) {
        this.builder.addDependency(dependency, type, target);
        return this;
    }

    @Override
    public <I> ServiceBuilder<T> addDependency(org.jboss.msc.service.ServiceBuilder.DependencyType dependencyType, ServiceName dependency, Class<I> type, Injector<I> target) {
        this.builder.addDependency(dependencyType, dependency, type, target);
        return this;
    }

    @Override
    public <I> ServiceBuilder<T> addInjection(Injector<? super I> target, I value) {
        this.builder.addInjection(target, value);
        return this;
    }

    @Override
    public <I> ServiceBuilder<T> addInjectionValue(Injector<? super I> target, Value<I> value) {
        this.builder.addInjectionValue(target, value);
        return this;
    }

    @Override
    public ServiceBuilder<T> addInjection(Injector<? super T> target) {
        this.builder.addInjection(target);
        return this;
    }

    @Override
    public ServiceBuilder<T> addListener(ServiceListener<? super T> listener) {
        this.builder.addListener(listener);
        return this;
    }

    @Override
    public ServiceBuilder<T> addListener(ServiceListener<? super T>... listeners) {
        this.builder.addListener(listeners);
        return this;
    }

    @Override
    public ServiceBuilder<T> addListener(Collection<? extends ServiceListener<? super T>> listeners) {
        this.builder.addListener(listeners);
        return this;
    }

    @Override
    public ServiceBuilder<T> addListener(Inheritance inheritance, ServiceListener<? super T> listener) {
        this.builder.addListener(inheritance, listener);
        return this;
    }

    @Override
    public ServiceBuilder<T> addListener(Inheritance inheritance, ServiceListener<? super T>... listeners) {
        this.builder.addListener(inheritance, listeners);
        return this;
    }

    @Override
    public ServiceBuilder<T> addListener(Inheritance inheritance, Collection<? extends ServiceListener<? super T>> listeners) {
        this.builder.addListener(inheritance, listeners);
        return this;
    }

    @Override
    public ServiceController<T> install() {
        return this.factory.createServiceController(this.builder.install());
    }
}
