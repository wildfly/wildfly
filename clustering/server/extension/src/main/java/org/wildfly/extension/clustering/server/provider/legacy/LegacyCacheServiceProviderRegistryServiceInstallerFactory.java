/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.provider.legacy;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.wildfly.clustering.server.infinispan.provider.CacheContainerServiceProviderRegistrar;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
@Deprecated
public class LegacyCacheServiceProviderRegistryServiceInstallerFactory<T> extends AbstractLegacyServiceProviderRegistryServiceInstallerFactory<T> {

    @Override
    public ServiceInstaller apply(CapabilityServiceSupport support, BinaryServiceConfiguration configuration) {
        ServiceDependency<CacheContainerServiceProviderRegistrar<Object>> registrar = configuration.getServiceDependency(ClusteringServiceDescriptor.SERVICE_PROVIDER_REGISTRAR).map(CacheContainerServiceProviderRegistrar.class::cast);
        return ServiceInstaller.builder(LegacyCacheServiceProviderRegistry::wrap, registrar)
                .provides(configuration.resolveServiceName(this.getServiceDescriptor()))
                .requires(registrar)
                .build();
    }
}
