/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.provider.legacy;

import org.wildfly.clustering.provider.ServiceProviderRegistry;
import org.wildfly.clustering.server.service.BinaryServiceInstallerFactory;
import org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;

/**
 * @author Paul Ferraro
 */
@Deprecated
public abstract class AbstractLegacyServiceProviderRegistryServiceInstallerFactory<T> implements BinaryServiceInstallerFactory<org.wildfly.clustering.provider.ServiceProviderRegistry<T>> {

    @SuppressWarnings("unchecked")
    @Override
    public BinaryServiceDescriptor<ServiceProviderRegistry<T>> getServiceDescriptor() {
        return (BinaryServiceDescriptor<org.wildfly.clustering.provider.ServiceProviderRegistry<T>>) (BinaryServiceDescriptor<?>) LegacyClusteringServiceDescriptor.SERVICE_PROVIDER_REGISTRY;
    }
}
