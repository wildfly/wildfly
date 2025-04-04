/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.server.dispatcher;

import java.util.function.Function;

import org.jboss.as.controller.ServiceNameFactory;
import org.wildfly.clustering.server.local.LocalGroup;
import org.wildfly.clustering.server.local.dispatcher.LocalCommandDispatcherFactory;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Builds a non-clustered {@link org.wildfly.clustering.dispatcher.CommandDispatcherFactory} service.
 * @author Paul Ferraro
 */
public enum LocalCommandDispatcherFactoryServiceInstallerFactory implements Function<String, ServiceInstaller> {
    INSTANCE;

    @Override
    public ServiceInstaller apply(String name) {
        ServiceDependency<LocalGroup> group = ServiceDependency.on(ClusteringServiceDescriptor.GROUP, name).map(LocalGroup.class::cast);
        return ServiceInstaller.builder(group.map(LocalCommandDispatcherFactory::of))
                .provides(ServiceNameFactory.resolveServiceName(ClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY, name))
                .build();
    }
}
