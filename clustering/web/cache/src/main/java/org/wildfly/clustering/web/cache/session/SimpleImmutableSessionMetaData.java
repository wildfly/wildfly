/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.cache.session;

import java.time.Duration;
import java.time.Instant;

import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * An immutable "snapshot" of a session's meta-data which can be accessed outside the scope of a transaction.
 * @author Paul Ferraro
 */
public class SimpleImmutableSessionMetaData implements ImmutableSessionMetaData {

    private final boolean newSession;
    private final Instant creationTime;
    private final Instant lastAccessStartTime;
    private final Instant lastAccessEndTime;
    private final Duration maxInactiveInterval;

    public SimpleImmutableSessionMetaData(ImmutableSessionMetaData metaData) {
        this.newSession = metaData.isNew();
        this.creationTime = metaData.getCreationTime();
        this.lastAccessStartTime = metaData.getLastAccessStartTime();
        this.lastAccessEndTime = metaData.getLastAccessEndTime();
        this.maxInactiveInterval = metaData.getMaxInactiveInterval();
    }

    @Override
    public boolean isNew() {
        return this.newSession;
    }

    @Override
    public Instant getCreationTime() {
        return this.creationTime;
    }

    @Override
    public Instant getLastAccessStartTime() {
        return this.lastAccessStartTime;
    }

    @Override
    public Instant getLastAccessEndTime() {
        return this.lastAccessEndTime;
    }

    @Override
    public Duration getMaxInactiveInterval() {
        return this.maxInactiveInterval;
    }
}
