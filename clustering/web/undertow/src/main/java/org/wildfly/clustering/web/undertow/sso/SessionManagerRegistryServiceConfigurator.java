/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.undertow.sso;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.undertow.UndertowBinaryRequirement;
import org.wildfly.clustering.web.undertow.UndertowRequirement;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowEventListener;
import org.wildfly.extension.undertow.UndertowService;

import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionManager;
import io.undertow.servlet.api.Deployment;

/**
 * Service providing a {@link SessionManagerRegistry} for a host.
 * @author Paul Ferraro
 */
public class SessionManagerRegistryServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Service, SessionManagerRegistry, UndertowEventListener {

    private final String serverName;
    private final String hostName;
    private final SupplierDependency<SessionListener> listener;

    private final ConcurrentMap<String, SessionManager> managers = new ConcurrentHashMap<>();

    private volatile SupplierDependency<UndertowService> service;
    private volatile SupplierDependency<Host> host;
    private volatile Consumer<SessionManagerRegistry> registry;

    public SessionManagerRegistryServiceConfigurator(ServiceName name, String serverName, String hostName, SupplierDependency<SessionListener> listener) {
        super(name);
        this.serverName = serverName;
        this.hostName = hostName;
        this.listener = listener;
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.service = new ServiceSupplierDependency<>(UndertowRequirement.UNDERTOW.getServiceName(support));
        this.host = new ServiceSupplierDependency<>(UndertowBinaryRequirement.HOST.getServiceName(support, this.serverName, this.hostName));
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        this.registry = new CompositeDependency(this.listener, this.service, this.host).register(builder).provides(this.getServiceName());
        return builder.setInstance(this).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public void start(StartContext context) {
        this.service.get().registerListener(this);
        for (Deployment deployment : this.host.get().getDeployments()) {
            this.addDeployment(deployment);
        }
        this.registry.accept(this);
    }

    @Override
    public void stop(StopContext context) {
        for (Deployment deployment : this.host.get().getDeployments()) {
            this.removeDeployment(deployment);
        }
        this.service.get().unregisterListener(this);
    }

    private void addDeployment(Deployment deployment) {
        SessionManager manager = deployment.getSessionManager();
        if (this.managers.putIfAbsent(deployment.getDeploymentInfo().getDeploymentName(), deployment.getSessionManager()) == null) {
            manager.registerSessionListener(this.listener.get());
        }
    }

    private void removeDeployment(Deployment deployment) {
        if (this.managers.remove(deployment.getDeploymentInfo().getDeploymentName()) != null) {
            deployment.getSessionManager().removeSessionListener(this.listener.get());
        }
    }

    @Override
    public void onDeploymentStart(Deployment deployment, Host host) {
        if (this.host.get().getName().equals(host.getName())) {
            this.addDeployment(deployment);
        }
    }

    @Override
    public void onDeploymentStop(Deployment deployment, Host host) {
        if (this.host.get().getName().equals(host.getName())) {
            this.removeDeployment(deployment);
        }
    }

    @Override
    public SessionManager getSessionManager(String deployment) {
        return this.managers.get(deployment);
    }
}
