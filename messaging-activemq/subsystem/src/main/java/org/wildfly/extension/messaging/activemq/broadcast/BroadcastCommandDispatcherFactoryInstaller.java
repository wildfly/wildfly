/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.broadcast;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;

import org.jboss.as.controller.OperationContext;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Installs a distinct {@link BroadcastCommandDispatcherFactory} service per channel.
 * @author Paul Ferraro
 */
public class BroadcastCommandDispatcherFactoryInstaller implements BiConsumer<OperationContext, String> {

    private final Set<ServiceName> names = Collections.synchronizedSet(new TreeSet<>());

    @Override
    public void accept(OperationContext context, String channelName) {
        ServiceName name = MessagingServices.getBroadcastCommandDispatcherFactoryServiceName(channelName);
        // N.B. BroadcastCommandDispatcherFactory implementations are shared across multiple server resources
        if (this.names.add(name)) {
            ServiceDependency<CommandDispatcherFactory<GroupMember>> factory = ServiceDependency.on(ClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY, channelName);
            ServiceInstaller.builder(ConcurrentBroadcastCommandDispatcherFactory::new, factory)
                    .provides(name)
                    .requires(factory)
                    .build()
                    .install(context);
        }
    }
}
