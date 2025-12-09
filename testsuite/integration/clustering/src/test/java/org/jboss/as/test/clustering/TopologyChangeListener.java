/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * @author Paul Ferraro
 */
public interface TopologyChangeListener {

    Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

    /**
     * Waits until the specified topology is established on the specified cache.
     * @param containerName the cache container name
     * @param cacheName the cache name
     * @param topology the desired topology
     * @param timeout a timeout for which to wait for the desired topology
     * @throws TimeoutException if the topology could not be established within the desired timeout
     */
    void establishTopology(String containerName, String cacheName, Set<String> topology, Duration timeout) throws TimeoutException;
}
