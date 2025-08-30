/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.user.elytron;

import java.net.URI;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.SuspendedBatch;
import org.wildfly.clustering.context.Context;
import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.session.user.User;
import org.wildfly.clustering.session.user.UserSessions;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.cache.CachedIdentity;
import org.wildfly.security.http.util.sso.SingleSignOn;

/**
 * @author Paul Ferraro
 */
public class DistributableSingleSignOn implements SingleSignOn {

    private final User<CachedIdentity, AtomicReference<SecurityIdentity>, String, Map.Entry<String, URI>> user;
    private final SuspendedBatch suspendedBatch;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public DistributableSingleSignOn(User<CachedIdentity, AtomicReference<SecurityIdentity>, String, Map.Entry<String, URI>> sso, SuspendedBatch suspendedBatch) {
        this.user = sso;
        this.suspendedBatch = suspendedBatch;
    }

    @Override
    public String getId() {
        return this.user.getId();
    }

    @Override
    public String getMechanism() {
        return this.user.getPersistentContext().getMechanismName();
    }

    @Override
    public boolean isProgrammatic() {
        return this.user.getPersistentContext().isProgrammatic();
    }

    @Override
    public String getName() {
        return this.user.getPersistentContext().getName();
    }

    @Override
    public SecurityIdentity getIdentity() {
        return this.user.getTransientContext().get();
    }

    @Override
    public Map<String, Map.Entry<String, URI>> getParticipants() {
        UserSessions<String, Map.Entry<String, URI>> sessions = this.user.getSessions();
        Map<String, Map.Entry<String, URI>> participants = new HashMap<>();
        for (String deployment : sessions.getDeployments()) {
            participants.put(deployment, sessions.getSession(deployment));
        }
        return Collections.unmodifiableMap(participants);
    }

    @Override
    public void setIdentity(SecurityIdentity identity) {
        this.user.getTransientContext().set(identity);
    }

    @Override
    public boolean addParticipant(String applicationId, String sessionId, URI participant) {
        try (Context<Batch> context = this.suspendedBatch.resumeWithContext()) {
            return this.user.getSessions().addSession(applicationId, new AbstractMap.SimpleImmutableEntry<>(sessionId, participant));
        }
    }

    @Override
    public Map.Entry<String, URI> removeParticipant(String applicationId) {
        try (Context<Batch> context = this.suspendedBatch.resumeWithContext()) {
            return this.user.getSessions().removeSession(applicationId);
        }
    }

    @Override
    public void invalidate() {
        // In some cases, Elytron neglects to close invalidated SSO, so close it here
        this.close(User::invalidate);
    }

    @Override
    public void close() {
        this.close(Consumer.empty());
    }

    private void close(Consumer<User<CachedIdentity, AtomicReference<SecurityIdentity>, String, Map.Entry<String, URI>>> action) {
        if (this.closed.compareAndSet(false, true)) {
            try (Context<Batch> context = this.suspendedBatch.resumeWithContext()) {
                try (Batch batch = context.get()) {
                    try (User<CachedIdentity, AtomicReference<SecurityIdentity>, String, Map.Entry<String, URI>> user = this.user) {
                        action.accept(user);
                    }
                }
            }
        }
    }
}
