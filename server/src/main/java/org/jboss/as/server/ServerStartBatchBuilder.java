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

package org.jboss.as.server;

import java.util.Collection;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.Location;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.value.Value;

/**
 * Super-hack batch builder used to ignore on-demand services for the server startup listener.
 *
 * @author John E. Bailey
 */
public class ServerStartBatchBuilder implements BatchBuilder {

    private final BatchBuilder delegate;
    private final ServerStartupListener serverStartupListener;

    public ServerStartBatchBuilder(final BatchBuilder delegate, final ServerStartupListener serverStartupListener) {
        this.delegate = delegate;
        this.serverStartupListener = serverStartupListener;
    }

    @Override
    public <T> BatchServiceBuilder<T> addServiceValue(ServiceName name, Value<? extends Service<T>> value) throws IllegalArgumentException {
        return new ServerStartBatchServiceBuilder<T>(name, delegate.addServiceValue(name, value));
    }

    @Override
    public <T> BatchServiceBuilder<T> addService(ServiceName name, Service<T> service) throws IllegalArgumentException {
        return new ServerStartBatchServiceBuilder<T>(name, delegate.addService(name, service));
    }

    @Override
    public <T> BatchServiceBuilder<T> addServiceValueIfNotExist(ServiceName name, Value<? extends Service<T>> value) throws IllegalArgumentException {
        return new ServerStartBatchServiceBuilder<T>(name, delegate.addServiceValueIfNotExist(name, value));
    }

    @Override
    public BatchBuilder addListener(ServiceListener<Object> listener) {
        delegate.addListener(listener);
        return this;
    }

    @Override
    public BatchBuilder addListener(ServiceListener<Object>... listeners) {
        delegate.addListener(listeners);
        return this;
    }

    @Override
    public BatchBuilder addListener(Collection<ServiceListener<Object>> listeners) {
        delegate.addListener(listeners);
        return this;
    }

    @Override
    public BatchBuilder addDependency(ServiceName dependency) {
        delegate.addDependency(dependency);
        return this;
    }

    @Override
    public BatchBuilder addDependency(ServiceName... dependencies) {
        delegate.addDependency(dependencies);
        return this;
    }

    @Override
    public BatchBuilder addDependency(Collection<ServiceName> dependencies) {
        delegate.addDependency(dependencies);
        return this;
    }

    @Override
    public void install() throws ServiceRegistryException {
        delegate.install();
    }

    @Override
    public BatchBuilder subBatchBuilder() {
        return new ServerStartBatchBuilder(delegate.subBatchBuilder(), serverStartupListener);
    }

    private class ServerStartBatchServiceBuilder<T> implements BatchServiceBuilder<T> {
        private final ServiceName serviceName;
        final BatchServiceBuilder<T> delegate;

        private ServerStartBatchServiceBuilder(ServiceName serviceName, BatchServiceBuilder<T> delegate) {
            this.serviceName = serviceName;
            this.delegate = delegate;
        }

        @Override
        public BatchServiceBuilder<T> addAliases(ServiceName... aliases) {
            delegate.addAliases(aliases);
            return this;
        }

        @Override
        public BatchServiceBuilder<T> setLocation() {
            delegate.setLocation();
            return this;
        }

        @Override
        public BatchServiceBuilder<T> setLocation(Location location) {
            delegate.setLocation(location);
            return this;
        }

        @Override
        public BatchServiceBuilder<T> setInitialMode(ServiceController.Mode mode) {
            if (mode != ServiceController.Mode.ACTIVE) {
                serverStartupListener.expectNonActive(serviceName);
            } else {
                serverStartupListener.unexpectNonActive(serviceName);
            }
            delegate.setInitialMode(mode);
            return this;
        }

        @Override
        public BatchServiceBuilder<T> addDependencies(ServiceName... dependencies) {
            delegate.addDependencies(dependencies);
            return this;
        }

        @Override
        public BatchServiceBuilder<T> addOptionalDependencies(ServiceName... dependencies) {
            delegate.addOptionalDependencies(dependencies);
            return this;
        }

        @Override
        public BatchServiceBuilder<T> addDependencies(Iterable<ServiceName> dependencies) {
            delegate.addDependencies(dependencies);
            return this;
        }

        @Override
        public BatchServiceBuilder<T> addOptionalDependencies(Iterable<ServiceName> dependencies) {
            delegate.addOptionalDependencies(dependencies);
            return this;
        }

        @Override
        public BatchServiceBuilder<T> addDependency(ServiceName dependency) {
            delegate.addDependency(dependency);
            return this;
        }

        @Override
        public BatchServiceBuilder<T> addOptionalDependency(ServiceName dependency) {
            delegate.addOptionalDependency(dependency);
            return this;
        }

        @Override
        public BatchServiceBuilder<T> addDependency(ServiceName dependency, Injector<Object> target) {
            delegate.addDependency(dependency, target);
            return this;
        }

        @Override
        public BatchServiceBuilder<T> addOptionalDependency(ServiceName dependency, Injector<Object> target) {
            delegate.addOptionalDependency(dependency, target);
            return this;
        }

        @Override
        public <I> BatchServiceBuilder<T> addDependency(ServiceName dependency, Class<I> type, Injector<I> target) {
            delegate.addDependency(dependency, type, target);
            return this;
        }

        @Override
        public <I> BatchServiceBuilder<T> addOptionalDependency(ServiceName dependency, Class<I> type, Injector<I> target) {
            delegate.addOptionalDependency(dependency, type, target);
            return this;
        }

        @Override
        public <I> BatchServiceBuilder<T> addInjection(Injector<? super I> target, I value) {
            delegate.addInjection(target, value);
            return this;
        }

        @Override
        public <I> BatchServiceBuilder<T> addInjectionValue(Injector<? super I> target, Value<I> value) {
            delegate.<I>addInjectionValue(target, value);
            return this;
        }

        @Override
        public BatchServiceBuilder<T> addListener(ServiceListener<? super T> serviceListener) {
            delegate.addListener(serviceListener);
            return this;
        }

        @Override
        public BatchServiceBuilder<T> addListener(ServiceListener<? super T>... serviceListeners) {
            delegate.addListener(serviceListeners);
            return this;
        }

        @Override
        public BatchServiceBuilder<T> addListener(Collection<? extends ServiceListener<? super T>> collection) {
            delegate.addListener(collection);
            return this;
        }
    }
}
