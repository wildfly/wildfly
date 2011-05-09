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

/**
 * Encapsulates the types of data about a session that can be stored to a distributed cache, except for the attributes. How to
 * obtain the attributes, if available, would be specified via a subinterface.
 * @author Brian Stansberry
 */
public interface OutgoingDistributableSessionData {
    /**
     * Gets the session id with any appended jvmRoute info removed.
     */
    String getRealId();

    /**
     * Gets the session's version.
     */
    int getVersion();

    /**
     * Gets the timestamp of the most recent session access.
     */
    Long getTimestamp();

    /**
     * Gets the other session metadata besides the version and timestamp.
     */
    DistributableSessionMetadata getMetadata();
}
