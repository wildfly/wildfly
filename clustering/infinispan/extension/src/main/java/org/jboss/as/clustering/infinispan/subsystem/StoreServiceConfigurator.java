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

import static org.jboss.as.clustering.infinispan.subsystem.CacheComponent.PERSISTENCE;
import static org.jboss.as.clustering.infinispan.subsystem.StoreResourceDefinition.Attribute.*;

import java.util.Collections;
import java.util.Properties;
import java.util.function.Consumer;

import org.infinispan.configuration.cache.AbstractStoreConfigurationBuilder;
import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.StoreConfiguration;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Paul Ferraro
 */
public abstract class StoreServiceConfigurator<C extends StoreConfiguration, B extends AbstractStoreConfigurationBuilder<C, B>> extends ComponentServiceConfigurator<PersistenceConfiguration> implements Consumer<B> {

    private final SupplierDependency<AsyncStoreConfiguration> async;
    private final Class<B> builderClass;
    private final Properties properties = new Properties();

    private volatile boolean passivation;
    private volatile boolean fetchState;
    private volatile boolean preload;
    private volatile boolean purge;
    private volatile boolean shared;
    private volatile int maxBatchSize;

    protected StoreServiceConfigurator(PathAddress address, Class<B> builderClass) {
        super(PERSISTENCE, address);
        this.builderClass = builderClass;
        this.async = new ServiceSupplierDependency<>(CacheComponent.STORE_WRITE.getServiceName(address.getParent()));
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        return super.register(this.async.register(builder));
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.passivation = PASSIVATION.resolveModelAttribute(context, model).asBoolean();
        this.fetchState = FETCH_STATE.resolveModelAttribute(context, model).asBoolean();
        this.preload = PRELOAD.resolveModelAttribute(context, model).asBoolean();
        this.purge = PURGE.resolveModelAttribute(context, model).asBoolean();
        this.shared = SHARED.resolveModelAttribute(context, model).asBoolean();
        this.maxBatchSize = MAX_BATCH_SIZE.resolveModelAttribute(context, model).asInt();
        this.properties.clear();
        for (Property property : ModelNodes.optionalPropertyList(PROPERTIES.resolveModelAttribute(context, model)).orElse(Collections.emptyList())) {
            this.properties.setProperty(property.getName(), property.getValue().asString());
        }
        return this;
    }

    @Override
    public PersistenceConfiguration get() {
        B builder = new ConfigurationBuilder().persistence()
                .passivation(this.passivation)
                .addStore(this.builderClass)
                    .fetchPersistentState(this.fetchState)
                    .maxBatchSize(this.maxBatchSize)
                    .preload(this.preload)
                    .purgeOnStartup(this.purge)
                    .shared(this.shared)
                    .withProperties(this.properties)
                    ;
        this.accept(builder);
        return builder.async().read(this.async.get()).persistence().create();
    }
}
