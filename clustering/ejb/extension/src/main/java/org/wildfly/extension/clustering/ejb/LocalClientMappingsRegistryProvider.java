/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.ejb;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.network.ClientMapping;
import org.wildfly.clustering.ejb.infinispan.network.ClientMappingsRegistryEntryServiceInstallerFactory;
import org.wildfly.clustering.ejb.remote.ClientMappingsRegistryProvider;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.clustering.server.service.FilteredBinaryServiceInstallerProvider;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * A local provider of services providing a client-mappings registry.
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public enum LocalClientMappingsRegistryProvider implements ClientMappingsRegistryProvider {
    INSTANCE;

    @Override
    public Iterable<ServiceInstaller> getServiceInstallers(String connectorName, ServiceDependency<List<ClientMapping>> clientMappings) {
        BinaryServiceConfiguration configuration = BinaryServiceConfiguration.of(ModelDescriptionConstants.LOCAL, connectorName);
        List<ServiceInstaller> installers = new LinkedList<>();
        installers.add(new ClientMappingsRegistryEntryServiceInstallerFactory(clientMappings).apply(configuration));
        new FilteredBinaryServiceInstallerProvider(Set.of(ClusteringServiceDescriptor.REGISTRY, ClusteringServiceDescriptor.REGISTRY_FACTORY)).apply(configuration).forEach(installers::add);
        return installers;
    }
}
