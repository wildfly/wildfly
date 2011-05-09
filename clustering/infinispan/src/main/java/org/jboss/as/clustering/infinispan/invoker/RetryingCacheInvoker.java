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

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.remoting.transport.jgroups.SuspectException;
import org.infinispan.util.concurrent.TimeoutException;
import org.jboss.logging.Logger;

/**
 * A cache invoker implementation that retries after a specified set of intervals upon timeout or suspect.
 *
 * @author Paul Ferraro
 */
public class RetryingCacheInvoker implements CacheInvoker {
    private static final Logger log = Logger.getLogger(RetryingCacheInvoker.class);

    private final int[] backOffIntervals;

    private volatile boolean forceSynchronous = false;

    /**
     * Creates a new RetryingCacheInvoker.
     *
     * @param backOffIntervals specifies the sleep intervals between retries, and implicitly, the number of retries
     */
    public RetryingCacheInvoker(int... backOffIntervals) {
        this.backOffIntervals = backOffIntervals;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.ha.web.tomcat.service.session.distributedcache.impl.CacheInvoker#invoke(org.infinispan.Cache,
     *      org.jboss.ha.web.tomcat.service.session.distributedcache.impl.CacheInvoker.Operation)
     */
    @Override
    public <K, V, R> R invoke(Cache<K, V> cache, Operation<K, V, R> operation) {
        Exception exception = null;

        for (int i = 0; i <= this.backOffIntervals.length; ++i) {
            if (this.forceSynchronous) {
                cache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS);
            }

            try {
                return operation.invoke(cache);
            } catch (TimeoutException e) {
                exception = e;
            } catch (SuspectException e) {
                exception = e;
            }

            if (i < this.backOffIntervals.length) {
                int delay = this.backOffIntervals[i];

                try {
                    if (log.isTraceEnabled()) {
                        log.trace(String.format("Cache operation failed.  Retrying in %d ms", Integer.valueOf(delay)),
                                exception);
                    }

                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        throw new RuntimeException(String.format("Aborting cache operation after %d retries.",
                Integer.valueOf(this.backOffIntervals.length + 1)), exception);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.ha.web.tomcat.service.session.distributedcache.impl.CacheInvoker#setForceSynchronous(boolean)
     */
    @Override
    public void setForceSynchronous(boolean forceSynchronous) {
        this.forceSynchronous = forceSynchronous;
    }
}
