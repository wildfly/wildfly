/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.registry.legacy;

import org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor;
import org.wildfly.clustering.server.service.BinaryServiceInstallerFactory;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;

/**
 * @author Paul Ferraro
 */
@Deprecated
public abstract class LegacyRegistryFactoryServiceInstallerFactory<K, V> implements BinaryServiceInstallerFactory<org.wildfly.clustering.registry.RegistryFactory<K, V>> {

    @SuppressWarnings("unchecked")
    @Override
    public BinaryServiceDescriptor<org.wildfly.clustering.registry.RegistryFactory<K, V>> getServiceDescriptor() {
        return (BinaryServiceDescriptor<org.wildfly.clustering.registry.RegistryFactory<K, V>>) (BinaryServiceDescriptor<?>) LegacyClusteringServiceDescriptor.REGISTRY_FACTORY;
    }
}
