/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group;

import java.util.function.BiFunction;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public enum GroupServiceInstallerFactory implements BiFunction<CapabilityServiceSupport, String, ServiceInstaller> {
    INSTANCE;

    @Override
    public ServiceInstaller apply(CapabilityServiceSupport support, String name) {
        return ServiceInstaller.builder(ServiceDependency.on(ClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY, name).map(CommandDispatcherFactory::getGroup))
                .provides(support.getCapabilityServiceName(ClusteringServiceDescriptor.GROUP, name))
                .asPassive()
                .build();
    }
}
