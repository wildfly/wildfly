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

package org.jboss.as.ejb3.cache.spi;

/**
 * Object that receives notifications as a {@link BackingCache} starts and stops.
 *
 * @author Brian Stansberry
 */
public interface BackingCacheLifecycleListener {
    public enum LifecycleState {
        /** The cache is starting */
        STARTING,
        /** The cache has started */
        STARTED,
        /** The cache is stopping */
        STOPPING,
        /** The cache has stopped */
        STOPPED,
        /** A failure has occurred in start() or stop() */
        FAILED
    };

    /**
     * Notifies the listener that the {@link BackingCache} has entered a new lifecycle state.
     *
     * @param newState the new state. Cannot be <code>null</code>.
     */
    void lifecycleChange(LifecycleState newState);
}
