/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.clustering.infinispan.invoker;

import static org.jboss.as.clustering.infinispan.InfinispanLogger.ROOT_LOGGER;
import static org.jboss.as.clustering.infinispan.InfinispanMessages.MESSAGES;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.remoting.transport.jgroups.SuspectException;
import org.infinispan.util.concurrent.TimeoutException;

/**
 * A cache invoker implementation that retries after a specified set of intervals upon timeout or suspect.
 * If the invocation includes Flag.FAIL_SILENTLY, this will only be applied to the last attempt.
 * @author Paul Ferraro
 */
public class RetryingCacheInvoker implements CacheInvoker {

    private final CacheInvoker invoker;
    private final int[] backOffIntervals;

    /**
     * Creates a new RetryingCacheInvoker.
     *
     * @param backOffIntervals specifies the sleep intervals between retries, and implicitly, the number of retries
     */
    public RetryingCacheInvoker(int... backOffIntervals) {
        this(new SimpleCacheInvoker(), backOffIntervals);
    }

    public RetryingCacheInvoker(CacheInvoker invoker, int... backOffIntervals) {
        this.invoker = invoker;
        this.backOffIntervals = backOffIntervals;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.as.clustering.infinispan.invoker.CacheInvoker#invoke(org.infinispan.Cache, org.jboss.as.clustering.infinispan.invoker.CacheInvoker.Operation)
     */
    @Override
    public <K, V, R> R invoke(Cache<K, V> cache, Operation<K, V, R> operation, Flag... allFlags) {
        Flag[] attemptFlags = null;
        // attemptFlags = allFlags - Flag.FAIL_SILENTLY
        if ((allFlags != null) && (allFlags.length > 0)) {
            Set<Flag> flags = EnumSet.noneOf(Flag.class);
            flags.addAll(Arrays.asList(allFlags));
            flags.remove(Flag.FAIL_SILENTLY);
            attemptFlags = flags.toArray(new Flag[flags.size()]);
        }

        Exception exception = null;

        for (int i = 0; i <= this.backOffIntervals.length; ++i) {
            // Make sure Flag.FAIL_SILENTLY, if specified, is applied to the last try only
            try {
                return this.invoker.invoke(cache, operation, (i < this.backOffIntervals.length) ? attemptFlags : allFlags);
            } catch (TimeoutException e) {
                exception = e;
            } catch (SuspectException e) {
                exception = e;
            }

            if (i < this.backOffIntervals.length) {
                int delay = this.backOffIntervals[i];

                try {
                    if (ROOT_LOGGER.isTraceEnabled()) {
                        ROOT_LOGGER.tracef(exception, "Cache operation failed.  Retrying in %d ms", Integer.valueOf(delay));
                    }

                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        throw MESSAGES.abortingCacheOperation(exception, Integer.valueOf(this.backOffIntervals.length + 1));
    }
}
