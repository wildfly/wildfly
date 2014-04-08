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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jboss.as.clustering.concurrent.Scheduler;
import org.jboss.as.clustering.infinispan.invoker.Evictor;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.web.Batcher;
import org.wildfly.clustering.web.session.ImmutableSession;

/**
 * Session eviction scheduler that eagerly evicts the oldest sessions when
 * the number of active sessions exceeds the configured maximum.
 * @author Paul Ferraro
 */
public class SessionEvictionScheduler implements Scheduler<ImmutableSession>, SessionEvictionContext {

    private final Set<String> evictionQueue = new LinkedHashSet<>();
    private final Batcher batcher;
    private final Evictor<String> evictor;
    private final CommandDispatcher<SessionEvictionContext> dispatcher;
    private final int maxSize;

    public SessionEvictionScheduler(String cacheName, Batcher batcher, Evictor<String> evictor, CommandDispatcherFactory dispatcherFactory, int maxSize) {
        this.batcher = batcher;
        this.evictor = evictor;
        this.dispatcher = dispatcherFactory.<SessionEvictionContext>createCommandDispatcher(cacheName, this);
        this.maxSize = maxSize;
    }

    @Override
    public Batcher getBatcher() {
        return this.batcher;
    }

    @Override
    public Evictor<String> getEvictor() {
        return this.evictor;
    }

    @Override
    public void cancel(ImmutableSession session) {
        synchronized (this.evictionQueue) {
            this.evictionQueue.remove(session.getId());
        }
    }

    @Override
    public void schedule(ImmutableSession session) {
        synchronized (this.evictionQueue) {
            this.evictionQueue.add(session.getId());
            // Trigger eviction of oldest session if necessary
            if (this.evictionQueue.size() > this.maxSize) {
                Iterator<String> sessions = this.evictionQueue.iterator();
                this.dispatcher.submitOnCluster(new SessionEvictionCommand(sessions.next()));
                sessions.remove();
            }
        }
    }

    @Override
    public void close() {
        synchronized (this.evictionQueue) {
            this.evictionQueue.clear();
        }
        this.dispatcher.close();
    }
}
