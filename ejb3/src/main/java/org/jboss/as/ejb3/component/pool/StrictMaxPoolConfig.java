/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
