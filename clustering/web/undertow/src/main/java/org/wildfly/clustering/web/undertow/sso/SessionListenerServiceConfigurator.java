/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

import java.util.function.Consumer;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.sso.SSOManager;
import org.wildfly.clustering.web.sso.Sessions;

import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionListener;

/**
 * @author Paul Ferraro
 */
public class SessionListenerServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, SessionListener {

    private final SupplierDependency<SSOManager<AuthenticatedSession, String, String, Void, Batch>> manager;

    public SessionListenerServiceConfigurator(ServiceName name, SupplierDependency<SSOManager<AuthenticatedSession, String, String, Void, Batch>> manager) {
        super(name);
        this.manager = manager;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<SessionListener> listener = this.manager.register(builder).provides(this.getServiceName());
        Service service = Service.newInstance(listener, this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public void sessionIdChanged(Session session, String oldSessionId) {
        SSOManager<AuthenticatedSession, String, String, Void, Batch> manager = this.manager.get();
        try (Batch batch = manager.getBatcher().createBatch()) {
            Sessions<String, String> sessions = manager.findSessionsContaining(oldSessionId);
            if (sessions != null) {
                for (String deployment : sessions.getDeployments()) {
                    if (sessions.getSession(deployment) != null) {
                        sessions.removeSession(deployment);
                        sessions.addSession(deployment, session.getId());
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void sessionCreated(Session session, HttpServerExchange exchange) {
    }

    @Override
    public void sessionDestroyed(Session session, HttpServerExchange exchange, SessionDestroyedReason reason) {
    }

    @Override
    public void attributeAdded(Session session, String name, Object value) {
    }

    @Override
    public void attributeUpdated(Session session, String name, Object newValue, Object oldValue) {
    }

    @Override
    public void attributeRemoved(Session session, String name, Object oldValue) {
    }
}
