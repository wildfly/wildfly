/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.pool;

import org.jboss.as.ejb3.pool.Pool;
import org.jboss.as.ejb3.pool.StatelessObjectFactory;
import org.jboss.as.ejb3.pool.strictmax.StrictMaxPool;

import java.util.concurrent.TimeUnit;

/**
 * User: Jaikiran Pai
 */
public class StrictMaxPoolConfig extends PoolConfig {

    public static final int DEFAULT_MAX_POOL_SIZE = 20;

    public static final long DEFAULT_TIMEOUT = 5;

    public static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.MINUTES;


    private volatile int maxPoolSize;

    private volatile TimeUnit timeoutUnit;

    private volatile long timeout;

    public StrictMaxPoolConfig(final String poolName, int maxSize, long timeout, TimeUnit timeUnit) {
        super(poolName);
        this.maxPoolSize = maxSize;
        this.timeout = timeout;
        this.timeoutUnit = timeUnit;
    }

    @Override
    public <T> Pool<T> createPool(final StatelessObjectFactory<T> statelessObjectFactory) {
        return new StrictMaxPool<T>(statelessObjectFactory, this.maxPoolSize, this.timeout, this.timeoutUnit);
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public TimeUnit getTimeoutUnit() {
        return timeoutUnit;
    }

    public void setTimeoutUnit(TimeUnit timeoutUnit) {
        this.timeoutUnit = timeoutUnit;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public String toString() {
        return "StrictMaxPoolConfig{" +
                "name=" + this.poolName +
                ", maxPoolSize=" + maxPoolSize +
                ", timeoutUnit=" + timeoutUnit +
                ", timeout=" + timeout +
                '}';
    }
}
