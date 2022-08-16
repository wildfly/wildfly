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

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.CustomStoreResourceDefinition.Attribute.CLASS;

import java.util.List;
import java.util.stream.Collectors;

import org.infinispan.commons.util.AggregatedClassLoader;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.configuration.cache.StoreConfigurationBuilder;
import org.jboss.as.clustering.infinispan.logging.InfinispanLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Paul Ferraro
 */
public class CustomStoreServiceConfigurator extends StoreServiceConfigurator<CustomStoreConfiguration, CustomStoreConfigurationBuilder> {

    private final SupplierDependency<List<Module>> modules;

    private volatile String className;

    CustomStoreServiceConfigurator(PathAddress address) {
        super(address, CustomStoreConfigurationBuilder.class);
        this.modules = new ServiceSupplierDependency<>(CacheComponent.MODULES.getServiceName(address.getParent()));
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.className = CLASS.resolveModelAttribute(context, model).asString();
        return super.configure(context, model);
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        return super.register(this.modules.register(builder));
    }

    @Override
    public PersistenceConfiguration get() {
        PersistenceConfiguration persistence = super.get();
        StoreConfiguration store = persistence.stores().get(0);
        List<Module> modules = this.modules.get();
        ClassLoader loader = modules.size() > 1 ? new AggregatedClassLoader(modules.stream().map(Module::getClassLoader).collect(Collectors.toList())) : modules.get(0).getClassLoader();
        try {
            @SuppressWarnings("unchecked")
            Class<StoreConfigurationBuilder<?, ?>> storeClass = (Class<StoreConfigurationBuilder<?, ?>>) loader.loadClass(this.className).asSubclass(StoreConfigurationBuilder.class);
            return new ConfigurationBuilder().persistence().passivation(persistence.passivation()).addStore(storeClass)
                    .async().read(store.async())
                    .fetchPersistentState(store.fetchPersistentState())
                    .preload(store.preload())
                    .purgeOnStartup(store.purgeOnStartup())
                    .shared(store.shared())
                    .withProperties(store.properties())
                    .persistence().create();
        } catch (ClassNotFoundException | ClassCastException e) {
            throw InfinispanLogger.ROOT_LOGGER.invalidCacheStore(e, this.className);
        }
    }

    @Override
    public void accept(CustomStoreConfigurationBuilder builder) {
        // Nothing to configure
    }
}
