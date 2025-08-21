/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.ejb;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.network.ClientMapping;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.ejb.infinispan.network.ClientMappingsRegistryEntryServiceInstallerFactory;
import org.wildfly.clustering.ejb.remote.EjbClientServicesProvider;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrar;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.clustering.server.service.FilteredBinaryServiceInstallerProvider;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * A local provider of services providing a client-mappings registry.
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public enum LocalEjbClientServicesProvider implements EjbClientServicesProvider {
    INSTANCE;

    /**
     * This method sets up the ServiceInstallers for the local variant of the ClientMappingsRegistry abstraction, for
     * a gien connector and list of ClientMappings instances.
     *
     * It makes use of the FilteredBinaryServiceInstallerProvider to service-load the Registry and RegistryFactory
     * clustering abstractions used to support the client mappings registry itself.
     *
     * @param connectorName
     * @param clientMappings
     * @return a configured set of ServiceInstaller instances for installing the client mappings registry
     */
    @Override
    public Iterable<ServiceInstaller> getClientMappingsRegistryServiceInstallers(String connectorName, ServiceDependency<List<ClientMapping>> clientMappings) {
        List<ServiceInstaller> installers = new LinkedList<>();

        BinaryServiceConfiguration configuration = BinaryServiceConfiguration.of(ModelDescriptionConstants.LOCAL, connectorName);
        // create an installer for an instance of ClientMappingsRegistryEntry service to hold client mappings information
        installers.add(new ClientMappingsRegistryEntryServiceInstallerFactory(clientMappings).apply(configuration));

        // create an installer for an instance of Registry and RegistryFactory service to hold the client mappings entries
        new FilteredBinaryServiceInstallerProvider(Set.of(ClusteringServiceDescriptor.REGISTRY, ClusteringServiceDescriptor.REGISTRY_FACTORY)).apply(configuration).forEach(installers::add);
        return installers;
    }

    /**
     * This method sets up the ServiceInstallers for the local variant of the ModuleAvailabilityRegistrar abstraction.

     * It makes use of the FilteredBinaryServiceInstallerProvider to service-load the ServiceProviderRegistrar
     * clustering abstraction used to support the module availability registrar itself.

     * @return a configured set of ServiceInstaller instances for installing the module availability registrar
     */
    @Override
    public Iterable<ServiceInstaller> getModuleAvailabilityRegistrarServiceInstallers() {
        List<ServiceInstaller> installers = new LinkedList<>();

        // create an installer for an instance of ServiceProviderRegistrar to hold module availability information
        BinaryServiceConfiguration configuration = BinaryServiceConfiguration.of(ModelDescriptionConstants.LOCAL, "module-availability");
        new FilteredBinaryServiceInstallerProvider(Set.of(ClusteringServiceDescriptor.SERVICE_PROVIDER_REGISTRAR)).apply(configuration).forEach(installers::add);

        // add an alias service to a well-known name
        ServiceName aliasServiceName = ServiceName.parse(EjbClientServicesProvider.MODULE_AVAILABILITY_REGISTRAR_SERVICE_PROVIDER_REGISTRAR.getName());
        ServiceDependency<ServiceProviderRegistrar<Object, GroupMember>> serviceProviderRegistry = configuration.getServiceDependency(ClusteringServiceDescriptor.SERVICE_PROVIDER_REGISTRAR);
        installers.add(ServiceInstaller.builder(serviceProviderRegistry).provides(aliasServiceName).build());
        return installers;
    }


}
