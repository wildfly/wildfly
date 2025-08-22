/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.provider;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.wildfly.clustering.server.local.LocalGroup;
import org.wildfly.clustering.server.local.provider.LocalServiceProviderRegistrar;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Used to create a configured ServiceInstaller for the local variant of the ServiceProviderRegistrar clustering abstraction.
 * @author Paul Ferraro
 */
public class LocalServiceProviderRegistrarServiceInstallerFactory<T> extends AbstractServiceProviderRegistrarServiceInstallerFactory<T> {

    @Override
    public ServiceInstaller apply(BinaryServiceConfiguration configuration) {
        ServiceDependency<LocalGroup> group = ServiceDependency.on(ClusteringServiceDescriptor.GROUP, ModelDescriptionConstants.LOCAL).map(LocalGroup.class::cast);
        return ServiceInstaller.builder(group.map(LocalServiceProviderRegistrar::of))
                .provides(configuration.resolveServiceName(this.getServiceDescriptor()))
                .build();
    }
}
