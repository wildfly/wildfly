/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.clustering.web;

import java.util.Map;

/**
 * Encapsulates the four types of data about a session that can be retrieved from a distributed cache.
 * @author Brian Stansberry
 */
public interface IncomingDistributableSessionData {
    /**
     * Gets the session's version.
     */
    int getVersion();

    /**
     * Gets the timestamp of the most recent session access.
     */
    long getTimestamp();

    /**
     * Gets the other session metadata besides the version and timestamp.
     */
    DistributableSessionMetadata getMetadata();

    /**
     * Gets whether it is safe to invoke {@link #getSessionAttributes()} on this object.
     * @return <code>true</code> if {@link #getSessionAttributes()} will return a map; <code>false</code> if it will throw an IllegalStateException.
     */
    boolean providesSessionAttributes();

    /**
     * Returns the session's attribute map, or throws an IllegalStateException if {@link #providesSessionAttributes()} would
     * return <code>false</code>.
     * @return the session attribute map. Will not return <code>null</code>
     * @throws IllegalStateException if {@link #providesSessionAttributes()} would return <code>false</code>.
     */
    Map<String, Object> getSessionAttributes();
}
