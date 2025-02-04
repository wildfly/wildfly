/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.ejb;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.wildfly.clustering.ejb.remote.ModuleAvailabilityRegistrarProvider;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.clustering.server.service.FilteredBinaryServiceInstallerProvider;
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

    @Override
    public Iterable<ServiceInstaller> getServiceInstallers(CapabilityServiceSupport support) {
        BinaryServiceConfiguration configuration = BinaryServiceConfiguration.of(ModelDescriptionConstants.LOCAL, "module-availability");
        List<ServiceInstaller> installers = new LinkedList<>();
        // install an instance of ServiceProviderRegistrar to hold module availability information
        new FilteredBinaryServiceInstallerProvider(Set.of(ClusteringServiceDescriptor.SERVICE_PROVIDER_REGISTRAR)).apply(support, configuration).forEach(installers::add);
        return installers;
    }
}
