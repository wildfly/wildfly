/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering;

/**
 * @author Paul Ferraro
 */
public interface TopologyChangeListener {

    long DEFAULT_TIMEOUT = 15000;

    /**
     * Waits until the specified topology is established on the specified cache.
     * @param containerName the cache container name
     * @param cacheName the cache name
     * @param nodes the anticipated topology
     * @throws InterruptedException if topology did not stabilize within a reasonable amount of time - or the process was interrupted.
     */
    void establishTopology(String containerName, String cacheName, long timeout, String... nodes) throws InterruptedException;
}
