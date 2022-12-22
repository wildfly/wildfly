/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ee.infinispan.expiration;

import java.time.Instant;

import org.wildfly.clustering.ee.Scheduler;
import org.wildfly.clustering.ee.expiration.ExpirationMetaData;
import org.wildfly.clustering.ee.infinispan.scheduler.AbstractCacheEntryScheduler;

/**
 * An {@link AbstractCacheEntryScheduler} suitable for expiration.
 * @author Paul Ferraro
 * @param <I> the identifier type of the scheduled object
 */
public abstract class AbstractExpirationScheduler<I> extends AbstractCacheEntryScheduler<I, ExpirationMetaData> {

    public AbstractExpirationScheduler(Scheduler<I, Instant> scheduler) {
        super(scheduler, metaData -> !metaData.isImmortal() ? metaData.getLastAccessTime().plus(metaData.getTimeout()) : null);
    }
}
