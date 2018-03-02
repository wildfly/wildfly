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

import java.time.Duration;
import java.util.Arrays;

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.modcluster.container.ContainerEventHandler;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.service.AsynchronousServiceBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.extension.mod_cluster.ContainerEventHandlerService;
import org.wildfly.extension.undertow.Capabilities;
import org.wildfly.extension.undertow.UndertowListener;
import org.wildfly.extension.undertow.UndertowService;

public class UndertowEventHandlerAdapterBuilder implements CapabilityServiceBuilder<Void>, UndertowEventHandlerAdapterConfiguration {
    static final ServiceName SERVICE_NAME = ContainerEventHandlerService.SERVICE_NAME.append("undertow");

    private final String listenerName;
    private final Duration statusInterval;

    private final InjectedValue<ContainerEventHandler> eventHandler = new InjectedValue<>();
    private final InjectedValue<SuspendController> suspendController = new InjectedValue<>();

    private volatile ValueDependency<UndertowService> service;
    private volatile ValueDependency<UndertowListener> listener;

    public UndertowEventHandlerAdapterBuilder(String listenerName, Duration statusInterval) {
        this.listenerName = listenerName;
        this.statusInterval = statusInterval;
    }

    @Override
    public ServiceName getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public Builder<Void> configure(CapabilityServiceSupport support) {
        this.service = new InjectedValueDependency<>(support.getCapabilityServiceName(Capabilities.CAPABILITY_UNDERTOW), UndertowService.class);
        this.listener = new InjectedValueDependency<>(support.getCapabilityServiceName(Capabilities.CAPABILITY_LISTENER, this.listenerName), UndertowListener.class);
        return this;
    }

    @Override
    public ServiceBuilder<Void> build(ServiceTarget target) {
        ServiceBuilder<Void> builder = new AsynchronousServiceBuilder<>(SERVICE_NAME, new UndertowEventHandlerAdapter(this)).build(target)
                .addDependency(ContainerEventHandlerService.SERVICE_NAME, ContainerEventHandler.class, this.eventHandler)
                .addDependency(SuspendController.SERVICE_NAME, SuspendController.class, this.suspendController)
                ;
        for (ValueDependency<?> dependency : Arrays.asList(this.service, this.listener)) {
            dependency.register(builder);
        }
        return builder;
    }

    @Override
    public Duration getStatusInterval() {
        return this.statusInterval;
    }

    @Override
    public UndertowService getUndertowService() {
        return this.service.getValue();
    }

    @Override
    public ContainerEventHandler getContainerEventHandler() {
        return this.eventHandler.getValue();
    }

    @Override
    public SuspendController getSuspendController() {
        return this.suspendController.getValue();
    }

    @Override
    public UndertowListener getListener() {
        return this.listener.getValue();
    }
}
