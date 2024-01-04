/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.CacheComponent.PERSISTENCE;
import static org.jboss.as.clustering.infinispan.subsystem.StoreResourceDefinition.Attribute.*;

import java.util.Properties;
import java.util.function.Consumer;

import org.infinispan.commons.configuration.Combine;
import org.infinispan.configuration.cache.AbstractStoreConfigurationBuilder;
import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.StoreConfiguration;
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
    private volatile boolean preload;
    private volatile boolean purge;
    private volatile boolean segmented;
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
        this.preload = PRELOAD.resolveModelAttribute(context, model).asBoolean();
        this.purge = PURGE.resolveModelAttribute(context, model).asBoolean();
        this.segmented = SEGMENTED.resolveModelAttribute(context, model).asBoolean();
        this.shared = SHARED.resolveModelAttribute(context, model).asBoolean();
        this.maxBatchSize = MAX_BATCH_SIZE.resolveModelAttribute(context, model).asInt();
        this.properties.clear();
        for (Property property : PROPERTIES.resolveModelAttribute(context, model).asPropertyListOrEmpty()) {
            this.properties.setProperty(property.getName(), property.getValue().asString());
        }
        return this;
    }

    @Override
    public PersistenceConfiguration get() {
        B builder = new ConfigurationBuilder().persistence()
                .passivation(this.passivation)
                .addStore(this.builderClass)
                    .maxBatchSize(this.maxBatchSize)
                    .preload(this.preload)
                    .purgeOnStartup(this.purge)
                    .segmented(this.segmented)
                    .shared(this.shared)
                    .withProperties(this.properties)
                    ;
        this.accept(builder);
        return builder.async().read(this.async.get(), Combine.DEFAULT).persistence().create();
    }

    boolean isPurgeOnStartup() {
        return this.purge;
    }
}
