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

import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.StoreConfigurationBuilder;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.service.Builder;

/**
 * @author Paul Ferraro
 */
public abstract class StoreBuilder extends CacheComponentBuilder<PersistenceConfiguration> implements ResourceServiceBuilder<PersistenceConfiguration> {

    private final InjectedValue<AsyncStoreConfiguration> async = new InjectedValue<>();
    private final String containerName;
    private final String cacheName;

    private volatile StoreConfigurationBuilder<?, ?> storeBuilder;

    StoreBuilder(String containerName, String cacheName) {
        super(CacheComponent.PERSISTENCE, containerName, cacheName);
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public ServiceBuilder<PersistenceConfiguration> build(ServiceTarget target) {
        return super.build(target)
                .addDependency(CacheComponent.STORE_WRITE.getServiceName(this.containerName, this.cacheName), AsyncStoreConfiguration.class, this.async)
        ;
    }

    @Override
    public Builder<PersistenceConfiguration> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.storeBuilder = this.createStore(context, model);
        this.storeBuilder.persistence().passivation(PASSIVATION.getDefinition().resolveModelAttribute(context, model).asBoolean());
        this.storeBuilder.fetchPersistentState(FETCH_STATE.getDefinition().resolveModelAttribute(context, model).asBoolean())
                .preload(PRELOAD.getDefinition().resolveModelAttribute(context, model).asBoolean())
                .purgeOnStartup(PURGE.getDefinition().resolveModelAttribute(context, model).asBoolean())
                .shared(SHARED.getDefinition().resolveModelAttribute(context, model).asBoolean())
                .singleton().enabled(SINGLETON.getDefinition().resolveModelAttribute(context, model).asBoolean())
                .withProperties(ModelNodes.asProperties(PROPERTIES.getDefinition().resolveModelAttribute(context, model)))
        ;
        return this;
    }

    abstract StoreConfigurationBuilder<?, ?> createStore(ExpressionResolver resolver, ModelNode model) throws OperationFailedException;

    @Override
    public PersistenceConfiguration getValue() {
        return this.storeBuilder.async().read(this.async.getValue()).persistence().create();
    }
}
