/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.registry;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.LocalCacheServiceInstallerProvider;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(LocalCacheServiceInstallerProvider.class)
public class LocalRegistryFactoryServiceInstallerProvider<K, V> extends RegistryFactoryServiceInstallerProvider<K, V> implements LocalCacheServiceInstallerProvider {

    public LocalRegistryFactoryServiceInstallerProvider() {
        super(new LocalRegistryFactoryServiceInstallerFactory<>());
    }
}
