/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ejb.infinispan;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jboss.as.clustering.concurrent.Scheduler;
import org.jboss.as.clustering.infinispan.invoker.Evictor;
import org.wildfly.clustering.ejb.Batch;
import org.wildfly.clustering.ejb.Batcher;
import org.wildfly.clustering.ejb.Bean;

/**
 * Schedules a bean for eviction.
 *
 * @author Paul Ferraro
 *
 * @param <G> the group identifier type
 * @param <I> the bean identifier type
 * @param <T> the bean type
 */
public class BeanEvictionScheduler<G, I, T> implements Scheduler<Bean<G, I, T>> {

    private final Set<I> evictionQueue = new LinkedHashSet<>();
    final Batcher batcher;
    final Evictor<I> evictor;
    private final PassivationConfiguration<?> config;

    public BeanEvictionScheduler(Batcher batcher, Evictor<I> evictor, PassivationConfiguration<?> config) {
        this.batcher = batcher;
        this.evictor = evictor;
        this.config = config;
    }

    @Override
    public void cancel(Bean<G, I, T> bean) {
        synchronized (this.evictionQueue) {
            this.evictionQueue.remove(bean.getId());
        }
    }

    @Override
    public void schedule(Bean<G, I, T> bean) {
        synchronized (this.evictionQueue) {
            this.evictionQueue.add(bean.getId());
            // Trigger eviction of oldest bean if necessary
            if (this.evictionQueue.size() > this.config.getConfiguration().getMaxSize()) {
                Iterator<I> beans = this.evictionQueue.iterator();
                this.config.getExecutor().execute(new EvictionTask(beans.next()));
                beans.remove();
            }
        }
    }

    @Override
    public void close() {
        synchronized (this.evictionQueue) {
            this.evictionQueue.clear();
        }
    }

    private class EvictionTask implements Runnable {
        private final I id;

        EvictionTask(I id) {
            this.id = id;
        }

        @Override
        public void run() {
            Batch batch = BeanEvictionScheduler.this.batcher.startBatch();
            boolean success = false;
            try {
                InfinispanEjbLogger.ROOT_LOGGER.tracef("Evicting stateful session bean %s", this.id);
                BeanEvictionScheduler.this.evictor.evict(this.id);
                success = true;
            } finally {
                if (success) {
                    batch.close();
                } else {
                    batch.discard();
                }
            }
        }
    }
}
