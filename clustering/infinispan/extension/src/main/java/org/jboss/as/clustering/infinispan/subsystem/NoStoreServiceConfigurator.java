/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.CacheComponent.PERSISTENCE;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.jboss.as.controller.PathAddress;

/**
 * @author Paul Ferraro
 */
public class NoStoreServiceConfigurator extends ComponentServiceConfigurator<PersistenceConfiguration> {

    NoStoreServiceConfigurator(PathAddress address) {
        super(PERSISTENCE, address);
    }

    @Override
    public PersistenceConfiguration get() {
        return new ConfigurationBuilder().persistence().passivation(false).create();
    }
}
