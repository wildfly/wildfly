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

import io.undertow.server.session.SessionManager;
import io.undertow.servlet.api.Deployment;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.service.Builder;
import org.wildfly.extension.undertow.AbstractUndertowEventListener;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowService;

/**
 * Service providing a {@link SessionManagerRegistry} for a host.
 * @author Paul Ferraro
 */
public class SessionManagerRegistryBuilder extends AbstractUndertowEventListener implements Builder<SessionManagerRegistry>, Service<SessionManagerRegistry>, SessionManagerRegistry {

    private final String serverName;
    private final String hostName;
    private final InjectedValue<UndertowService> service = new InjectedValue<>();
    private final InjectedValue<Host> host = new InjectedValue<>();

    private final ConcurrentMap<String, SessionManager> managers = new ConcurrentHashMap<>();

    public SessionManagerRegistryBuilder(String serverName, String hostName) {
        this.serverName = serverName;
        this.hostName = hostName;
    }

    @Override
    public ServiceName getServiceName() {
        return UndertowService.virtualHostName(this.serverName, this.hostName).append("managers");
    }

    @Override
    public ServiceBuilder<SessionManagerRegistry> build(ServiceTarget target) {
        return target.addService(this.getServiceName(), this)
                .addDependency(UndertowService.UNDERTOW, UndertowService.class, this.service)
                .addDependency(UndertowService.virtualHostName(this.serverName, this.hostName), Host.class, this.host)
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public SessionManagerRegistry getValue() {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
        UndertowService service = this.service.getValue();
        service.registerListener(this);
        for (Deployment deployment: this.host.getValue().getDeployments()) {
            this.managers.putIfAbsent(deployment.getDeploymentInfo().getDeploymentName(), deployment.getSessionManager());
        }
    }

    @Override
    public void stop(StopContext context) {
        this.service.getValue().unregisterListener(this);
        this.managers.clear();
    }

    @Override
    public void onDeploymentStart(Deployment deployment, Host host) {
        if (this.host.getValue().getName().equals(host.getName())) {
            this.managers.putIfAbsent(deployment.getDeploymentInfo().getDeploymentName(), deployment.getSessionManager());
        }
    }

    @Override
    public void onDeploymentStop(Deployment deployment, Host host) {
        if (this.host.getValue().getName().equals(host.getName())) {
            this.managers.remove(deployment.getDeploymentInfo().getDeploymentName());
        }
    }

    @Override
    public SessionManager getSessionManager(String deployment) {
        return this.managers.get(deployment);
    }
}
