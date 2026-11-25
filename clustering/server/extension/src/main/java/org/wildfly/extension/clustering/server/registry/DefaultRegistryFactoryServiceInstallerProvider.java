/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.registry;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.registry.RegistryFactory;
import org.wildfly.clustering.server.service.DefaultCacheServiceInstallerProvider;
import org.wildfly.extension.clustering.server.CacheJndiNameFactory;
import org.wildfly.extension.clustering.server.DefaultBinaryServiceInstallerProvider;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(DefaultCacheServiceInstallerProvider.class)
public class DefaultRegistryFactoryServiceInstallerProvider<K, V> extends DefaultBinaryServiceInstallerProvider<RegistryFactory<GroupMember, K, V>> implements DefaultCacheServiceInstallerProvider {

    public DefaultRegistryFactoryServiceInstallerProvider() {
        super(new RegistryFactoryServiceInstallerFactory<K, V>().getServiceDescriptor(), CacheJndiNameFactory.REGISTRY_FACTORY);
    }
}
