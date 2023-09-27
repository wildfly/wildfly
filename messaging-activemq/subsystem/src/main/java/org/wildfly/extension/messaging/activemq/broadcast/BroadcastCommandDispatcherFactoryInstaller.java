/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.broadcast;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.OperationContext;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.service.ClusteringRequirement;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.extension.messaging.activemq.MessagingServices;

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
            ServiceBuilder<?> builder = context.getServiceTarget().addService(name);
            Consumer<BroadcastCommandDispatcherFactory> injector = builder.provides(name);
            Supplier<CommandDispatcherFactory> factory = builder.requires(ClusteringRequirement.COMMAND_DISPATCHER_FACTORY.getServiceName(context, channelName));
            Service service = new FunctionalService<>(injector, ConcurrentBroadcastCommandDispatcherFactory::new, factory);
            builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND).install();
        }
    }
}
