/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ee.infinispan;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.transaction.LockingMode;
import org.infinispan.util.concurrent.IsolationLevel;

/**
 * Eagerly calculates the properties of a cache configuration.
 * @author Paul Ferraro
 */
public class InfinispanCacheProperties implements CacheProperties {

    private final boolean lockOnRead;
    private final boolean lockOnWrite;
    private final boolean marshalling;
    private final boolean persistent;
    private final boolean transactional;

    public InfinispanCacheProperties(Configuration config) {
        this.transactional = config.transaction().transactionMode().isTransactional();
        this.lockOnWrite = this.transactional && (config.transaction().lockingMode() == LockingMode.PESSIMISTIC);
        this.lockOnRead = this.lockOnWrite && (config.locking().isolationLevel() == IsolationLevel.REPEATABLE_READ);
        boolean clustered = config.clustering().cacheMode().needsStateTransfer();
        boolean hasStore = config.persistence().usingStores();
        this.marshalling = clustered || hasStore;
        this.persistent = clustered || (hasStore && !config.persistence().passivation());
    }

    @Override
    public boolean isPersistent() {
        return this.persistent;
    }

    @Override
    public boolean isTransactional() {
        return this.transactional;
    }

    @Override
    public boolean isLockOnRead() {
        return this.lockOnRead;
    }

    @Override
    public boolean isLockOnWrite() {
        return this.lockOnWrite;
    }

    @Override
    public boolean isMarshalling() {
        return this.marshalling;
    }
}
