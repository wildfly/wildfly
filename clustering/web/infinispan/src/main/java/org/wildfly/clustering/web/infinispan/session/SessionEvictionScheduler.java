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
package org.wildfly.clustering.web.infinispan.session;

import java.security.AccessController;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.jboss.threads.JBossThreadFactory;
import org.wildfly.clustering.web.Batcher;
import org.wildfly.clustering.web.infinispan.Evictor;
import org.wildfly.clustering.web.infinispan.Scheduler;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.security.manager.GetAccessControlContextAction;

/**
 * Session eviction scheduler that eagerly evicts the oldest sessions when
 * the number of active sessions exceeds the configured maximum.
 * @author Paul Ferraro
 */
public class SessionEvictionScheduler<L> implements Scheduler<Session<L>> {

    final Queue<String> evictionQueue = new ConcurrentLinkedQueue<>();
    final Batcher batcher;
    final Evictor<String> evictor;
    private final ExecutorService executor;
    private final int maxSize;

    public SessionEvictionScheduler(Batcher batcher, Evictor<String> evictor, int maxSize) {
        this(batcher, evictor, maxSize, Executors.newCachedThreadPool(createThreadFactory()));
    }

    private static ThreadFactory createThreadFactory() {
        return new JBossThreadFactory(new ThreadGroup(SessionEvictionScheduler.class.getSimpleName()), Boolean.FALSE, null, "%G - %t", null, null, AccessController.doPrivileged(GetAccessControlContextAction.getInstance()));
    }

    public SessionEvictionScheduler(Batcher batcher, Evictor<String> evictor, int maxSize, ExecutorService executor) {
        this.batcher = batcher;
        this.evictor = evictor;
        this.maxSize = maxSize;
        this.executor = executor;
    }

    @Override
    public void cancel(Session<L> session) {
        this.evictionQueue.remove(session.getId());
    }

    @Override
    public void schedule(Session<L> session) {
        this.evictionQueue.add(session.getId());
        // Trigger eviction of oldest sessions if necessary
        while (this.evictionQueue.size() > this.maxSize) {
            String id = this.evictionQueue.poll();
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
        private final String id;

        EvictionTask(String id) {
            this.id = id;
        }

        @Override
        public void run() {
            boolean started = SessionEvictionScheduler.this.batcher.startBatch();
            boolean successful = false;
            try {
                SessionEvictionScheduler.this.evictor.evict(this.id);
                successful = true;
            } finally {
                if (started) {
                    SessionEvictionScheduler.this.batcher.endBatch(successful);
                }
            }
        }
    }
}
