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

package org.wildfly.clustering.web.infinispan.session;

import java.time.Duration;
import java.time.Instant;

import org.wildfly.clustering.web.session.SessionExpirationMetaData;

/**
 * Immutable view of the expiration metadata for a session.
 * @author Paul Ferraro
 */
public class SimpleSessionExpirationMetaData implements SessionExpirationMetaData {

    private final Instant lastAccessEndTime;
    private final Duration maxInactiveInterval;

    public SimpleSessionExpirationMetaData(SessionExpirationMetaData metaData) {
        this(metaData.getMaxInactiveInterval(), metaData.getLastAccessEndTime());
    }

    SimpleSessionExpirationMetaData(Duration maxInactiveInterval, Instant lastAccessEndTime) {
        this.maxInactiveInterval = maxInactiveInterval;
        this.lastAccessEndTime = lastAccessEndTime;
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
