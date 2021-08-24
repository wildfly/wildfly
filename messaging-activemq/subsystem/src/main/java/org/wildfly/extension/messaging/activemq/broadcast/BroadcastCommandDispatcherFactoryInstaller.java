/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.spi.ClusteringRequirement;
import org.wildfly.clustering.spi.dispatcher.CommandDispatcherFactory;
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
