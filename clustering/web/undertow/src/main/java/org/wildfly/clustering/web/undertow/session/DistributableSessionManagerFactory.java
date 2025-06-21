/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.undertow.session;

import java.time.Duration;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import jakarta.servlet.ServletContext;

import org.wildfly.clustering.cache.batch.BatchContextualizerFactory;
import org.wildfly.clustering.context.Contextualizer;
import org.wildfly.clustering.context.ContextualizerFactory;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.SessionManagerConfiguration;
import org.wildfly.clustering.session.SessionManagerFactory;
import org.wildfly.clustering.web.container.SessionManagerFactoryConfiguration;

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
    private static final ContextualizerFactory BATCH_CONTEXTUALIZER_FACTORY = ServiceLoader.load(BatchContextualizerFactory.class, BatchContextualizerFactory.class.getClassLoader()).findFirst().orElseThrow();

    private final SessionManagerFactory<ServletContext, Map<String, Object>> factory;
    private final SessionManagerFactoryConfiguration config;

    public DistributableSessionManagerFactory(SessionManagerFactory<ServletContext, Map<String, Object>> factory, SessionManagerFactoryConfiguration config) {
        this.factory = factory;
        this.config = config;
    }

    @Override
    public io.undertow.server.session.SessionManager createSessionManager(final Deployment deployment) {
        DeploymentInfo info = deployment.getDeploymentInfo();
        boolean statisticsEnabled = info.getMetricsCollector() != null;
        RecordableInactiveSessionStatistics inactiveSessionStatistics = statisticsEnabled ? new DistributableInactiveSessionStatistics() : null;
        Supplier<String> factory = info.getSessionIdGenerator()::createSessionId;
        // Session listeners are application-specific
        SessionListeners listeners = new SessionListeners();
        Consumer<ImmutableSession> expirationListener = new UndertowSessionExpirationListener(deployment, listeners, inactiveSessionStatistics);
        SessionManagerConfiguration<ServletContext> configuration = new SessionManagerConfiguration<>() {
            @Override
            public ServletContext getContext() {
                return deployment.getServletContext();
            }

            @Override
            public Supplier<String> getIdentifierFactory() {
                return factory;
            }

            @Override
            public Consumer<ImmutableSession> getExpirationListener() {
                return expirationListener;
            }

            @Override
            public Duration getTimeout() {
                return Duration.ofMinutes(this.getContext().getSessionTimeout());
            }
        };
        SessionManager<Map<String, Object>> manager = this.factory.createSessionManager(configuration);
        Contextualizer contextualizer = BATCH_CONTEXTUALIZER_FACTORY.createContextualizer(deployment.getServletContext().getClassLoader());
        info.addThreadSetupAction(new ThreadSetupHandler() {
            @Override
            public <T, C> Action<T, C> create(Action<T, C> action) {
                BiFunction<HttpServerExchange, C, T> actionCaller = new BiFunction<>() {
                    @Override
                    public T apply(HttpServerExchange exchange, C context) {
                        try {
                            return action.call(exchange, context);
                        } catch (RuntimeException e) {
                            // Avoid unnecessary wrapping
                            throw e;
                        } catch (Exception e) {
                            // Wrap as unchecked exception
                            throw new CompletionException(e);
                        }
                    }
                };
                BiFunction<HttpServerExchange, C, T> contextualActionCaller = contextualizer.contextualize(actionCaller);
                return new Action<>() {
                    @Override
                    public T call(HttpServerExchange exchange, C context) throws Exception {
                        try {
                            return contextualActionCaller.apply(exchange, context);
                        } catch (CompletionException e) {
                            // Unwrap checked exception
                            throw (Exception) e.getCause();
                        }
                    }
                };
            }
        });
        RecordableSessionManagerStatistics statistics = (inactiveSessionStatistics != null) ? new DistributableSessionManagerStatistics(manager.getStatistics(), inactiveSessionStatistics, this.config.getMaxActiveSessions()) : null;
        io.undertow.server.session.SessionManager result = new DistributableSessionManager(new DistributableSessionManagerConfiguration() {
            @Override
            public String getDeploymentName() {
                return info.getDeploymentName();
            }

            @Override
            public SessionManager<Map<String, Object>> getSessionManager() {
                return manager;
            }

            @Override
            public SessionListeners getSessionListeners() {
                return listeners;
            }

            @Override
            public RecordableSessionManagerStatistics getStatistics() {
                return statistics;
            }
        });
        result.setDefaultSessionTimeout((int) this.config.getDefaultSessionTimeout().getSeconds());
        return result;
    }
}
