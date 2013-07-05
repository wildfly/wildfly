/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.cluster;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * ClusterSubsystemMessages
 *
 * logging id range: 20710 - 20719
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface ClusterSubsystemMessages {
    /**
     * The messages.
     */
    ClusterSubsystemMessages MESSAGES = Messages.getBundle(ClusterSubsystemMessages.class);

    /**
     * Creates a message indicating that an unknown metric was passed to an attribute handler..
     *
     * @param metricName the unknown metric.
     *
     * @return a {@link String} for the error.
     */
    @Message(id = 20710, value = "Unknown metric %s")
    String unknownMetric(String metricName);

    /**
     * Creates a message indicating that the RPC service for a channel was not started.
     *
     * @param channelName the channel whose RPC service did not start.
     *
     * @return a {@link String} for the error.
     */
    @Message(id = 20711, value = "RPC service not started for channel %s")
    String rpcServiceNotStarted(String channelName);

    /**
     * Creates a message indicating that the RPC service for a channel was interrupted.
     *
     * @param channelName the channel whose RPC service did not start.
     *
     * @return a {@link String} for the error.
     */
    @Message(id = 20712, value = "RPC service call interrupted for channel %s")
    String interrupted(String channelName);

    /**
     * Creates a message describing group communication statistics..
     *
     * @param unicasts the number of unicast communications.
     * @param multicasts the number of multicast communications.
     * @param anycasts the number of anycast communications.
     *
     * @return a {@link String} describing the statistics.
     */
    @Message(id = 20713, value = "unicasts: %s, multicasts: %s, anycasts: %s")
    String clusterRPCStats(int unicasts, int multicasts, int anycasts);

    /**
     * Creates a message describing cache distribution statistics..
     *
     * @param entries the number of cache entries.
     *
     * @return a {@link String} describing the statistics.
     */
    @Message(id = 20714, value = "cache entries: %s")
    String cacheDistributionStats(long entries);

    /**
     * Creates a message describing cache operation statistics..
     *
     * @param hits the number of cache get hits.
     * @param misses the number of cache get misses.
     * @param stores the number of cache puts.
     * @param remove_hits the number of cache remove hits.
     * @param remove_misses the number of cache remove misses.
     *
     * @return a {@link String} describing the statistics.
     */
    @Message(id = 20715, value = "get(hits): %s, get(misses): %s, puts %s, remove(hits): %s, remove(misses): %s")
    String cacheOperationStats(long hits, long misses, long stores, long remove_hits, long remove_misses);

    /**
     * Creates a message describing cache RPC communication statistics..
     *
     * @param count the number of cache RPC communications.
     * @param failures the number of cache RPC communication failures.
     *
     * @return a {@link String} describing the statistics.
     */
    @Message(id = 20716, value = "RPC count: %s, RPC failures: %s")
    String cacheRPCStats(long count, long failures);

    /**
     * Creates a message describing cache transaction statistics..
     *
     * @param prepares the number of cache transaction prepares.
     * @param commits the number of cache transaction commits.
     * @param rollbacks the number of cache transaction commits.
     *
     * @return a {@link String} describing the statistics.
     */
    @Message(id = 20717, value = "prepares: %s, commits: %s, rollbacks: %s")
    String cacheTxnStats(long prepares, long commits, long rollbacks);
}
