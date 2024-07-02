/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.registry;

import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.registry.RegistryFactory;
import org.wildfly.clustering.server.service.BinaryServiceInstallerFactory;
import org.wildfly.extension.clustering.server.BinaryServiceInstallerProvider;
import org.wildfly.extension.clustering.server.CacheJndiNameFactory;

/**
 * @author Paul Ferraro
 */
public class RegistryFactoryServiceInstallerProvider<K, V> extends BinaryServiceInstallerProvider<RegistryFactory<GroupMember, K, V>> {

    public RegistryFactoryServiceInstallerProvider(BinaryServiceInstallerFactory<RegistryFactory<GroupMember, K, V>> installerFactory) {
        super(installerFactory, CacheJndiNameFactory.REGISTRY_FACTORY);
    }
}
