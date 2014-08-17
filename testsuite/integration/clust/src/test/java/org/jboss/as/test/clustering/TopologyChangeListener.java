/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering;

/**
 * @author Paul Ferraro
 */
public interface TopologyChangeListener {
    /**
     * Waits until the specified topology is established on the specified cache.
     * @param containerName the cache container name
     * @param cacheName the cache name
     * @param nodes the anticipated topology
     * @throws InterruptedException if topology to stabilize did not stabilize within a reasonable amount of time - or the process was interrupted.
     */
    void establishTopology(String containerName, String cacheName, String... nodes) throws InterruptedException;
}
