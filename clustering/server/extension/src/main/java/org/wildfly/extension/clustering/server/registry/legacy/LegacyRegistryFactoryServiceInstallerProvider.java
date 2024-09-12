/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.registry.legacy;

import org.wildfly.clustering.server.service.BinaryServiceInstallerFactory;
import org.wildfly.extension.clustering.server.BinaryServiceInstallerProvider;
import org.wildfly.extension.clustering.server.LegacyCacheJndiNameFactory;

/**
 * @author Paul Ferraro
 */
@Deprecated
public class LegacyRegistryFactoryServiceInstallerProvider<K, V> extends BinaryServiceInstallerProvider<org.wildfly.clustering.registry.RegistryFactory<K, V>> {

    public LegacyRegistryFactoryServiceInstallerProvider(BinaryServiceInstallerFactory<org.wildfly.clustering.registry.RegistryFactory<K, V>> installerFactory) {
        super(installerFactory, LegacyCacheJndiNameFactory.REGISTRY_FACTORY);
    }
}
