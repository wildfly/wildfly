/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfiguration;
import org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfigurationBuilder;
import org.jboss.as.controller.PathAddress;

/**
 * @author Paul Ferraro
 */
public class FileStoreServiceConfigurator extends StoreServiceConfigurator<SoftIndexFileStoreConfiguration, SoftIndexFileStoreConfigurationBuilder> {

    FileStoreServiceConfigurator(PathAddress address) {
        super(address, SoftIndexFileStoreConfigurationBuilder.class);
    }

    @Override
    public void accept(SoftIndexFileStoreConfigurationBuilder builder) {
        builder.segmented(true);
    }
}
