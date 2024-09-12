/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.registry.legacy;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.ClusteredCacheServiceInstallerProvider;

/**
 * @author Paul Ferraro
 */
@Deprecated
@MetaInfServices(ClusteredCacheServiceInstallerProvider.class)
public class LegacyCacheRegistryFactoryServiceInstallerProvider<K, V> extends LegacyRegistryFactoryServiceInstallerProvider<K, V> implements ClusteredCacheServiceInstallerProvider {

    public LegacyCacheRegistryFactoryServiceInstallerProvider() {
        super(new LegacyCacheRegistryFactoryServiceInstallerFactory<>());
    }
}
