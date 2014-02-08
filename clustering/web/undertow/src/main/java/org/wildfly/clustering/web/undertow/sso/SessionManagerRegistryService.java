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
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.undertow.AbstractUndertowEventListener;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowService;

/**
 * Service providing a {@link SessionManagerRegistry} for a host.
 * @author Paul Ferraro
 */
public class SessionManagerRegistryService extends AbstractUndertowEventListener implements Service<SessionManagerRegistry>, SessionManagerRegistry {

    public static ServiceName getServiceName(ServiceName hostServiceName) {
        return hostServiceName.append("managers");
    }

    public static ServiceBuilder<SessionManagerRegistry> build(ServiceTarget target, ServiceName hostServiceName) {
        SessionManagerRegistryService registry = new SessionManagerRegistryService();
        return target.addService(getServiceName(hostServiceName), registry)
                .addDependency(UndertowService.UNDERTOW, UndertowService.class, registry.service)
                .addDependency(hostServiceName, Host.class, registry.host)
        ;
    }

    private final InjectedValue<UndertowService> service = new InjectedValue<>();
    private final InjectedValue<Host> host = new InjectedValue<>();

    private final ConcurrentMap<String, SessionManager> managers = new ConcurrentHashMap<>();

    private SessionManagerRegistryService() {
        // Hide
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
