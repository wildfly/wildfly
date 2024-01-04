/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.infinispan.retry;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;

/**
 * Retrying invoker whose retry intervals are auto-generated based an Infinispan cache configuration.
 * @author Paul Ferraro
 */
public class RetryingInvoker extends org.wildfly.clustering.ee.cache.retry.RetryingInvoker {

    public RetryingInvoker(Cache<?, ?> cache) {
        super(calculateRetryIntervals(cache.getCacheConfiguration()));
    }

    private static List<Duration> calculateRetryIntervals(Configuration config) {
        long timeout = config.locking().lockAcquisitionTimeout();
        List<Duration> intervals = new LinkedList<>();
        // Generate exponential back-off intervals
        for (long interval = timeout; interval > 1; interval /= 10) {
            intervals.add(0, Duration.ofMillis(interval));
        }
        intervals.add(0, Duration.ZERO);
        return intervals;
    }
}
