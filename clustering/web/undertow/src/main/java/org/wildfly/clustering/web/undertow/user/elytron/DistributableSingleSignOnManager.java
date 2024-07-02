/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.user.elytron;

import java.net.URI;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.session.user.User;
import org.wildfly.clustering.session.user.UserManager;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.cache.CachedIdentity;
import org.wildfly.security.http.util.sso.SingleSignOn;
import org.wildfly.security.http.util.sso.SingleSignOnManager;

/**
 * @author Paul Ferraro
 */
public class DistributableSingleSignOnManager implements SingleSignOnManager {

    private final UserManager<CachedIdentity, AtomicReference<SecurityIdentity>, String, Map.Entry<String, URI>> manager;

    public DistributableSingleSignOnManager(UserManager<CachedIdentity, AtomicReference<SecurityIdentity>, String, Map.Entry<String, URI>> manager) {
        this.manager = manager;
    }

    @Override
    public SingleSignOn create(String mechanismName, boolean programmatic, SecurityIdentity identity) {
        String id = this.manager.getIdentifierFactory().get();
        // Batch will be closed when SSO is closed
        @SuppressWarnings("resource")
        Batch batch = this.manager.getBatchFactory().get();
        try {
            User<CachedIdentity, AtomicReference<SecurityIdentity>, String, Entry<String, URI>> user = this.manager.createUser(id, new CachedIdentity(mechanismName, programmatic, identity));
            user.getTransientContext().set(identity);
            return new DistributableSingleSignOn(user, batch.suspend());
        } catch (RuntimeException | Error e) {
            batch.discard();
            batch.close();
            throw e;
        }
    }

    @Override
    public SingleSignOn find(String id) {
        // Batch will be closed when SSO is closed
        Batch batch = this.manager.getBatchFactory().get();
        boolean close = true;
        try {
            User<CachedIdentity, AtomicReference<SecurityIdentity>, String, Entry<String, URI>> user = this.manager.findUser(id);
            close = (user == null);
            return (user != null) ? new DistributableSingleSignOn(user, batch.suspend()) : null;
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
