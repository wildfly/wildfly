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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.jboss.threads.JBossThreadFactory;
import org.wildfly.clustering.web.Batcher;
import org.wildfly.clustering.web.infinispan.InfinispanWebLogger;
import org.wildfly.clustering.web.infinispan.Remover;
import org.wildfly.clustering.web.infinispan.Scheduler;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.security.manager.GetAccessControlContextAction;

/**
 * Session expiration scheduler that eagerly expires sessions as soon as they are eligible.
 * If/When Infinispan implements expiration notifications (ISPN-694), this will be obsolete.
 * @author Paul Ferraro
 */
public class SessionExpirationScheduler<L> implements Scheduler<Session<L>> {

    final Map<String, Future<?>> expirationFutures = new ConcurrentHashMap<>();
    final Batcher batcher;
    final Remover<String> remover;
    private final ScheduledExecutorService executor;

    public SessionExpirationScheduler(Batcher batcher, Remover<String> remover) {
        this(batcher, remover, Executors.newSingleThreadScheduledExecutor(createThreadFactory()));
    }

    private static ThreadFactory createThreadFactory() {
        return new JBossThreadFactory(new ThreadGroup(SessionExpirationScheduler.class.getSimpleName()), Boolean.FALSE, null, "%G - %t", null, null, AccessController.doPrivileged(GetAccessControlContextAction.getInstance()));
    }

    public SessionExpirationScheduler(Batcher batcher, Remover<String> remover, ScheduledExecutorService executor) {
        this.batcher = batcher;
        this.remover = remover;
        this.executor = executor;
    }

    @Override
    public void cancel(Session<L> session) {
        Future<?> future = this.expirationFutures.remove(session.getId());
        if (future != null) {
            future.cancel(false);
        }
    }

    @Override
    public void schedule(Session<L> session) {
        long timeout = session.getMetaData().getMaxInactiveInterval(TimeUnit.MILLISECONDS);
        if (timeout > 0) {
            String id = session.getId();
            InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s will expire in %d ms", id, timeout);
            this.expirationFutures.put(id, this.executor.schedule(new ExpirationTask(id), timeout, TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public void close() {
        this.executor.shutdown();
    }

    private class ExpirationTask implements Runnable {
        private final String id;

        public ExpirationTask(String id) {
            this.id = id;
        }

        @Override
        public void run() {
            SessionExpirationScheduler.this.expirationFutures.remove(this.id);
            InfinispanWebLogger.ROOT_LOGGER.tracef("Expiring session %s", this.id);
            boolean started = SessionExpirationScheduler.this.batcher.startBatch();
            boolean successful = false;
            try {
                SessionExpirationScheduler.this.remover.remove(this.id);
                successful = true;
            } finally {
                if (started) {
                    SessionExpirationScheduler.this.batcher.endBatch(successful);
                }
            }
        }
    }
}
