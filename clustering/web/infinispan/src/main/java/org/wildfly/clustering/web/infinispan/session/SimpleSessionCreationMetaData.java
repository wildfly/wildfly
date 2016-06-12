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

package org.wildfly.clustering.web.infinispan.session;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Paul Ferraro
 */
public class SimpleSessionCreationMetaData implements SessionCreationMetaData {

    private final Instant creationTime;
    private volatile Duration maxInactiveInterval = Duration.ZERO;
    private final AtomicBoolean valid = new AtomicBoolean(true);

    public SimpleSessionCreationMetaData() {
        this(Instant.now());
    }

    public SimpleSessionCreationMetaData(Instant creationTime) {
        this.creationTime = creationTime;
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
}
