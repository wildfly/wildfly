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

/**
 * Composite view of the meta data of a session, combining volatile and static aspects.
 * @author Paul Ferraro
 */
public class CompositeSessionMetaData implements InvalidatableSessionMetaData {

    private final SessionCreationMetaData creationMetaData;
    private final SessionAccessMetaData accessMetaData;

    public CompositeSessionMetaData(SessionCreationMetaData creationMetaData, SessionAccessMetaData accessMetaData) {
        this.creationMetaData = creationMetaData;
        this.accessMetaData = accessMetaData;
    }

    @Override
    public boolean isNew() {
        return this.creationMetaData.isNew();
    }

    @Override
    public boolean isValid() {
        return this.creationMetaData.isValid();
    }

    @Override
    public boolean invalidate() {
        return this.creationMetaData.invalidate();
    }

    @Override
    public Instant getCreationTime() {
        return this.creationMetaData.getCreationTime();
    }

    @Override
    public Instant getLastAccessStartTime() {
        return this.getCreationTime().plus(this.accessMetaData.getSinceCreationDuration());
    }

    @Override
    public Instant getLastAccessEndTime() {
        return this.getLastAccessStartTime().plus(this.accessMetaData.getLastAccessDuration());
    }

    @Override
    public Duration getMaxInactiveInterval() {
        return this.creationMetaData.getMaxInactiveInterval();
    }

    @Override
    public void setLastAccess(Instant startTime, Instant endTime) {
        Instant creationTime = this.creationMetaData.getCreationTime();
        this.accessMetaData.setLastAccessDuration(!startTime.equals(creationTime) ? Duration.between(creationTime, startTime) : Duration.ZERO, Duration.between(startTime, endTime));
    }

    @Override
    public void setMaxInactiveInterval(Duration duration) {
        this.creationMetaData.setMaxInactiveInterval(duration.isNegative() ? Duration.ZERO : duration);
    }

    @Override
    public void close() {
        this.creationMetaData.close();
    }
}
