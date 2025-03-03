/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.ejb;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.ejb.remote.ModuleAvailabilityRegistrarProvider;
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
 * A local module availability registrar provider implementation.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public enum LocalModuleAvailabilityRegistrarProvider implements ModuleAvailabilityRegistrarProvider {
    INSTANCE;

    /**
     * This method aliases the ServiceProviderRegistry defined for the default local cache for the "ejb" container
     * to the capability for the ServiceProviderRegistry used by the ModuleAvailabilityRegistrar instance.
     * @param support
     * @return
     */
    @Override
    public Iterable<ServiceInstaller> getServiceInstallers(CapabilityServiceSupport support) {
        List<ServiceInstaller> installers = new LinkedList<>();
        // install an instance of ServiceProviderRegistrar to hold module availability information
        BinaryServiceConfiguration configuration = BinaryServiceConfiguration.of(ModelDescriptionConstants.LOCAL, "module-availability");
        new FilteredBinaryServiceInstallerProvider(Set.of(ClusteringServiceDescriptor.SERVICE_PROVIDER_REGISTRAR)).apply(support, configuration).forEach(installers::add);
        // add an alias service to a well-known name
        ServiceName aliasServiceName = ServiceName.of(ModuleAvailabilityRegistrarProvider.MODULE_AVAILABILITY_REGISTRAR_SERVICE_PROVIDER_REGISTRAR.getName());
        ServiceDependency<ServiceProviderRegistrar<Object, GroupMember>> serviceProviderRegistry = configuration.getServiceDependency(ClusteringServiceDescriptor.SERVICE_PROVIDER_REGISTRAR);
        installers.add(ServiceInstaller.builder(serviceProviderRegistry).provides(aliasServiceName).build());
        return installers;
    }
}
