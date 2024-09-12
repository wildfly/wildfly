/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.provider.legacy;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.ClusteredCacheServiceInstallerProvider;

/**
 * @author Paul Ferraro
 */
@Deprecated
@MetaInfServices(ClusteredCacheServiceInstallerProvider.class)
public class LegacyCacheServiceProviderRegistryServiceInstallerProvider<T> extends LegacyServiceProviderRegistryServiceInstallerProvider<T> implements ClusteredCacheServiceInstallerProvider {

    public LegacyCacheServiceProviderRegistryServiceInstallerProvider() {
        super(new LegacyCacheServiceProviderRegistryServiceInstallerFactory<>());
    }
}
