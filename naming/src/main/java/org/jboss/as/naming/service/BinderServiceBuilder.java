/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.naming.service;

import org.jboss.as.naming.ImmediateManagedReferenceFactory;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StabilityMonitor;
import org.jboss.msc.value.Value;

import java.util.Collection;

/**
 * A {@link org.jboss.msc.service.ServiceBuilder} which simplifies installation of {@link org.jboss.as.naming.service.BinderService}.
 *
 * The builder handles the creation of the concrete binder service, the setup of mandatory dependencies to other services (e.g. naming store) and the bind value injection.
 *
 * @author Eduardo Martins
 */
public class BinderServiceBuilder implements ServiceBuilder<ManagedReferenceFactory> {

    private final ServiceBuilder<ManagedReferenceFactory> serviceBuilder;

    /**
     * Constructs a service builder for installing a binder service, which value is provided.
     * @param bindInfo the bind info providing the services names required to setup the binder service
     * @param serviceTarget the service target to add the binder service
     * @param value the bind value, used also as the binder service's source
     */
    public BinderServiceBuilder(ContextNames.BindInfo bindInfo, ServiceTarget serviceTarget, Object value) {
        this(bindInfo, serviceTarget, new ImmediateManagedReferenceFactory(value), value);
    }

    /**
     * Constructs a service builder for installing a binder service, which value is provided by the specified factory.
     * @param bindInfo the bind info providing the services names required to setup the binder service
     * @param serviceTarget the service target to add the binder service
     * @param valueFactory the factory which provides the bind value
     * @param binderServiceSource the binder service's source
     */
    public BinderServiceBuilder(ContextNames.BindInfo bindInfo, ServiceTarget serviceTarget, ManagedReferenceFactory valueFactory, Object binderServiceSource) {
        final BinderService binderService = new BinderService(bindInfo.getBindName(), binderServiceSource);
        serviceBuilder = serviceTarget.addService(bindInfo.getBinderServiceName(), binderService)
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                .addInjection(binderService.getManagedObjectInjector(), valueFactory);
    }

    /**
     * Constructs a service builder for installing a binder service, which value is obtained from another service.
     * @param bindInfo the bind info providing the services names required to setup the binder service
     * @param serviceTarget the service target to add the binder service
     * @param valueServiceName the service which value will be used as the bind's value
     */
    public BinderServiceBuilder(ContextNames.BindInfo bindInfo, ServiceTarget serviceTarget, ServiceName valueServiceName) {
        final BinderService binderService = new BinderService(bindInfo.getBindName(), valueServiceName);
        serviceBuilder = serviceTarget.addService(bindInfo.getBinderServiceName(), binderService)
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                .addDependency(valueServiceName, new ManagedReferenceInjector(binderService.getManagedObjectInjector()));
    }

