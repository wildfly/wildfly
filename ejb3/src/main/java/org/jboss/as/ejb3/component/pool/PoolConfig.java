/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.pool;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.pool.Pool;
import org.jboss.as.ejb3.pool.StatelessObjectFactory;

/**
 * User: jpai
 */
public abstract class PoolConfig {

    protected final String poolName;

    public PoolConfig(final String poolName) {
        if (poolName == null || poolName.trim().isEmpty()) {
            throw EjbLogger.ROOT_LOGGER.poolConfigIsEmpty();
        }
        this.poolName = poolName;
    }

    public String getPoolName() {
        return this.poolName;
    }

    public abstract <T> Pool<T> createPool(final StatelessObjectFactory<T> statelessObjectFactory);
}
