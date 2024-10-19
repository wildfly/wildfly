/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.configuration.cache.StoreConfigurationBuilder;

/**
 * Registers a custom store resource definition.
 * @author Paul Ferraro
 */
public class CustomStoreResourceDefinitionRegistrar<C extends StoreConfiguration, B extends StoreConfigurationBuilder<C, B>> extends StoreResourceDefinitionRegistrar<C, B> {

    CustomStoreResourceDefinitionRegistrar() {
        super(new CustomStoreResourceDescription<>());
    }
}
