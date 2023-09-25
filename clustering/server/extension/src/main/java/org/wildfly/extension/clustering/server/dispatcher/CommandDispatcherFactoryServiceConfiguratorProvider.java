/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.server.dispatcher;

import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.service.ClusteringRequirement;
import org.wildfly.clustering.server.service.GroupCapabilityServiceConfiguratorFactory;
import org.wildfly.extension.clustering.server.GroupJndiNameFactory;
import org.wildfly.extension.clustering.server.GroupRequirementServiceConfiguratorProvider;

/**
 * Provides the requisite builders for creating a {@link org.wildfly.clustering.dispatcher.CommandDispatcherFactory} created from a specified factory..
 * @author Paul Ferraro
 */
public class CommandDispatcherFactoryServiceConfiguratorProvider extends GroupRequirementServiceConfiguratorProvider<CommandDispatcherFactory> {

    protected CommandDispatcherFactoryServiceConfiguratorProvider(GroupCapabilityServiceConfiguratorFactory<CommandDispatcherFactory> factory) {
        super(ClusteringRequirement.COMMAND_DISPATCHER_FACTORY, factory, GroupJndiNameFactory.COMMAND_DISPATCHER_FACTORY);
    }
}
