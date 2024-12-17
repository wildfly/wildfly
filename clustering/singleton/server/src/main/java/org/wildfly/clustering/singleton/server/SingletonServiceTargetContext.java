/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrar;
import org.wildfly.service.ServiceDependency;

/**
 * @author Paul Ferraro
 */
public interface SingletonServiceTargetContext {
    ServiceDependency<ServiceProviderRegistrar<ServiceName, GroupMember>> getServiceProviderRegistrarDependency();
    ServiceDependency<CommandDispatcherFactory<GroupMember>> getCommandDispatcherFactoryDependency();
}
