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

import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.security.idm.Account;
import io.undertow.security.impl.SingleSignOn;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.web.sso.SSO;
import org.wildfly.clustering.web.sso.SSOManager;
import org.wildfly.extension.undertow.security.sso.SingleSignOnManager;

/**
 * Adapts an {@link SSOManager} to a {@link SingleSignOnManager}.
 * @author Paul Ferraro
 */
public class DistributableSingleSignOnManager implements SingleSignOnManager {

    private final SSOManager<AuthenticatedSession, String, Void, Batch> manager;
    private final SessionManagerRegistry registry;
    private volatile boolean started = false;

    public DistributableSingleSignOnManager(SSOManager<AuthenticatedSession, String, Void, Batch> manager, SessionManagerRegistry registry) {
        this.manager = manager;
        this.registry = registry;
    }

    @Override
    public boolean isStarted() {
        return this.started;
    }

    @Override
    public void start() {
        this.manager.start();
        this.started = true;
    }

    @Override
    public void stop() {
        this.started = false;
        this.manager.stop();
    }

    @Override
    public SingleSignOn createSingleSignOn(Account account, String mechanism) {
        String id = this.manager.createIdentifier();
        Batcher<Batch> batcher = this.manager.getBatcher();
        Batch batch = batcher.createBatch();
        try {
            AuthenticatedSession session = new AuthenticatedSession(account, mechanism);
            SSO<AuthenticatedSession, String, Void> sso = this.manager.createSSO(id, session);
            return new DistributableSingleSignOn(sso, this.registry, batcher, batch);
        } catch (RuntimeException | Error e) {
            batch.discard();
            throw e;
        } finally {
            if (batch.isActive()) {
                // Always disassociate the batch with the current thread
                batcher.suspendBatch();
            }
        }
    }

    @Override
    public SingleSignOn findSingleSignOn(String id) {
        Batcher<Batch> batcher = this.manager.getBatcher();
        Batch batch = batcher.createBatch();
        try {
            SSO<AuthenticatedSession, String, Void> sso = this.manager.findSSO(id);
            if (sso == null) {
                batch.discard();
                return null;
            }
            return new DistributableSingleSignOn(sso, this.registry, batcher, batch);
        } catch (RuntimeException | Error e) {
            if (batch.isActive()) {
                batch.discard();
            }
            throw e;
        } finally {
            if (batch.isActive()) {
                // Always disassociate the batch with the current thread
                batcher.suspendBatch();
            }
        }
    }

    @Override
    public void removeSingleSignOn(SingleSignOn sso) {
        if (sso instanceof InvalidatableSingleSignOn) {
            ((InvalidatableSingleSignOn) sso).invalidate();
        }
    }
}
