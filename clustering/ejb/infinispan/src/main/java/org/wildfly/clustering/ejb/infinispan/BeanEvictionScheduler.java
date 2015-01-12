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

import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.infinispan.Evictor;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.infinispan.spi.distribution.Locality;

/**
 * Schedules a bean for eviction.
 *
 * @author Paul Ferraro
 *
 * @param <G> the group identifier type
 * @param <I> the bean identifier type
 * @param <T> the bean type
 */
public class BeanEvictionScheduler<I> implements Scheduler<I>, BeanEvictionContext<I> {

    private final Set<I> evictionQueue = new LinkedHashSet<>();
    private final Batcher<TransactionBatch> batcher;
    private final Evictor<I> evictor;
    private final CommandDispatcher<BeanEvictionContext<I>> dispatcher;
    private final PassivationConfiguration<?> config;

    public BeanEvictionScheduler(String name, Batcher<TransactionBatch> batcher, Evictor<I> evictor, CommandDispatcherFactory dispatcherFactory, PassivationConfiguration<?> config) {
        this.batcher = batcher;
        this.evictor = evictor;
        this.config = config;
        this.dispatcher = dispatcherFactory.<BeanEvictionContext<I>>createCommandDispatcher(name, this);
    }

    @Override
    public Batcher<TransactionBatch> getBatcher() {
        return this.batcher;
    }

    @Override
    public Evictor<I> getEvictor() {
        return this.evictor;
    }

    @Override
    public void cancel(I id) {
        synchronized (this.evictionQueue) {
            this.evictionQueue.remove(id);
        }
    }

    @Override
    public void cancel(Locality locality) {
        synchronized (this.evictionQueue) {
            Iterator<I> beans = this.evictionQueue.iterator();
            while (beans.hasNext()) {
                I id = beans.next();
                if (!locality.isLocal(id)) {
                    beans.remove();
                }
            }
        }
    }

    @Override
    public void schedule(I id) {
        synchronized (this.evictionQueue) {
            this.evictionQueue.add(id);
            // Trigger eviction of oldest bean if necessary
            if (this.evictionQueue.size() > this.config.getConfiguration().getMaxSize()) {
                Iterator<I> beans = this.evictionQueue.iterator();
                I bean = beans.next();
                try {
                    this.dispatcher.submitOnCluster(new BeanEvictionCommand<>(bean));
                    beans.remove();
                } catch (Exception e) {
                    InfinispanEjbLogger.ROOT_LOGGER.failedToPassivateBean(e, bean);
                }
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
