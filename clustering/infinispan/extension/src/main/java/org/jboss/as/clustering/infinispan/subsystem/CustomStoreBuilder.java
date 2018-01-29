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

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.configuration.cache.StoreConfigurationBuilder;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * @author Paul Ferraro
 */
public class CustomStoreBuilder extends StoreBuilder<CustomStoreConfiguration, CustomStoreConfigurationBuilder> {

    private final ValueDependency<Module> module;

    private volatile String className;

    CustomStoreBuilder(PathAddress address) {
        super(address, CustomStoreConfigurationBuilder.class);
        this.module = new InjectedValueDependency<>(CacheComponent.MODULE.getServiceName(address.getParent()), Module.class);
    }

    @Override
    public Builder<PersistenceConfiguration> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.className = CLASS.resolveModelAttribute(context, model).asString();
        return super.configure(context, model);
    }

    @Override
    public ServiceBuilder<PersistenceConfiguration> build(ServiceTarget target) {
        return this.module.register(super.build(target));
    }

    @Override
    public PersistenceConfiguration getValue() {
        PersistenceConfiguration persistence = super.getValue();
        StoreConfiguration store = persistence.stores().get(0);
        try {
            @SuppressWarnings("unchecked")
            Class<StoreConfigurationBuilder<?, ?>> storeClass = (Class<StoreConfigurationBuilder<?, ?>>) this.module.getValue().getClassLoader().loadClass(this.className).asSubclass(StoreConfigurationBuilder.class);
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
