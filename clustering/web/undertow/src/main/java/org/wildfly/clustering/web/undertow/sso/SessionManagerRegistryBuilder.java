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

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;
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
public class SessionManagerRegistryBuilder implements CapabilityServiceBuilder<SessionManagerRegistry>, Service<SessionManagerRegistry>, SessionManagerRegistry, UndertowEventListener {

    private final ServiceName name;
    private final String serverName;
    private final String hostName;
    private final ValueDependency<SessionListener> listener;

    private final ConcurrentMap<String, SessionManager> managers = new ConcurrentHashMap<>();

    private volatile ValueDependency<UndertowService> service;
    private volatile ValueDependency<Host> host;

    public SessionManagerRegistryBuilder(ServiceName name, String serverName, String hostName, ValueDependency<SessionListener> listener) {
        this.name = name;
        this.serverName = serverName;
        this.hostName = hostName;
        this.listener = listener;
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public Builder<SessionManagerRegistry> configure(CapabilityServiceSupport support) {
        this.service = new InjectedValueDependency<>(UndertowRequirement.UNDERTOW.getServiceName(support), UndertowService.class);
        this.host = new InjectedValueDependency<>(UndertowBinaryRequirement.HOST.getServiceName(support, this.serverName, this.hostName), Host.class);
        return this;
    }

    @Override
    public ServiceBuilder<SessionManagerRegistry> build(ServiceTarget target) {
        ServiceBuilder<SessionManagerRegistry> builder = target.addService(this.name, this).setInitialMode(ServiceController.Mode.ON_DEMAND);
        return new CompositeDependency(this.listener, this.service, this.host).register(builder);
    }

    @Override
    public SessionManagerRegistry getValue() {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
        this.service.getValue().registerListener(this);
        for (Deployment deployment : this.host.getValue().getDeployments()) {
            this.addDeployment(deployment);
        }
    }

    @Override
    public void stop(StopContext context) {
        for (Deployment deployment : this.host.getValue().getDeployments()) {
            this.removeDeployment(deployment);
        }
        this.service.getValue().unregisterListener(this);
    }

    private void addDeployment(Deployment deployment) {
        SessionManager manager = deployment.getSessionManager();
        if (this.managers.putIfAbsent(deployment.getDeploymentInfo().getDeploymentName(), deployment.getSessionManager()) == null) {
            manager.registerSessionListener(this.listener.getValue());
        }
    }

    private void removeDeployment(Deployment deployment) {
        if (this.managers.remove(deployment.getDeploymentInfo().getDeploymentName()) != null) {
            deployment.getSessionManager().removeSessionListener(this.listener.getValue());
        }
    }

    @Override
    public void onDeploymentStart(Deployment deployment, Host host) {
        if (this.host.getValue().getName().equals(host.getName())) {
            this.addDeployment(deployment);
        }
    }

    @Override
    public void onDeploymentStop(Deployment deployment, Host host) {
        if (this.host.getValue().getName().equals(host.getName())) {
            this.removeDeployment(deployment);
        }
    }

    @Override
    public SessionManager getSessionManager(String deployment) {
        return this.managers.get(deployment);
    }
}
