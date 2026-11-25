/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.provider;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.LocalCacheServiceInstallerProvider;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(LocalCacheServiceInstallerProvider.class)
public class LocalServiceProviderRegistrarServiceInstallerProvider<T> extends ServiceProviderRegistrarServiceInstallerProvider<T> implements LocalCacheServiceInstallerProvider {

    public LocalServiceProviderRegistrarServiceInstallerProvider() {
        super(new LocalServiceProviderRegistrarServiceInstallerFactory<>());
    }
}
