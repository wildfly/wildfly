/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.user.elytron;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.SuspendedBatch;
import org.wildfly.clustering.context.Context;
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
    private static final Logger LOGGER = Logger.getLogger(DistributableSingleSignOnManager.class);

    private final UserManager<CachedIdentity, AtomicReference<SecurityIdentity>, String, Map.Entry<String, URI>> manager;

    public DistributableSingleSignOnManager(UserManager<CachedIdentity, AtomicReference<SecurityIdentity>, String, Map.Entry<String, URI>> manager) {
        this.manager = manager;
    }

    @Override
    public SingleSignOn create(String mechanismName, boolean programmatic, SecurityIdentity identity) {
        String id = this.manager.getIdentifierFactory().get();
        // Batch will be closed when SSO is closed
        SuspendedBatch suspended = this.manager.getBatchFactory().get().suspend();
        try (Context<Batch> context = suspended.resumeWithContext()) {
            User<CachedIdentity, AtomicReference<SecurityIdentity>, String, Map.Entry<String, URI>> user = this.manager.createUser(id, new CachedIdentity(mechanismName, programmatic, identity));
            user.getTransientContext().set(identity);
            return new DistributableSingleSignOn(user, suspended);
        } catch (RuntimeException | Error e) {
            rollback(suspended::resume);
            throw e;
        }
    }

    @Override
    public SingleSignOn find(String id) {
        // Batch will be closed when SSO is closed
        SuspendedBatch suspended = this.manager.getBatchFactory().get().suspend();
        try (Context<Batch> context = suspended.resumeWithContext()) {
            User<CachedIdentity, AtomicReference<SecurityIdentity>, String, Map.Entry<String, URI>> user = this.manager.findUser(id);
            return (user != null) ? new DistributableSingleSignOn(user, suspended) : rollback(context);
        } catch (RuntimeException | Error e) {
            rollback(suspended::resume);
            throw e;
        }
    }

    private static SingleSignOn rollback(Supplier<Batch> batchProvider) {
        try (Batch batch = batchProvider.get()) {
            batch.discard();
        } catch (RuntimeException | Error e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        return null;
    }
}
