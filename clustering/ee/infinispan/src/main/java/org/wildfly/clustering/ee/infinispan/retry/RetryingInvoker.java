/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
public class RetryingInvoker extends org.wildfly.clustering.ee.retry.RetryingInvoker {

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
