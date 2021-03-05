/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.session.oob;

import java.time.Duration;
import java.time.Instant;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SessionMetaData;

/**
 * Out-of-band session meta data implementation, for use outside the context of a request.
 * @author Paul Ferraro
 */
public class OOBSessionMetaData<L, B extends Batch> implements SessionMetaData {

    private final SessionManager<L, B> manager;
    private final String id;

    public OOBSessionMetaData(SessionManager<L, B> manager, String id) {
        this.manager = manager;
        this.id = id;
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
    public Duration getMaxInactiveInterval() {
        try (B batch = this.manager.getBatcher().createBatch()) {
            ImmutableSession session = this.manager.readSession(this.id);
            if (session == null) {
                throw new IllegalStateException();
            }
            return session.getMetaData().getMaxInactiveInterval();
        }
    }

    @Override
    public void setLastAccess(Instant startTime, Instant endTime) {
        throw new IllegalStateException();
    }

    @Override
    public void setMaxInactiveInterval(Duration duration) {
        try (B batch = this.manager.getBatcher().createBatch()) {
            try (Session<L> session = this.manager.findSession(this.id)) {
                if (session == null) {
                    throw new IllegalStateException();
                }
                session.getMetaData().setMaxInactiveInterval(duration);
            }
        }
    }
}
