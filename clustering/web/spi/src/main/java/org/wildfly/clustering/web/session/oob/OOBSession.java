/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.session.oob;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionAttributes;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SessionMetaData;

/**
 * Out-of-band session implementation, for use outside the context of a request.
 * @author Paul Ferraro
 */
public class OOBSession<L, B extends Batch> implements Session<L>, SessionMetaData, SessionAttributes {

    private final SessionManager<L, B> manager;
    private final String id;
    private final L localContext;

    public OOBSession(SessionManager<L, B> manager, String id, L localContext) {
        this.manager = manager;
        this.id = id;
        this.localContext = localContext;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public boolean isValid() {
        try (B batch = this.manager.getBatcher().createBatch()) {
            return this.manager.readSession(this.id) != null;
        }
    }

    @Override
    public SessionMetaData getMetaData() {
        return this;
    }

    @Override
    public void invalidate() {
        try (B batch = this.manager.getBatcher().createBatch()) {
            try (Session<L> session = this.manager.findSession(this.id)) {
                if (session == null) {
                    throw new IllegalStateException();
                }
                session.invalidate();
            }
        }
    }

    @Override
    public SessionAttributes getAttributes() {
        return this;
    }

    @Override
    public L getLocalContext() {
        return this.localContext;
    }

    @Override
    public void close() {
        // OOB session have no concept of lifecycle
    }

    @Override
    public boolean isNew() {
        try (B batch = this.manager.getBatcher().createBatch()) {
            ImmutableSession session = this.manager.readSession(this.id);
            if (session == null) {
                throw new IllegalStateException();
            }
            return session.getMetaData().isNew();
        }
    }

    @Override
    public boolean isExpired() {
        try (B batch = this.manager.getBatcher().createBatch()) {
            ImmutableSession session = this.manager.readSession(this.id);
            if (session == null) {
                throw new IllegalStateException();
            }
            return session.getMetaData().isExpired();
        }
    }

    @Override
    public Instant getCreationTime() {
        try (B batch = this.manager.getBatcher().createBatch()) {
            ImmutableSession session = this.manager.readSession(this.id);
            if (session == null) {
                throw new IllegalStateException();
            }
            return session.getMetaData().getCreationTime();
        }
    }

    @Override
    public Instant getLastAccessStartTime() {
        try (B batch = this.manager.getBatcher().createBatch()) {
            ImmutableSession session = this.manager.readSession(this.id);
            if (session == null) {
                throw new IllegalStateException();
            }
            return session.getMetaData().getLastAccessStartTime();
        }
    }

    @Override
    public Instant getLastAccessEndTime() {
        try (B batch = this.manager.getBatcher().createBatch()) {
            ImmutableSession session = this.manager.readSession(this.id);
            if (session == null) {
                throw new IllegalStateException();
            }
            return session.getMetaData().getLastAccessEndTime();
        }
    }

    @Override
    public Duration getTimeout() {
        try (B batch = this.manager.getBatcher().createBatch()) {
            ImmutableSession session = this.manager.readSession(this.id);
            if (session == null) {
                throw new IllegalStateException();
            }
            return session.getMetaData().getTimeout();
        }
    }

    @Override
    public void setLastAccess(Instant startTime, Instant endTime) {
        throw new IllegalStateException();
    }

    @Override
    public void setTimeout(Duration duration) {
        try (B batch = this.manager.getBatcher().createBatch()) {
            try (Session<L> session = this.manager.findSession(this.id)) {
                if (session == null) {
                    throw new IllegalStateException();
                }
                session.getMetaData().setTimeout(duration);
            }
        }
    }

    @Override
    public Set<String> getAttributeNames() {
        try (B batch = this.manager.getBatcher().createBatch()) {
            ImmutableSession session = this.manager.readSession(this.id);
            if (session == null) {
                throw new IllegalStateException();
            }
            return session.getAttributes().getAttributeNames();
        }
    }

    @Override
    public Object getAttribute(String name) {
        try (B batch = this.manager.getBatcher().createBatch()) {
            ImmutableSession session = this.manager.readSession(this.id);
            if (session == null) {
                throw new IllegalStateException();
            }
            return session.getAttributes().getAttribute(name);
        }
    }

    @Override
    public Object removeAttribute(String name) {
        try (B batch = this.manager.getBatcher().createBatch()) {
            try (Session<L> session = this.manager.findSession(this.id)) {
                if (session == null) {
                    throw new IllegalStateException();
                }
                return session.getAttributes().removeAttribute(name);
            }
        }
    }

    @Override
    public Object setAttribute(String name, Object value) {
        try (B batch = this.manager.getBatcher().createBatch()) {
            try (Session<L> session = this.manager.findSession(this.id)) {
                if (session == null) {
                    throw new IllegalStateException();
                }
                return session.getAttributes().setAttribute(name, value);
            }
        }
    }
}
