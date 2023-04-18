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

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * User: jpai
 */
public class StrictMaxPoolConfigService implements Service<StrictMaxPoolConfig> {

    private final Consumer<StrictMaxPoolConfig> configConsumer;
    private final Supplier<Integer> maxThreadsSupplier;
    private final StrictMaxPoolConfig poolConfig;


    private volatile int declaredMaxSize;

    public enum Derive {NONE, FROM_WORKER_POOLS, FROM_CPU_COUNT}

    private volatile Derive derive;


    public StrictMaxPoolConfigService(final Consumer<StrictMaxPoolConfig> configConsumer, final Supplier<Integer> maxThreadsSupplier, final String poolName, int declaredMaxSize, Derive derive, long timeout, TimeUnit timeUnit) {
        this.configConsumer = configConsumer;
        this.maxThreadsSupplier = maxThreadsSupplier;
        this.declaredMaxSize = declaredMaxSize;
        this.derive = derive;
        this.poolConfig = new StrictMaxPoolConfig(poolName, declaredMaxSize, timeout, timeUnit);
    }

    @Override
    public void start(final StartContext context) throws StartException {
        setDerive(derive);
        configConsumer.accept(poolConfig);
    }

    @Override
    public void stop(final StopContext context) {
        configConsumer.accept(null);
    }

    @Override
    public StrictMaxPoolConfig getValue() {
        return poolConfig;
    }

    private int calcMaxFromWorkPools() {
        Integer max = maxThreadsSupplier != null ? maxThreadsSupplier.get() : null;
        return max != null && max > 0 ? max : calcMaxFromCPUCount();
    }

    private int calcMaxFromCPUCount() {
        return Runtime.getRuntime().availableProcessors() * 4;
    }

    public synchronized void setMaxPoolSize(int newMax) {
        this.declaredMaxSize = newMax;

        if (derive == Derive.NONE) {
            poolConfig.setMaxPoolSize(newMax);
        }
    }

    public synchronized int getDerivedSize() {
        return poolConfig.getMaxPoolSize();
    }

    public synchronized void setDerive(Derive derive) {
        this.derive = derive;
        int max = this.declaredMaxSize;
        switch (derive) {
            case FROM_WORKER_POOLS: {
                max = calcMaxFromWorkPools();
                EjbLogger.ROOT_LOGGER.strictPoolDerivedFromWorkers(poolConfig.getPoolName(), max);
                break;
            }
            case FROM_CPU_COUNT: {
                max = calcMaxFromCPUCount();
                EjbLogger.ROOT_LOGGER.strictPoolDerivedFromCPUs(poolConfig.getPoolName(), max);
                break;
            }
        }
        poolConfig.setMaxPoolSize(max);
    }

    public void setTimeout(long timeout) {
        poolConfig.setTimeout(timeout);
    }

    public void setTimeoutUnit(TimeUnit timeUnit) {
        poolConfig.setTimeoutUnit(timeUnit);
    }
}
