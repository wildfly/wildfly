/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.registry.legacy;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.LocalCacheServiceInstallerProvider;

/**
 * @author Paul Ferraro
 */
@Deprecated
@MetaInfServices(LocalCacheServiceInstallerProvider.class)
public class LegacyLocalRegistryFactoryServiceInstallerProvider<K, V> extends LegacyRegistryFactoryServiceInstallerProvider<K, V> implements LocalCacheServiceInstallerProvider {

    public LegacyLocalRegistryFactoryServiceInstallerProvider() {
        super(new LegacyLocalRegistryFactoryServiceInstallerFactory<>());
    }
}
