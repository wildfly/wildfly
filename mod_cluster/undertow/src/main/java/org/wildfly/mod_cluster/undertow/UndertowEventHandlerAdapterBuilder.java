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

package org.wildfly.mod_cluster.undertow;

import org.jboss.as.server.suspend.SuspendController;
import org.jboss.modcluster.container.ContainerEventHandler;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.service.AsynchronousServiceBuilder;
import org.wildfly.extension.mod_cluster.ContainerEventHandlerAdapterBuilder;
import org.wildfly.extension.mod_cluster.ContainerEventHandlerService;
import org.wildfly.extension.undertow.ListenerService;
import org.wildfly.extension.undertow.UndertowService;

public class UndertowEventHandlerAdapterBuilder implements ContainerEventHandlerAdapterBuilder {
    public static final ServiceName SERVICE_NAME = ContainerEventHandlerService.SERVICE_NAME.append("undertow");

    @Override
    public ServiceBuilder<?> build(ServiceTarget target, String connector, int statusInterval) {
        InjectedValue<ContainerEventHandler> eventHandler = new InjectedValue<>();
        InjectedValue<UndertowService> undertowService = new InjectedValue<>();
        InjectedValue<SuspendController> suspendController = new InjectedValue<>();
        @SuppressWarnings("rawtypes")
        InjectedValue<ListenerService> listener = new InjectedValue<>();
        return new AsynchronousServiceBuilder<>(SERVICE_NAME, new UndertowEventHandlerAdapter(eventHandler, undertowService, listener, suspendController, statusInterval)).build(target)
                .addDependency(ContainerEventHandlerService.SERVICE_NAME, ContainerEventHandler.class, eventHandler)
                .addDependency(UndertowService.UNDERTOW, UndertowService.class, undertowService)
                .addDependency(UndertowService.listenerName(connector), ListenerService.class, listener)
                .addDependency(SuspendController.SERVICE_NAME, SuspendController.class, suspendController)
        ;
    }
}
