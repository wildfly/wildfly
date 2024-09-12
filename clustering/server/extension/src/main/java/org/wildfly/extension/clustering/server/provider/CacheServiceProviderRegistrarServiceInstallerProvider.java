/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.provider;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.ClusteredCacheServiceInstallerProvider;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(ClusteredCacheServiceInstallerProvider.class)
public class CacheServiceProviderRegistrarServiceInstallerProvider<T> extends ServiceProviderRegistrarServiceInstallerProvider<T> implements ClusteredCacheServiceInstallerProvider {

    public CacheServiceProviderRegistrarServiceInstallerProvider() {
        super(new CacheServiceProviderRegistrarServiceInstallerFactory<>());
    }
}
