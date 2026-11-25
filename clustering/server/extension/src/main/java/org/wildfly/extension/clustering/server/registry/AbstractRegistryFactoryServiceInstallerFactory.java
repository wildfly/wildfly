/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.registry;

import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.registry.RegistryFactory;
import org.wildfly.clustering.server.service.BinaryServiceInstallerFactory;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractRegistryFactoryServiceInstallerFactory<K, V> implements BinaryServiceInstallerFactory<RegistryFactory<GroupMember, K, V>> {

    @SuppressWarnings("unchecked")
    @Override
    public BinaryServiceDescriptor<RegistryFactory<GroupMember, K, V>> getServiceDescriptor() {
        return (BinaryServiceDescriptor<RegistryFactory<GroupMember, K, V>>) (BinaryServiceDescriptor<?>) ClusteringServiceDescriptor.REGISTRY_FACTORY;
    }
}
