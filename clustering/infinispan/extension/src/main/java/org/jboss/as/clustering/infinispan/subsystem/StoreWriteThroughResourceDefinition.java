/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class StoreWriteThroughResourceDefinition extends StoreWriteResourceDefinition {

    static final PathElement PATH = pathElement("through");

    StoreWriteThroughResourceDefinition() {
        super(PATH, UnaryOperator.identity());
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        Supplier<AsyncStoreConfiguration> configurationFactory = new Supplier<>() {
            @Override
            public AsyncStoreConfiguration get() {
                return new ConfigurationBuilder().persistence().addSoftIndexFileStore().async().disable().create();
            }
        };
        return CapabilityServiceInstaller.builder(CAPABILITY, configurationFactory).build();
    }
}
