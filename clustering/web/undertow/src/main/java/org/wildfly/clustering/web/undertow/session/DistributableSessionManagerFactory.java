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

import io.undertow.UndertowLogger;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SecureRandomSessionIdGenerator;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ThreadSetupAction;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.web.IdentifierFactory;
import org.wildfly.clustering.web.session.SessionContext;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SessionManagerFactory;
import org.wildfly.clustering.web.undertow.IdentifierFactoryAdapter;

/**
 * Factory for creating a {@link DistributableSessionManager}.
 * @author Paul Ferraro
 */
public class DistributableSessionManagerFactory implements io.undertow.servlet.api.SessionManagerFactory {

    private final SessionManagerFactory<Batch> factory;

    public DistributableSessionManagerFactory(SessionManagerFactory<Batch> factory) {
        this.factory = factory;
    }

    @Override
    public io.undertow.server.session.SessionManager createSessionManager(Deployment deployment) {
        boolean statisticsEnabled = deployment.getDeploymentInfo().getMetricsCollector() != null;
        RecordableInactiveSessionStatistics inactiveSessionStatistics = statisticsEnabled ? new RecordableInactiveSessionStatistics() : null;
        SessionContext context = new UndertowSessionContext(deployment);
        IdentifierFactory<String> factory = new IdentifierFactoryAdapter(new SecureRandomSessionIdGenerator());
        final SessionManager<LocalSessionContext, Batch> manager = this.factory.createSessionManager(context, factory, new LocalSessionContextFactory(), inactiveSessionStatistics);
        DeploymentInfo info = deployment.getDeploymentInfo();
        ThreadSetupAction action = new ThreadSetupAction() {
            @Override
            public Handle setup(HttpServerExchange exchange) {
                return new Handle() {
                    @Override
                    public void tearDown() {
                        // If the session was closed from an async context, the session batch may still be associated with the initial request thread
                        // We suspend the active batch, if present, otherwise the transaction associated with this thread may leak into a subsequent request.
                        Batch batch = manager.getBatcher().suspendBatch();
                        if (batch != null) {
                            UndertowLogger.REQUEST_LOGGER.tracef("Suspending residual active batch: %s", batch);
                        }
                    }
                };
            }
        };
        info.addThreadSetupAction(action);
        RecordableSessionManagerStatistics statistics = (inactiveSessionStatistics != null) ? new DistributableSessionManagerStatistics(manager, inactiveSessionStatistics) : null;
        return new DistributableSessionManager(info.getDeploymentName(), manager, statistics);
    }
}
