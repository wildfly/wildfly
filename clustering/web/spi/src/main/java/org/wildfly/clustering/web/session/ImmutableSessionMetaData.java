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
package org.wildfly.clustering.web.session;

import java.time.Duration;
import java.time.Instant;

/**
 * Abstraction for immutable meta information about a web session.
 * @author Paul Ferraro
 */
public interface ImmutableSessionMetaData {
    /**
     * Indicates whether or not this session was created by the current thread.
     * @return true, if this session is new, false otherwise
     */
    boolean isNew();

    /**
     * Indicates whether or not this session was previously idle for longer than the maximum inactive interval.
     * @return true, if this session is expired, false otherwise
     */
    boolean isExpired();

    /**
     * Returns the time this session was created.
     * @return the time this session was created
     */
    Instant getCreationTime();

    /**
     * Returns the time this session was last accessed.
     * @return the time this session was last accessed
     */
    Instant getLastAccessedTime();

    /**
     * Returns the time interval, using the specified unit, after which this session will expire.
     * @param unit a time unit
     * @return a time interval
     */
    Duration getMaxInactiveInterval();
}
