/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.configuration.cache.AbstractStoreConfiguration;
import org.infinispan.configuration.cache.AbstractStoreConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;

/**
 * @author Paul Ferraro
 */
public class CustomStoreConfigurationBuilder extends AbstractStoreConfigurationBuilder<CustomStoreConfiguration, CustomStoreConfigurationBuilder> {

    public CustomStoreConfigurationBuilder(PersistenceConfigurationBuilder builder) {
        super(builder, AbstractStoreConfiguration.attributeDefinitionSet());
    }

    @Override
    public CustomStoreConfiguration create() {
        return new CustomStoreConfiguration(this.attributes.protect(), this.async.create());
    }

    @Override
    public CustomStoreConfigurationBuilder self() {
        return this;
    }
}
