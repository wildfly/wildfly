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

import java.security.AccessController;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.clustering.concurrent.Scheduler;
import org.jboss.as.clustering.infinispan.invoker.Evictor;
import org.jboss.logging.Logger;
import org.jboss.threads.JBossThreadFactory;
import org.wildfly.clustering.ejb.Batch;
import org.wildfly.clustering.ejb.Batcher;
import org.wildfly.clustering.ejb.Bean;
import org.wildfly.clustering.ejb.BeanPassivationConfiguration;
import org.wildfly.security.manager.GetAccessControlContextAction;

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
    static final Logger logger = Logger.getLogger(BeanEvictionScheduler.class);

    final Queue<I> evictionQueue = new ConcurrentLinkedQueue<>();
    final Batcher batcher;
    final Evictor<I> evictor;
    private final ExecutorService executor;
    private final BeanPassivationConfiguration config;

    public BeanEvictionScheduler(Batcher batcher, Evictor<I> evictor, BeanPassivationConfiguration config) {
        this(batcher, evictor, config, Executors.newCachedThreadPool(createThreadFactory()));
    }

    private static ThreadFactory createThreadFactory() {
        return new JBossThreadFactory(new ThreadGroup(BeanEvictionScheduler.class.getSimpleName()), Boolean.FALSE, null, "%G - %t", null, null, AccessController.doPrivileged(GetAccessControlContextAction.getInstance()));
    }

    public BeanEvictionScheduler(Batcher batcher, Evictor<I> evictor, BeanPassivationConfiguration config, ExecutorService executor) {
        this.batcher = batcher;
        this.evictor = evictor;
        this.config = config;
        this.executor = executor;
    }

    @Override
    public void cancel(Bean<G, I, T> session) {
        this.evictionQueue.remove(session.getId());
    }

    @Override
    public void schedule(Bean<G, I, T> session) {
        this.evictionQueue.add(session.getId());
        // Trigger eviction of oldest sessions if necessary
        while (this.evictionQueue.size() > this.config.getMaxSize()) {
            I id = this.evictionQueue.poll();
            if (id != null) {
                this.executor.submit(new EvictionTask(id));
            }
        }
    }

    @Override
    public void close() {
        this.evictionQueue.clear();
        this.executor.shutdown();
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
                logger.tracef("Evicting stateful session bean %s", this.id);
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