    /**
     * Adds the specified {@link org.jboss.as.naming.deployment.ContextNames.BindInfo}s aliases, handling also the update of each alias's naming store, adding/removing its service name.
     * @param bindInfos
     * @return
     */
    public BinderServiceBuilder addAliases(ContextNames.BindInfo... bindInfos) {
        for (final ContextNames.BindInfo bindInfo : bindInfos) {
            // add alias to service builder
            serviceBuilder.addAliases(bindInfo.getBinderServiceName());
            // setup dependency wrt injection to add and remove the alias service name from its naming store
            final Injector<ServiceBasedNamingStore> injector = new Injector<ServiceBasedNamingStore>() {
                private ServiceBasedNamingStore namingStore;
                @Override
                public synchronized void inject(ServiceBasedNamingStore injectedNamingStore) throws InjectionException {
                    namingStore = injectedNamingStore;
                    namingStore.add(bindInfo.getBinderServiceName());
                }
                @Override
                public synchronized void uninject() {
                    if (namingStore != null) {
                        namingStore.remove(bindInfo.getBinderServiceName());
                        namingStore = null;
                    }
                }
            };
            serviceBuilder.addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, injector);
        }
        return this;
    }

    // ServiceBuilder delegation

    @Override
    public BinderServiceBuilder addAliases(ServiceName... serviceNames) {
        serviceBuilder.addAliases(serviceNames);
        return this;
    }

    @Override
    public BinderServiceBuilder setInitialMode(ServiceController.Mode mode) {
        serviceBuilder.setInitialMode(mode);
        return this;
    }

    @Override
    public BinderServiceBuilder addDependencies(ServiceName... serviceNames) {
        serviceBuilder.addDependencies(serviceNames);
        return this;
    }

    @Override
    public BinderServiceBuilder addDependencies(DependencyType dependencyType, ServiceName... serviceNames) {
        serviceBuilder.addDependencies(dependencyType, serviceNames);
        return this;
    }

    @Override
    public BinderServiceBuilder addDependencies(Iterable<ServiceName> iterable) {
        serviceBuilder.addDependencies(iterable);
        return this;
    }

    @Override
    public BinderServiceBuilder addDependencies(DependencyType dependencyType, Iterable<ServiceName> iterable) {
        serviceBuilder.addDependencies(dependencyType, iterable);
        return this;
    }

    @Override
    public BinderServiceBuilder addDependency(ServiceName serviceName) {
        serviceBuilder.addDependency(serviceName);
        return this;
    }

    @Override
    public BinderServiceBuilder addDependency(DependencyType dependencyType, ServiceName serviceName) {
        serviceBuilder.addDependency(dependencyType, serviceName);
        return this;
    }

    @Override
    public BinderServiceBuilder addDependency(ServiceName serviceName, Injector<Object> injector) {
        serviceBuilder.addDependency(serviceName, injector);
        return this;
    }

    @Override
    public BinderServiceBuilder addDependency(DependencyType dependencyType, ServiceName serviceName, Injector<Object> injector) {
        serviceBuilder.addDependency(dependencyType, serviceName, injector);
        return this;
    }

    @Override
    public <I> BinderServiceBuilder addDependency(ServiceName serviceName, Class<I> aClass, Injector<I> injector) {
        serviceBuilder.addDependency(serviceName, aClass, injector);
        return this;
    }

    @Override
    public <I> BinderServiceBuilder addDependency(DependencyType dependencyType, ServiceName serviceName, Class<I> aClass, Injector<I> injector) {
        serviceBuilder.addDependency(dependencyType, serviceName, aClass, injector);
        return this;
    }

    @Override
    public <I> BinderServiceBuilder addInjection(Injector<? super I> injector, I i) {
        serviceBuilder.addInjection(injector, i);
        return this;
    }

    @Override
    public <I> BinderServiceBuilder addInjectionValue(Injector<? super I> injector, Value<I> value) {
        serviceBuilder.addInjectionValue(injector, value);
        return this;
    }

    @Override
    public BinderServiceBuilder addInjection(Injector<? super ManagedReferenceFactory> injector) {
        serviceBuilder.addInjection(injector);
        return this;
    }

    @Override
    public BinderServiceBuilder addMonitor(StabilityMonitor stabilityMonitor) {
        serviceBuilder.addMonitor(stabilityMonitor);
        return this;
    }

    @Override
    public BinderServiceBuilder addMonitors(StabilityMonitor... stabilityMonitors) {
        serviceBuilder.addMonitors(stabilityMonitors);
        return this;
    }

    @Override
    public BinderServiceBuilder addListener(ServiceListener<? super ManagedReferenceFactory> serviceListener) {
        serviceBuilder.addListener(serviceListener);
        return this;
    }

    @Override
    public BinderServiceBuilder addListener(ServiceListener<? super ManagedReferenceFactory>... serviceListeners) {
        serviceBuilder.addListener(serviceListeners);
        return this;
    }

    @Override
    public BinderServiceBuilder addListener(Collection<? extends ServiceListener<? super ManagedReferenceFactory>> collection) {
        serviceBuilder.addListener(collection);
        return this;
    }

    @Override
    public ServiceController<ManagedReferenceFactory> install() throws ServiceRegistryException, IllegalStateException {
        return serviceBuilder.install();
    }
}
