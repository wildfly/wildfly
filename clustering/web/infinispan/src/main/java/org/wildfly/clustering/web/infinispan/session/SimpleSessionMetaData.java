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
package org.wildfly.clustering.web.infinispan.session;

import java.time.Duration;
import java.time.Instant;

/**
 * Composite view of the meta data of a session, combining volatile and static aspects.
 * @author Paul Ferraro
 */
public class SimpleSessionMetaData extends AbstractImmutableSessionMetaData implements InvalidatableSessionMetaData {

    private final SessionCreationMetaData creationMetaData;
    private final SessionAccessMetaData accessMetaData;

    public SimpleSessionMetaData(SessionCreationMetaData creationMetaData, SessionAccessMetaData accessMetaData) {
        this.creationMetaData = creationMetaData;
        this.accessMetaData = accessMetaData;
    }

    @Override
    public boolean isNew() {
        // We can implement this more efficiently than the super implementation
        return this.accessMetaData.getLastAccessedDuration().isZero();
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
    public Instant getLastAccessedTime() {
        return this.getCreationTime().plus(this.accessMetaData.getLastAccessedDuration());
    }

    @Override
    public Duration getMaxInactiveInterval() {
        return this.creationMetaData.getMaxInactiveInterval();
    }

    @Override
    public void setLastAccessedTime(Instant instant) {
        this.accessMetaData.setLastAccessedDuration(Duration.between(this.creationMetaData.getCreationTime(), instant));
    }

    @Override
    public void setMaxInactiveInterval(Duration duration) {
        this.creationMetaData.setMaxInactiveInterval(duration.isNegative() ? Duration.ZERO : duration);
    }
}
