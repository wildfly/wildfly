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
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.modcluster.container.ContainerEventHandler;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.AsyncServiceConfigurator;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition;
import org.wildfly.extension.undertow.Capabilities;
import org.wildfly.extension.undertow.Server;
import org.wildfly.extension.undertow.UndertowListener;
import org.wildfly.extension.undertow.UndertowService;

/**
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public class UndertowEventHandlerAdapterServiceConfigurator extends UndertowEventHandlerAdapterServiceNameProvider implements CapabilityServiceConfigurator, UndertowEventHandlerAdapterConfiguration {

    private final String proxyName;
    private final String listenerName;
    private final Duration statusInterval;

    private volatile Supplier<ContainerEventHandler> eventHandler;
    private volatile Supplier<SuspendController> suspendController;

    private volatile SupplierDependency<UndertowService> service;
    private volatile SupplierDependency<UndertowListener> listener;

    public UndertowEventHandlerAdapterServiceConfigurator(String proxyName, String listenerName, Duration statusInterval) {
        super(proxyName);
        this.proxyName = proxyName;
        this.listenerName = listenerName;
        this.statusInterval = statusInterval;
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.service = new ServiceSupplierDependency<>(support.getCapabilityServiceName(Capabilities.CAPABILITY_UNDERTOW));
        this.listener = new ServiceSupplierDependency<>(support.getCapabilityServiceName(Capabilities.CAPABILITY_LISTENER, this.listenerName));
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = new AsyncServiceConfigurator(this.getServiceName()).build(target);
        new CompositeDependency(this.service, this.listener).register(builder);
        this.eventHandler = builder.requires(ProxyConfigurationResourceDefinition.Capability.SERVICE.getDefinition().getCapabilityServiceName(proxyName));
        this.suspendController = builder.requires(SuspendController.SERVICE_NAME);
        Service service = new UndertowEventHandlerAdapterService(this);
        return builder.setInstance(service);
    }

    @Override
    public Duration getStatusInterval() {
        return this.statusInterval;
    }

    @Override
    public UndertowService getUndertowService() {
        return this.service.get();
    }

    @Override
    public ContainerEventHandler getContainerEventHandler() {
        return this.eventHandler.get();
    }

    @Override
    public SuspendController getSuspendController() {
        return this.suspendController.get();
    }

    @Override
    public UndertowListener getListener() {
        return this.listener.get();
    }

    @Override
    public Server getServer() {
        return this.listener.get().getServer();
    }
}
