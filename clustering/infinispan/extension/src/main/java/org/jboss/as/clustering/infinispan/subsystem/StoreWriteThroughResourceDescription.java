/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.infinispan.configuration.cache.AsyncStoreConfigurationBuilder;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfigurationBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Describes a write-through store component resource.
 * @author Paul Ferraro
 */
public enum StoreWriteThroughResourceDescription implements StoreWriteResourceDescription {
    INSTANCE;

    private final PathElement path = StoreWriteResourceDescription.pathElement("through");

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.empty();
    }

    @Override
    public ServiceDependency<AsyncStoreConfigurationBuilder<SoftIndexFileStoreConfigurationBuilder>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        return ServiceDependency.from(new Supplier<>() {
            @Override
            public AsyncStoreConfigurationBuilder<SoftIndexFileStoreConfigurationBuilder> get() {
                return new ConfigurationBuilder().persistence().addSoftIndexFileStore().async().disable();
            }
        });
    }
}
