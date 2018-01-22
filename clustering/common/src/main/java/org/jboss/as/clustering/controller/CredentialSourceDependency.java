/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.StabilityMonitor;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.InjectorDependency;
import org.wildfly.clustering.service.SimpleDependency;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.security.credential.source.CredentialSource;

/**
 * @author Paul Ferraro
 */
public class CredentialSourceDependency implements ValueDependency<CredentialSource> {

    private final ExceptionSupplier<CredentialSource, Exception> supplier;
    private final Iterable<Dependency> dependencies;

    public CredentialSourceDependency(OperationContext context, Attribute attribute, ModelNode model) throws OperationFailedException {
        DependencyCollectingServiceBuilder builder = new DependencyCollectingServiceBuilder();
        this.supplier = CredentialReference.getCredentialSourceSupplier(context, (ObjectTypeAttributeDefinition) attribute.getDefinition(), model, builder);
        this.dependencies = builder;
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        for (Dependency dependency : this.dependencies) {
            dependency.register(builder);
        }
        return builder;
    }

    @Override
    public CredentialSource getValue() {
        try {
            return this.supplier.get();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    static class DependencyCollectingServiceBuilder implements ServiceBuilder<Object>, Iterable<Dependency> {
        private final List<Dependency> dependencies = new LinkedList<>();

        @Override
        public Iterator<Dependency> iterator() {
            return this.dependencies.iterator();
        }

        @Override
        public ServiceBuilder<Object> addAliases(ServiceName... aliases) {
            throw new IllegalStateException();
        }

        @Override
        public ServiceBuilder<Object> setInitialMode(ServiceController.Mode mode) {
            throw new IllegalStateException();
        }

        @Override
        public ServiceBuilder<Object> addDependencies(ServiceName... serviceNames) {
            this.addDependencies(Arrays.asList(serviceNames));
            return this;
        }

        @Override
        public ServiceBuilder<Object> addDependencies(ServiceBuilder.DependencyType dependencyType, ServiceName... serviceNames) {
            if (dependencyType != ServiceBuilder.DependencyType.REQUIRED) {
                throw new UnsupportedOperationException();
            }
            return this.addDependencies(serviceNames);
        }

        @Override
        public ServiceBuilder<Object> addDependencies(Iterable<ServiceName> serviceNames) {
            for (ServiceName serviceName : serviceNames) {
                this.dependencies.add(new SimpleDependency(serviceName));
            }
            return this;
        }

        @Override
        public ServiceBuilder<Object> addDependencies(ServiceBuilder.DependencyType dependencyType, Iterable<ServiceName> serviceNames) {
            if (dependencyType != ServiceBuilder.DependencyType.REQUIRED) {
                throw new UnsupportedOperationException();
            }
            return this.addDependencies(serviceNames);
        }

        @Override
        public ServiceBuilder<Object> addDependency(ServiceName serviceName) {
            this.dependencies.add(new SimpleDependency(serviceName));
            return this;
        }

        @Override
        public ServiceBuilder<Object> addDependency(ServiceBuilder.DependencyType dependencyType, ServiceName serviceName) {
            if (dependencyType != ServiceBuilder.DependencyType.REQUIRED) {
                throw new UnsupportedOperationException();
            }
            return this.addDependency(serviceName);
        }

        @Override
        public ServiceBuilder<Object> addDependency(ServiceName serviceName, Injector<Object> target) {
            return this.addDependency(serviceName, Object.class, target);
        }

        @Override
        public ServiceBuilder<Object> addDependency(ServiceBuilder.DependencyType dependencyType, ServiceName serviceName, Injector<Object> target) {
            return this.addDependency(dependencyType, serviceName, Object.class, target);
        }

        @Override
        public <I> ServiceBuilder<Object> addDependency(ServiceName serviceName, Class<I> type, Injector<I> target) {
            this.dependencies.add(new InjectorDependency<>(serviceName, type, target));
            return this;
        }

        @Override
        public <I> ServiceBuilder<Object> addDependency(ServiceBuilder.DependencyType dependencyType, ServiceName serviceName, Class<I> type, Injector<I> target) {
            if (dependencyType != ServiceBuilder.DependencyType.REQUIRED) {
                throw new UnsupportedOperationException();
            }
            return this.addDependency(serviceName, type, target);
        }

        @Override
        public <I> ServiceBuilder<Object> addInjection(Injector<? super I> target, I value) {
            throw new IllegalStateException();
        }

        @Override
        public <I> ServiceBuilder<Object> addInjectionValue(Injector<? super I> target, Value<I> value) {
            throw new IllegalStateException();
        }

        @Override
        public ServiceBuilder<Object> addInjection(Injector<? super Object> target) {
            throw new IllegalStateException();
        }

        @Override
        public ServiceBuilder<Object> addMonitor(StabilityMonitor monitor) {
            throw new IllegalStateException();
        }

        @Override
        public ServiceBuilder<Object> addMonitors(StabilityMonitor... monitors) {
            throw new IllegalStateException();
        }

        @Deprecated
        @Override
        public ServiceBuilder<Object> addListener(org.jboss.msc.service.ServiceListener<? super Object> listener) {
            throw new IllegalStateException();
        }

        @Deprecated
        @Override
        public ServiceBuilder<Object> addListener(@SuppressWarnings("unchecked") org.jboss.msc.service.ServiceListener<? super Object>... listeners) {
            throw new IllegalStateException();
        }

        @Deprecated
        @Override
        public ServiceBuilder<Object> addListener(Collection<? extends org.jboss.msc.service.ServiceListener<? super Object>> listeners) {
            throw new IllegalStateException();
        }

        @Override
        public ServiceBuilder<Object> addListener(final LifecycleListener lifecycleListener) {
            throw new IllegalStateException();
        }

        @Override
        public ServiceController<Object> install() throws ServiceRegistryException, IllegalStateException {
            throw new IllegalStateException();
        }
    }
}
