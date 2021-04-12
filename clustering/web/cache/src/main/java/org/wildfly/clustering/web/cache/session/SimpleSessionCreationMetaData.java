/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
import java.time.temporal.ChronoField;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Paul Ferraro
 */
public class SimpleSessionCreationMetaData implements SessionCreationMetaData {

    private final Instant creationTime;
    private volatile Duration maxInactiveInterval = Duration.ZERO;
    private volatile boolean newSession;
    private final AtomicBoolean valid = new AtomicBoolean(true);

    public SimpleSessionCreationMetaData() {
        this.creationTime = Instant.now();
        this.newSession = true;
    }

    public SimpleSessionCreationMetaData(Instant creationTime) {
        // Only retain millisecond precision
        this.creationTime = (creationTime.getNano() % 1_000_000 > 0) ? creationTime.with(ChronoField.MILLI_OF_SECOND, creationTime.get(ChronoField.MILLI_OF_SECOND)) : creationTime;
        this.newSession = false;
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
    public Duration getMaxInactiveInterval() {
        return this.maxInactiveInterval;
    }

    @Override
    public void setMaxInactiveInterval(Duration duration) {
        this.maxInactiveInterval = duration;
    }

    @Override
    public boolean isValid() {
        return this.valid.get();
    }

    @Override
    public boolean invalidate() {
        return this.valid.compareAndSet(true, false);
    }

    @Override
    public void close() {
        this.newSession = false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.getClass().getSimpleName()).append('{');
        builder.append("created=").append(this.creationTime);
        builder.append(", max-inactive-interval=").append(this.maxInactiveInterval);
        return builder.append('}').toString();
    }
}
