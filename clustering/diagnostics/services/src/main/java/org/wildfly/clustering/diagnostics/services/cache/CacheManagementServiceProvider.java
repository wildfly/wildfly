/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.diagnostics.services.cache;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.clustering.jgroups.subsystem.ChannelServiceProvider;
import org.jboss.as.clustering.msc.AsynchronousService;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactoryProvider;

public class CacheManagementServiceProvider implements ChannelServiceProvider {

    @Override
    public Collection<ServiceName> getServiceNames(String containerName) {
        return Collections.singleton(CacheManagementService.getServiceName(containerName));
    }

    @Override
    public Collection<ServiceController<?>> install(ServiceTarget target, String containerName, ModuleIdentifier moduleId) {
        ServiceName name = CacheManagementService.getServiceName(containerName);
        InjectedValue<CommandDispatcherFactory> dispatcherFactory = new InjectedValue<CommandDispatcherFactory>();
        Service<CacheManagement> service = new CacheManagementService(name, dispatcherFactory, containerName);
        // start asynchronously to be safe
        ServiceController<CacheManagement> controller = AsynchronousService.addService(target, name, service)
                // we use this injected reference
                .addDependency(CommandDispatcherFactoryProvider.getServiceName(containerName), CommandDispatcherFactory.class, dispatcherFactory)
                .setInitialMode(ServiceController.Mode.PASSIVE)
                .install()
        ;
        return Collections.<ServiceController<?>>singleton(controller);
    }
}
