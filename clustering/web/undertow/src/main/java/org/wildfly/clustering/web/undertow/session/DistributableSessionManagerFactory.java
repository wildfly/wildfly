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

import javax.servlet.ServletContext;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.BatchContext;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Recordable;
import org.wildfly.clustering.web.IdentifierFactory;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.SessionExpirationListener;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SessionManagerConfiguration;
import org.wildfly.clustering.web.session.SessionManagerFactory;
import org.wildfly.clustering.web.undertow.IdentifierFactoryAdapter;
import org.wildfly.extension.undertow.session.DistributableSessionManagerConfiguration;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionListeners;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ThreadSetupHandler;

/**
 * Factory for creating a {@link DistributableSessionManager}.
 * @author Paul Ferraro
 */
public class DistributableSessionManagerFactory implements io.undertow.servlet.api.SessionManagerFactory {

    private final SessionManagerFactory<LocalSessionContext, Batch> factory;
    private final DistributableSessionManagerConfiguration config;

    public DistributableSessionManagerFactory(SessionManagerFactory<LocalSessionContext, Batch> factory, DistributableSessionManagerConfiguration config) {
        this.factory = factory;
        this.config = config;
    }

    @Override
    public io.undertow.server.session.SessionManager createSessionManager(final Deployment deployment) {
        DeploymentInfo info = deployment.getDeploymentInfo();
        boolean statisticsEnabled = info.getMetricsCollector() != null;
        RecordableInactiveSessionStatistics inactiveSessionStatistics = statisticsEnabled ? new RecordableInactiveSessionStatistics() : null;
        IdentifierFactory<String> factory = new IdentifierFactoryAdapter(info.getSessionIdGenerator());
        SessionListeners listeners = new SessionListeners();
        SessionExpirationListener expirationListener = new UndertowSessionExpirationListener(deployment, listeners);
        SessionManagerConfiguration configuration = new SessionManagerConfiguration() {
            @Override
            public ServletContext getServletContext() {
                return deployment.getServletContext();
            }

            @Override
            public IdentifierFactory<String> getIdentifierFactory() {
                return factory;
            }

            @Override
            public SessionExpirationListener getExpirationListener() {
                return expirationListener;
            }

            @Override
            public Recordable<ImmutableSession> getInactiveSessionRecorder() {
                return inactiveSessionStatistics;
            }
        };
        SessionManager<LocalSessionContext, Batch> manager = this.factory.createSessionManager(configuration);
        Batcher<Batch> batcher = manager.getBatcher();
        info.addThreadSetupAction(new ThreadSetupHandler() {
            @Override
            public <T, C> Action<T, C> create(Action<T, C> action) {
                return new Action<T, C>() {
                    @Override
                    public T call(HttpServerExchange exchange, C context) throws Exception {
                        Batch batch = batcher.suspendBatch();
                        try (BatchContext ctx = batcher.resumeBatch(batch)) {
                            return action.call(exchange, context);
                        }
                    }
                };
            }
        });
        RecordableSessionManagerStatistics statistics = (inactiveSessionStatistics != null) ? new DistributableSessionManagerStatistics(manager, inactiveSessionStatistics, this.config.getMaxActiveSessions()) : null;
        return new DistributableSessionManager(info.getDeploymentName(), manager, listeners, statistics);
    }
}
