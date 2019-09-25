/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

import java.util.List;

import org.wildfly.clustering.ee.infinispan.scheduler.Scheduler;
import org.wildfly.clustering.infinispan.spi.distribution.Locality;

/**
 * Scheduler that delegates to a list of schedulers.
 * @author Paul Ferraro
 */
public class CompositeScheduler<I> implements Scheduler<I, ImmutableBeanEntry<I>> {

    private final List<Scheduler<I, ImmutableBeanEntry<I>>> schedulers;

    public CompositeScheduler(List<Scheduler<I, ImmutableBeanEntry<I>>> schedulers) {
        this.schedulers = schedulers;
    }

    @Override
    public void schedule(I id) {
        for (Scheduler<I, ImmutableBeanEntry<I>> scheduler : this.schedulers) {
            scheduler.schedule(id);
        }
    }

    @Override
    public void schedule(I id, ImmutableBeanEntry<I> entry) {
        for (Scheduler<I, ImmutableBeanEntry<I>> scheduler : this.schedulers) {
            scheduler.schedule(id, entry);
        }
    }

    @Override
    public void cancel(I id) {
        for (Scheduler<I, ImmutableBeanEntry<I>> scheduler : this.schedulers) {
            scheduler.cancel(id);
        }
    }

    @Override
    public void cancel(Locality locality) {
        for (Scheduler<I, ImmutableBeanEntry<I>> scheduler : this.schedulers) {
            scheduler.cancel(locality);
        }
    }

    @Override
    public void close() {
        for (Scheduler<I, ImmutableBeanEntry<I>> scheduler : this.schedulers) {
            scheduler.close();
        }
    }
}
