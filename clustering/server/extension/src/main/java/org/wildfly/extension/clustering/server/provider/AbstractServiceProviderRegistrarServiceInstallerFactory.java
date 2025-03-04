/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.server.provider;

import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrar;
import org.wildfly.clustering.server.service.BinaryServiceInstallerFactory;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;

/**
 * Builds a cache-based {@link ServiceProviderRegistrarFactory} service.
 * @author Paul Ferraro
 */
public abstract class AbstractServiceProviderRegistrarServiceInstallerFactory<T> implements BinaryServiceInstallerFactory<ServiceProviderRegistrar<T, GroupMember>> {

    @SuppressWarnings("unchecked")
    @Override
    public BinaryServiceDescriptor<ServiceProviderRegistrar<T, GroupMember>> getServiceDescriptor() {
        return (BinaryServiceDescriptor<ServiceProviderRegistrar<T, GroupMember>>) (BinaryServiceDescriptor<?>) ClusteringServiceDescriptor.SERVICE_PROVIDER_REGISTRAR;
    }
}
