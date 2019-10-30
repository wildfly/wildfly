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
import io.undertow.security.impl.SingleSignOnManager;

import java.util.Base64;

import org.jboss.logging.Logger;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.web.sso.SSO;
import org.wildfly.clustering.web.sso.SSOManager;

/**
 * Adapts an {@link SSOManager} to a {@link SingleSignOnManager}.
 * @author Paul Ferraro
 */
public class DistributableSingleSignOnManager implements SingleSignOnManager {

    private static final Logger log = Logger.getLogger(DistributableSingleSignOnManager.class);

    private final SSOManager<AuthenticatedSession, String, String, Void, Batch> manager;
    private final SessionManagerRegistry registry;

    public DistributableSingleSignOnManager(SSOManager<AuthenticatedSession, String, String, Void, Batch> manager, SessionManagerRegistry registry) {
        this.manager = manager;
        this.registry = registry;
    }

    @Override
    public SingleSignOn createSingleSignOn(Account account, String mechanism) {
        String id = this.manager.createIdentifier();
        Batcher<Batch> batcher = this.manager.getBatcher();
        // Batch will be closed when SSO is closed
        @SuppressWarnings("resource")
        Batch batch = batcher.createBatch();
        try {
            AuthenticatedSession session = new AuthenticatedSession(account, mechanism);
            SSO<AuthenticatedSession, String, String, Void> sso = this.manager.createSSO(id, session);
            if (log.isTraceEnabled()) {
                log.tracef("Creating SSO ID %s for Principal %s and Roles %s", id, account.getPrincipal().getName(), account.getRoles().toString());
            }
            return new DistributableSingleSignOn(sso, this.registry, batcher, batcher.suspendBatch());
        } catch (RuntimeException | Error e) {
            batch.discard();
            batch.close();
            throw e;
        }
    }

    @Override
    public SingleSignOn findSingleSignOn(String id) {
        // If requested id contains invalid characters, then sso cannot exist and would otherwise cause sso lookup to fail
        try {
            Base64.getUrlDecoder().decode(id);
        } catch (IllegalArgumentException e) {
            return null;
        }

        Batcher<Batch> batcher = this.manager.getBatcher();
        // Batch will be closed when SSO is closed
        @SuppressWarnings("resource")
        Batch batch = batcher.createBatch();
        try {
            SSO<AuthenticatedSession, String, String, Void> sso = this.manager.findSSO(id);
            if (sso == null) {
                if (log.isTraceEnabled()) {
                    log.tracef("SSO ID %s not found on the session manager.", id);
                }
                batch.close();
                return null;
            }
            if (log.isTraceEnabled()) {
                log.tracef("SSO ID %s found on the session manager.", id);
            }
            return new DistributableSingleSignOn(sso, this.registry, batcher, batcher.suspendBatch());
        } catch (RuntimeException | Error e) {
            batch.discard();
            batch.close();
            throw e;
        }
    }

    @Override
    public void removeSingleSignOn(SingleSignOn sso) {
        if (sso instanceof InvalidatableSingleSignOn) {
            if(log.isTraceEnabled()) {
                log.tracef("Removing SSO ID %s", sso.getId());
            }
            ((InvalidatableSingleSignOn) sso).invalidate();
        }
    }
}
