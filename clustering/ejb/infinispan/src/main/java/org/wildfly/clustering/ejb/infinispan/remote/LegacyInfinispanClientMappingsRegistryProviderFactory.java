/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.remote;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.ejb.remote.ClientMappingsRegistryProvider;
import org.wildfly.clustering.ejb.remote.LegacyClientMappingsRegistryProviderFactory;

/**
 * Factory for creating legacy version of the InfinispanClientMappingsRegistryProvider
 *
 * @author Richard Achmatowicz
 */
@Deprecated
@MetaInfServices(LegacyClientMappingsRegistryProviderFactory.class)
public class LegacyInfinispanClientMappingsRegistryProviderFactory implements LegacyClientMappingsRegistryProviderFactory {

    @Override
    public ClientMappingsRegistryProvider createClientMappingsRegistryProvider(String clusterName) {
        // need to create and return a configured client mappings registry factory
        return new LegacyInfinispanClientMappingsRegistryProvider(clusterName);
    }
}
