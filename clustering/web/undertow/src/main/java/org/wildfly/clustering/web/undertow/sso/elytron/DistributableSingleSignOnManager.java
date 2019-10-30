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

package org.wildfly.clustering.web.undertow.sso.elytron;

import java.net.URI;
import java.util.Map;
import java.util.Map.Entry;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.web.sso.SSO;
import org.wildfly.clustering.web.sso.SSOManager;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.http.util.sso.SingleSignOn;
import org.wildfly.security.http.util.sso.SingleSignOnManager;

/**
 * @author Paul Ferraro
 */
public class DistributableSingleSignOnManager implements SingleSignOnManager {

    private final SSOManager<ElytronAuthentication, String, Map.Entry<String, URI>, LocalSSOContext, Batch> manager;

    public DistributableSingleSignOnManager(SSOManager<ElytronAuthentication, String, Map.Entry<String, URI>, LocalSSOContext, Batch> manager) {
        this.manager = manager;
    }

    @Override
    public SingleSignOn create(String mechanismName, SecurityIdentity identity) {
        String id = this.manager.createIdentifier();
        Batcher<Batch> batcher = this.manager.getBatcher();
        // Batch will be closed when SSO is closed
        @SuppressWarnings("resource")
        Batch batch = batcher.createBatch();
        try {
            SSO<ElytronAuthentication, String, Entry<String, URI>, LocalSSOContext> sso = this.manager.createSSO(id, new ElytronAuthentication(mechanismName, identity.getPrincipal().getName()));
            sso.getLocalContext().setSecurityIdentity(identity);
            return new DistributableSingleSignOn(sso, batcher, batcher.suspendBatch());
        } catch (RuntimeException | Error e) {
            batch.discard();
            batch.close();
            throw e;
        }
    }

    @Override
    public SingleSignOn find(String id) {
        Batcher<Batch> batcher = this.manager.getBatcher();
        // Batch will be closed when SSO is closed
        Batch batch = batcher.createBatch();
        boolean close = true;
        try {
            SSO<ElytronAuthentication, String, Entry<String, URI>, LocalSSOContext> sso = this.manager.findSSO(id);
            if (sso == null) return null;
            close = false;
            return new DistributableSingleSignOn(sso, batcher, batcher.suspendBatch());
        } catch (RuntimeException | Error e) {
            batch.discard();
            throw e;
        } finally {
            if (close) {
                batch.close();
            }
        }
    }
}
