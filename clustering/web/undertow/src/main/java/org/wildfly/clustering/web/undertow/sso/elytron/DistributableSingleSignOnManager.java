/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import org.wildfly.security.cache.CachedIdentity;
import org.wildfly.security.http.util.sso.SingleSignOn;
import org.wildfly.security.http.util.sso.SingleSignOnManager;

/**
 * @author Paul Ferraro
 */
public class DistributableSingleSignOnManager implements SingleSignOnManager {

    private final SSOManager<CachedIdentity, String, Map.Entry<String, URI>, LocalSSOContext, Batch> manager;

    public DistributableSingleSignOnManager(SSOManager<CachedIdentity, String, Map.Entry<String, URI>, LocalSSOContext, Batch> manager) {
        this.manager = manager;
    }

    @Override
    public SingleSignOn create(String mechanismName, boolean programmatic, SecurityIdentity identity) {
        String id = this.manager.getIdentifierFactory().get();
        Batcher<Batch> batcher = this.manager.getBatcher();
        // Batch will be closed when SSO is closed
        @SuppressWarnings("resource")
        Batch batch = batcher.createBatch();
        try {
            SSO<CachedIdentity, String, Entry<String, URI>, LocalSSOContext> sso = this.manager.createSSO(id, new CachedIdentity(mechanismName, programmatic, identity));
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
            SSO<CachedIdentity, String, Entry<String, URI>, LocalSSOContext> sso = this.manager.findSSO(id);
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
