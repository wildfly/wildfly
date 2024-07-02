/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.provider.legacy;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.LocalCacheServiceInstallerProvider;

/**
 * @author Paul Ferraro
 */
@Deprecated
@MetaInfServices(LocalCacheServiceInstallerProvider.class)
public class LegacyLocalServiceProviderRegistryServiceInstallerProvider<T> extends LegacyServiceProviderRegistryServiceInstallerProvider<T> implements LocalCacheServiceInstallerProvider {

    public LegacyLocalServiceProviderRegistryServiceInstallerProvider() {
        super(new LegacyLocalServiceProviderRegistryServiceInstallerFactory<>());
    }
}
