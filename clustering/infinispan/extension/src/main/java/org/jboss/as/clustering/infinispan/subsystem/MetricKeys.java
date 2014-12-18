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

package org.jboss.as.clustering.infinispan.subsystem;

/**
 * @author Tristan Tarrant
 * @author Richard Achmatowicz
 */
public class MetricKeys {
    // cache container
    public static final String CACHE_MANAGER_STATUS = "cache-manager-status";
    public static final String IS_COORDINATOR = "is-coordinator";
    public static final String COORDINATOR_ADDRESS = "coordinator-address";
    public static final String LOCAL_ADDRESS = "local-address";
    public static final String CLUSTER_NAME = "cluster-name";
    // cache
    public static final String BYTES_READ = "bytes-read";
    public static final String BYTES_WRITTEN = "bytes-written";
    public static final String CACHE_STATUS = "cache-status";
    // LockManager
    public static final String NUMBER_OF_LOCKS_AVAILABLE = "number-of-locks-available";
    public static final String NUMBER_OF_LOCKS_HELD = "number-of-locks-held";
    public static final String CURRENT_CONCURRENCY_LEVEL = "current-concurrency-level";
    // cache management interceptor
    public static final String AVERAGE_READ_TIME = "average-read-time";
    public static final String AVERAGE_WRITE_TIME = "average-write-time";
    public static final String ELAPSED_TIME = "elapsed-time";
    public static final String EVICTIONS = "evictions";
    public static final String HIT_RATIO = "hit-ratio";
    public static final String HITS = "hits";
    public static final String MISSES = "misses";
    public static final String NUMBER_OF_ENTRIES = "number-of-entries";
    public static final String READ_WRITE_RATIO = "read-write-ratio";
    public static final String REMOVE_HITS = "remove-hits";
    public static final String REMOVE_MISSES = "remove-misses";
    public static final String STORES = "stores";
    public static final String TIME_SINCE_RESET = "time-since-reset";
    // transaction manager
    public static final String COMMITS = "commits";
    public static final String PREPARES = "prepares";
    public static final String ROLLBACKS = "rollbacks";
    // invalidation interceptor
    public static final String INVALIDATIONS = "invalidations";
    // passivation interceptor
    public static final String PASSIVATIONS = "passivations";
    // activation interceptor
    public static final String ACTIVATIONS = "activations";
    public static final String CACHE_LOADER_LOADS = "cache-loader-loads";
    public static final String CACHE_LOADER_MISSES = "cache-loader-misses";
    public static final String CACHE_LOADER_STORES = "cache-loader-stores";

    public static final String JOIN_COMPLETE = "join-complete";
    public static final String STATE_TRANSFER_IN_PROGRESS = "state-transfer-in-progress";
    // Rpc manager
    public static final String AVERAGE_REPLICATION_TIME = "average-replication-time";
    public static final String REPLICATION_COUNT = "replication-count";
    public static final String REPLICATION_FAILURES = "replication-failures";
    public static final String SUCCESS_RATIO = "success-ratio";
}
