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
package org.wildfly.clustering.web.undertow.session;

import org.wildfly.clustering.web.session.SessionContext;
import org.wildfly.clustering.web.session.SessionIdentifierFactory;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SessionManagerFactory;

import io.undertow.server.session.SecureRandomSessionIdGenerator;
import io.undertow.servlet.api.Deployment;

/**
 * Factory for creating a {@link DistributableSessionManager}.
 * @author Paul Ferraro
 */
public class DistributableSessionManagerFactory implements io.undertow.servlet.api.SessionManagerFactory {

    private final SessionManagerFactory factory;

    public DistributableSessionManagerFactory(SessionManagerFactory factory) {
        this.factory = factory;
    }

    @Override
    public io.undertow.server.session.SessionManager createSessionManager(Deployment deployment) {
        SessionContext context = new UndertowSessionContext(deployment);
        SessionIdentifierFactory factory = new UndertowSessionIdentifierFactory(new SecureRandomSessionIdGenerator());
        SessionManager<LocalSessionContext> manager = this.factory.createSessionManager(context, factory, new LocalSessionContextFactory());
        return new DistributableSessionManager(deployment.getDeploymentInfo().getDeploymentName(), manager);
    }
}
