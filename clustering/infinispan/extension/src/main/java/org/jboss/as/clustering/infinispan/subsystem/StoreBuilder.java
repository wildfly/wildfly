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

import static org.jboss.as.clustering.infinispan.subsystem.StoreResourceDefinition.Attribute.FETCH_STATE;
import static org.jboss.as.clustering.infinispan.subsystem.StoreResourceDefinition.Attribute.PASSIVATION;
import static org.jboss.as.clustering.infinispan.subsystem.StoreResourceDefinition.Attribute.PRELOAD;
import static org.jboss.as.clustering.infinispan.subsystem.StoreResourceDefinition.Attribute.PROPERTIES;
import static org.jboss.as.clustering.infinispan.subsystem.StoreResourceDefinition.Attribute.PURGE;
import static org.jboss.as.clustering.infinispan.subsystem.StoreResourceDefinition.Attribute.SHARED;
import static org.jboss.as.clustering.infinispan.subsystem.StoreResourceDefinition.Attribute.SINGLETON;

import java.util.Properties;

import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.StoreConfigurationBuilder;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.service.Builder;

/**
 * @author Paul Ferraro
 */
public abstract class StoreBuilder extends ComponentBuilder<PersistenceConfiguration> implements ResourceServiceBuilder<PersistenceConfiguration> {

    private final InjectedValue<AsyncStoreConfiguration> async = new InjectedValue<>();
    private final PathAddress cacheAddress;

    private volatile StoreConfigurationBuilder<?, ?> storeBuilder;

    StoreBuilder(PathAddress cacheAddress) {
        super(CacheComponent.PERSISTENCE, cacheAddress);
        this.cacheAddress = cacheAddress;
    }

    @Override
    public ServiceBuilder<PersistenceConfiguration> build(ServiceTarget target) {
        return super.build(target)
                .addDependency(CacheComponent.STORE_WRITE.getServiceName(this.cacheAddress), AsyncStoreConfiguration.class, this.async)
        ;
    }

    @Override
    public Builder<PersistenceConfiguration> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.storeBuilder = this.createStore(context, model);
        this.storeBuilder.persistence().passivation(PASSIVATION.resolveModelAttribute(context, model).asBoolean());
        Properties properties = new Properties();
        ModelNodes.optionalPropertyList(PROPERTIES.resolveModelAttribute(context, model)).ifPresent(list -> list.forEach(property -> properties.setProperty(property.getName(), property.getValue().asString())));
        this.storeBuilder.fetchPersistentState(FETCH_STATE.resolveModelAttribute(context, model).asBoolean())
                .preload(PRELOAD.resolveModelAttribute(context, model).asBoolean())
                .purgeOnStartup(PURGE.resolveModelAttribute(context, model).asBoolean())
                .shared(SHARED.resolveModelAttribute(context, model).asBoolean())
                .singleton().enabled(SINGLETON.resolveModelAttribute(context, model).asBoolean())
                .withProperties(properties)
        ;
        return this;
    }

    abstract StoreConfigurationBuilder<?, ?> createStore(OperationContext context, ModelNode model) throws OperationFailedException;

    @Override
    public PersistenceConfiguration getValue() {
        return this.storeBuilder.async().read(this.async.getValue()).persistence().create();
    }
}
