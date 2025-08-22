/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.remote;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.network.ClientMapping;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.ejb.infinispan.network.ClientMappingsRegistryEntryServiceInstallerFactory;
import org.wildfly.clustering.ejb.remote.EjbClientServicesProvider;
import org.wildfly.clustering.infinispan.service.CacheConfigurationServiceInstaller;
import org.wildfly.clustering.infinispan.service.CacheServiceInstaller;
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
import java.util.function.UnaryOperator;

/**
 * The non-legacy version of the EJB client services provider, used when the distributable-ejb subsystem is present.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class InfinispanEjbClientServicesProvider implements EjbClientServicesProvider {

    private final BinaryServiceConfiguration configuration;
    private final UnaryOperator<ConfigurationBuilder> configurator;

    /**
     * Creates an instance of the Infinispan-based EJB client services provider, for local or distribute use, based on a cache-service abstraction.
     * @param configuration a cache configuration
     */
    public InfinispanEjbClientServicesProvider(BinaryServiceConfiguration configuration) {
        this(configuration, UnaryOperator.identity());
    }

    InfinispanEjbClientServicesProvider(BinaryServiceConfiguration configuration, UnaryOperator<ConfigurationBuilder> configurator) {
        this.configuration = configuration;
        this.configurator = configurator;
    }

    /**
     * This method sets up the ServiceInstallers for the distributed variant of the ClientMappingsRegistry abstraction, for
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
        BinaryServiceConfiguration configuration = this.configuration.withChildName(connectorName);
        List<ServiceInstaller> installers = new LinkedList<>();
        installers.add(new CacheConfigurationServiceInstaller(configuration, CacheConfigurationServiceInstaller.fromTemplate(this.configuration).map(this.configurator)));
        installers.add(new CacheServiceInstaller(configuration));
        installers.add(new ClientMappingsRegistryEntryServiceInstallerFactory(clientMappings).apply(configuration));
        new FilteredBinaryServiceInstallerProvider(Set.of(ClusteringServiceDescriptor.REGISTRY, ClusteringServiceDescriptor.REGISTRY_FACTORY)).apply(configuration).forEach(installers::add);
        return installers;
    }

    /**
     * This method sets up the ServiceInstallers for the distributed variant of the ModuleAvailabilityRegistrar abstraction.

     * @return a configured set of ServiceInstaller instances for installing the module availability registrar
     */
    @Override
    public Iterable<ServiceInstaller> getModuleAvailabilityRegistrarServiceInstallers() {
        List<ServiceInstaller> installers = new LinkedList<>();

        // get a handle to the existing ServiceProviderRegistry installed for the cache
        ServiceName serviceProviderRegistrarServiceName = configuration.resolveServiceName(ClusteringServiceDescriptor.SERVICE_PROVIDER_REGISTRAR);
        ServiceDependency<ServiceProviderRegistrar<Object, GroupMember>> serviceProviderRegistrar = ServiceDependency.on(serviceProviderRegistrarServiceName);

        // create an installer to install a well-known alias to that SPR
        ServiceName aliasServiceName = ServiceName.parse(EjbClientServicesProvider.MODULE_AVAILABILITY_REGISTRAR_SERVICE_PROVIDER_REGISTRAR.getName());
        installers.add(ServiceInstaller.builder(serviceProviderRegistrar).provides(aliasServiceName).build());
        return installers;
    }
}
