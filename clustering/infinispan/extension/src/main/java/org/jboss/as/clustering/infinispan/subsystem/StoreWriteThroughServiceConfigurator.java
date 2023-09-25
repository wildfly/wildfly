/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.controller.PathAddress;

/**
 * @author Paul Ferraro
 */
public class StoreWriteThroughServiceConfigurator extends ComponentServiceConfigurator<AsyncStoreConfiguration> {

    StoreWriteThroughServiceConfigurator(PathAddress address) {
        super(CacheComponent.STORE_WRITE, address.getParent());
    }

    @Override
    public AsyncStoreConfiguration get() {
        return new ConfigurationBuilder().persistence().addSoftIndexFileStore().async().disable().create();
    }
}
