/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.remote;

import java.util.List;

import org.jboss.as.network.ClientMapping;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * interface defining ClientMappingsRegistryProvider instances, used to install configured client-mappings registry services.
 *
 * @author Paul Ferraro
 */
public interface ClientMappingsRegistryProvider {
    NullaryServiceDescriptor<ClientMappingsRegistryProvider> SERVICE_DESCRIPTOR = NullaryServiceDescriptor.of("org.wildfly.clustering.ejb.client-mappings-registry-provider", ClientMappingsRegistryProvider.class);

    Iterable<ServiceInstaller> getServiceInstallers(String connectorName, ServiceDependency<List<ClientMapping>> clientMappings);
}
