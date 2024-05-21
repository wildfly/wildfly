/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.registry.legacy;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.DefaultCacheServiceInstallerProvider;
import org.wildfly.extension.clustering.server.DefaultBinaryServiceInstallerProvider;
import org.wildfly.extension.clustering.server.LegacyCacheJndiNameFactory;

/**
 * @author Paul Ferraro
 */
@Deprecated
@MetaInfServices(DefaultCacheServiceInstallerProvider.class)
public class LegacyDefaultRegistryFactoryServiceInstallerProvider<K, V> extends DefaultBinaryServiceInstallerProvider<org.wildfly.clustering.registry.RegistryFactory<K, V>> implements DefaultCacheServiceInstallerProvider {

    public LegacyDefaultRegistryFactoryServiceInstallerProvider() {
        super(new LegacyCacheRegistryFactoryServiceInstallerFactory<K, V>().getServiceDescriptor(), LegacyCacheJndiNameFactory.REGISTRY_FACTORY);
    }
}
