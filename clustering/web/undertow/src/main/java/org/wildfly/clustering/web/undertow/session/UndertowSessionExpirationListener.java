/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.util.function.Consumer;

import io.undertow.server.session.Session;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionListeners;
import io.undertow.servlet.api.Deployment;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Recordable;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * @author Paul Ferraro
 */
public class UndertowSessionExpirationListener implements Consumer<ImmutableSession> {

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
        UndertowSessionManager manager = (UndertowSessionManager) this.deployment.getSessionManager();
        Session undertowSession = new DistributableImmutableSession(manager, session);
        Batcher<Batch> batcher = manager.getSessionManager().getBatcher();
        // Perform listener invocation in isolated batch context
        Batch batch = batcher.suspendBatch();
        try {
            this.listeners.sessionDestroyed(undertowSession, null, SessionListener.SessionDestroyedReason.TIMEOUT);
        } finally {
            batcher.resumeBatch(batch);
        }
        // Trigger attribute listeners
        ImmutableSessionAttributes attributes = session.getAttributes();
        for (String name : attributes.getAttributeNames()) {
            Object value = attributes.getAttribute(name);
            manager.getSessionListeners().attributeRemoved(undertowSession, name, value);
        }
    }
}
