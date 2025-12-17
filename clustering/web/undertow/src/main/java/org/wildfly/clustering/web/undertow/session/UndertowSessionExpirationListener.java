/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.util.Map;
import java.util.ServiceLoader;

import org.wildfly.clustering.cache.batch.BatchContextualizerFactory;
import org.wildfly.clustering.context.Contextualizer;
import org.wildfly.clustering.context.ContextualizerFactory;
import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.ImmutableSessionMetaData;

import io.undertow.server.session.Session;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionListeners;
import io.undertow.servlet.api.Deployment;

/**
 * @author Paul Ferraro
 */
public class UndertowSessionExpirationListener implements Consumer<ImmutableSession> {
    private static final ContextualizerFactory BATCH_CONTEXTUALIZER_FACTORY = ServiceLoader.load(BatchContextualizerFactory.class, BatchContextualizerFactory.class.getClassLoader()).findFirst().orElseThrow();

    private final Deployment deployment;
    private final SessionListeners listeners;
    private final Recordable<ImmutableSessionMetaData> recorder;

    public UndertowSessionExpirationListener(Deployment deployment, SessionListeners listeners, Recordable<ImmutableSessionMetaData> recorder) {
        this.deployment = deployment;
        this.listeners = listeners;
        this.recorder = recorder;
    }

    @Override
    public void accept(ImmutableSession session) {
        if (this.recorder != null) {
            this.recorder.record(session.getMetaData());
        }
        Session undertowSession = new DistributableImmutableSession(this.deployment.getSessionManager(), session);
        Contextualizer contextualizer = BATCH_CONTEXTUALIZER_FACTORY.createContextualizer(this.deployment.getServletContext().getClassLoader());
        Consumer<Session> notifier = this::notify;
        // Perform listener invocation in isolated batch context
        contextualizer.contextualize(notifier).accept(undertowSession);
        // Trigger attribute listeners
        for (Map.Entry<String, Object> entry : session.getAttributes().entrySet()) {
            this.listeners.attributeRemoved(undertowSession, entry.getKey(), entry.getValue());
        }
    }

    private void notify(Session session) {
        this.listeners.sessionDestroyed(session, null, SessionListener.SessionDestroyedReason.TIMEOUT);
    }
}
