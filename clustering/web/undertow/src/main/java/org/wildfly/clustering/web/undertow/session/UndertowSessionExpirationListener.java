/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.SessionExpirationListener;

import io.undertow.server.session.Session;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionListeners;
import io.undertow.servlet.api.Deployment;

/**
 * @author Paul Ferraro
 */
public class UndertowSessionExpirationListener implements SessionExpirationListener {

    private final Deployment deployment;
    private final SessionListeners listeners;

    public UndertowSessionExpirationListener(Deployment deployment, SessionListeners listeners) {
        this.deployment = deployment;
        this.listeners = listeners;
    }

    @Override
    public void sessionExpired(ImmutableSession session) {
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
    }
}
