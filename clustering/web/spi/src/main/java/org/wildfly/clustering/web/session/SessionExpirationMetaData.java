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

package org.wildfly.clustering.web.session;

import java.time.Duration;
import java.time.Instant;

/**
 * Session meta data that governs expiration.
 * @author Paul Ferraro
 */
public interface SessionExpirationMetaData {

    /**
     * Indicates whether or not this session was previously idle for longer than the maximum inactive interval.
     * @return true, if this session is expired, false otherwise
     */
    default boolean isExpired() {
        Duration maxInactiveInterval = this.getMaxInactiveInterval();
        return !maxInactiveInterval.isZero() ? this.getLastAccessEndTime().plus(maxInactiveInterval).isBefore(Instant.now()) : false;
    }

    /**
     * Returns the end time of the last request to access this session.
     * @return the end time of the last request to access this session.
     */
    Instant getLastAccessEndTime();

    /**
     * Returns the time interval since {@link #getLastAccessEndTime()} after which this session will expire.
     * @return a time interval
     */
    Duration getMaxInactiveInterval();
}
