/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.osgi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.Location;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.value.Value;

/**
 * A batch builder that maintains a map of service initial modes.
 *
 * [MSC-27] Expose initial Mode on ServiceController
 *
 * @author Thomas.Diesler@jboss.com
 * @since 08-Oct-2010
 */
class BatchBuilderSupport implements BatchBuilder {
    private BatchBuilder delegate;
    private Map<Mode, List<ServiceName>> initialModes = new HashMap<Mode, List<ServiceName>>();

    BatchBuilderSupport(BatchBuilder delegate) {
        this.delegate = delegate;
        for (Mode aux : Mode.values())
            initialModes.put(aux, new ArrayList<ServiceName>());
    }

    Map<Mode, List<ServiceName>> getInitialModes() {
        return Collections.unmodifiableMap(initialModes);
    }

    public <T> ServiceBuilder<T> addServiceValue(ServiceName name, Value<? extends Service<T>> value)
            throws IllegalArgumentException {
        return new ServiceBuilderWrapper<T>(name, delegate.addServiceValue(name, value));
    }

    public <T> ServiceBuilder<T> addService(ServiceName name, Service<T> service) throws IllegalArgumentException {
        return new ServiceBuilderWrapper<T>(name, delegate.addService(name, service));
    }

    public <T> ServiceBuilder<T> addServiceValueIfNotExist(ServiceName name, Value<? extends Service<T>> value)
            throws IllegalArgumentException {
        return new ServiceBuilderWrapper<T>(name, delegate.addServiceValueIfNotExist(name, value));
    }

    public BatchBuilder addListener(ServiceListener<Object> listener) {
        delegate.addListener(listener);
        return this;
    }

    public BatchBuilder addListener(ServiceListener<Object>... listeners) {
        delegate.addListener(listeners);
        return this;
    }

    public BatchBuilder addListener(Collection<ServiceListener<Object>> listeners) {
        delegate.addListener(listeners);
        return this;
    }

    public BatchBuilder addDependency(ServiceName dependency) {
        delegate.addDependency(dependency);
        return this;
    }

    public BatchBuilder addDependency(ServiceName... dependencies) {
        delegate.addDependency(dependencies);
        return this;
    }

    public BatchBuilder addDependency(Collection<ServiceName> dependencies) {
        delegate.addDependency(dependencies);
        return this;
    }

    public void install() throws ServiceRegistryException {
        delegate.install();
    }

    public BatchBuilder subBatchBuilder() {
        delegate.subBatchBuilder();
        return this;
    }

    private class ServiceBuilderWrapper<T> implements ServiceBuilder<T> {
        private final ServiceName serviceName;;
        private final ServiceBuilder<T> delegate;

        ServiceBuilderWrapper(ServiceName serviceName, ServiceBuilder<T> delegate) {
            this.serviceName = serviceName;
            this.delegate = delegate;
        }

        public ServiceBuilder<T> addAliases(ServiceName... aliases) {
            delegate.addAliases(aliases);
            return this;
        }

        public ServiceBuilder<T> setLocation() {
            delegate.setLocation();
            return this;
        }

        public ServiceBuilder<T> setLocation(Location location) {
            delegate.setLocation(location);
            return this;
        }

        public ServiceBuilder<T> setInitialMode(Mode mode) {
            initialModes.get(mode).add(serviceName);
            delegate.setInitialMode(mode);
            return this;
        }

        public ServiceBuilder<T> addDependencies(ServiceName... dependencies) {
            delegate.addDependencies(dependencies);
            return this;
        }

        public ServiceBuilder<T> addOptionalDependencies(ServiceName... dependencies) {
            delegate.addOptionalDependencies(dependencies);
            return this;
        }

        public ServiceBuilder<T> addDependencies(Iterable<ServiceName> dependencies) {
            delegate.addDependencies(dependencies);
            return this;
        }

        public ServiceBuilder<T> addOptionalDependencies(Iterable<ServiceName> dependencies) {
            delegate.addOptionalDependencies(dependencies);
            return this;
        }

        public ServiceBuilder<T> addDependency(ServiceName dependency) {
            delegate.addDependency(dependency);
            return this;
        }

        public ServiceBuilder<T> addOptionalDependency(ServiceName dependency) {
            delegate.addOptionalDependency(dependency);
            return this;
        }

        public ServiceBuilder<T> addDependency(ServiceName dependency, Injector<Object> target) {
            delegate.addDependency(dependency, target);
            return this;
        }

        public ServiceBuilder<T> addOptionalDependency(ServiceName dependency, Injector<Object> target) {
            delegate.addOptionalDependency(dependency, target);
            return this;
        }

        public <I> ServiceBuilder<T> addDependency(ServiceName dependency, Class<I> type, Injector<I> target) {
            delegate.addDependency(dependency, type, target);
            return this;
        }

        public <I> ServiceBuilder<T> addOptionalDependency(ServiceName dependency, Class<I> type, Injector<I> target) {
            delegate.addOptionalDependency(dependency, type, target);
            return this;
        }

        public <I> ServiceBuilder<T> addInjection(Injector<? super I> target, I value) {
            delegate.addInjection(target, value);
            return this;
        }

        public <I> ServiceBuilder<T> addInjectionValue(Injector<? super I> target, Value<I> value) {
            delegate.addInjectionValue(target, value);
            return this;
        }

        public ServiceBuilder<T> addListener(ServiceListener<? super T> listener) {
            delegate.addListener(listener);
            return this;
        }

        public ServiceBuilder<T> addListener(ServiceListener<? super T>... listeners) {
            delegate.addListener(listeners);
            return this;
        }

        public ServiceBuilder<T> addListener(Collection<? extends ServiceListener<? super T>> listeners) {
            delegate.addListener(listeners);
            return this;
        }
    }
}