/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.remote;

import java.util.List;
import java.util.Map;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.ejb.remote.EjbClientServicesProvider;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.local.LocalGroup;
import org.wildfly.clustering.server.local.provider.LocalServiceProviderRegistrar;
import org.wildfly.clustering.server.local.registry.LocalRegistry;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrar;
import org.wildfly.clustering.server.registry.Registry;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.service.Installer.StartWhen;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * An {@link EjbClientServicesProvider} for use when no clustering provider is available, e.g. an
 * application client container, or a server profile that configures ejb3 without infinispan.
 * <p>
 * The services it installs are backed by the non-clustered ("local") implementations of the
 * clustering server SPI, which describe a singleton group containing only this node.
 *
 * @author Richard Achmatowicz
 */
public enum NonClusteredEjbClientServicesProvider implements EjbClientServicesProvider {
    INSTANCE;

    private static final String GROUP_NAME = ModelDescriptionConstants.LOCAL;

    private static ServiceDependency<LocalGroup> localGroup() {
        return ServiceDependency.on(ServerEnvironment.SERVICE_DESCRIPTOR)
                .map(environment -> LocalGroup.of(GROUP_NAME, environment.getNodeName()));
    }

    @Override
    public Iterable<ServiceInstaller> getModuleAvailabilityRegistrarServiceInstallers() {
        @SuppressWarnings("unchecked")
        ServiceDependency<ServiceProviderRegistrar<Object, GroupMember>> registrar =
                (ServiceDependency<ServiceProviderRegistrar<Object, GroupMember>>) (ServiceDependency<?>)
                        localGroup().map(LocalServiceProviderRegistrar::of);

        ServiceName name = ServiceName.parse(MODULE_AVAILABILITY_REGISTRAR_SERVICE_PROVIDER_REGISTRAR.getName());

        return List.of(ServiceInstaller.builder(registrar)
                .provides(name)
                .startWhen(StartWhen.AVAILABLE)
                .build());
    }

    @Override
    public Iterable<ServiceInstaller> getClientMappingsRegistryServiceInstallers(String connectorName, ServiceDependency<List<ClientMapping>> clientMappings) {
        ServiceDependency<LocalRegistry<String, List<ClientMapping>>> registry = localGroup()
                .combine(clientMappings, (group, mappings) ->
                        LocalRegistry.of(group, Map.entry(group.getLocalMember().getName(), mappings), () -> {}));

        // Must be provided beneath the ClusteringServiceDescriptor.REGISTRY base name: EJB3RemoteServiceAdd
        // scans the provided names of each returned installer and aliases any descendant of that base name
        // to EJB3RemoteResourceDefinition.CLIENT_MAPPINGS_REGISTRY/<connectorName>.
        ServiceName name = ServiceNameFactory.resolveServiceName(ClusteringServiceDescriptor.REGISTRY, GROUP_NAME, connectorName);

        return List.of(ServiceInstaller.builder(registry)
                .provides(name)
                .onStop(Registry::close)
                .startWhen(StartWhen.AVAILABLE)
                .build());
    }
}
