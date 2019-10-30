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

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * User: jpai
 */
public class StrictMaxPoolConfigService implements Service<StrictMaxPoolConfig> {

    public static final ServiceName EJB_POOL_CONFIG_BASE_SERVICE_NAME = ServiceName.JBOSS.append("ejb").append("pool-config");

    public static final ServiceName DEFAULT_SLSB_POOL_CONFIG_SERVICE_NAME = EJB_POOL_CONFIG_BASE_SERVICE_NAME.append("slsb-default");

    public static final ServiceName DEFAULT_MDB_POOL_CONFIG_SERVICE_NAME = EJB_POOL_CONFIG_BASE_SERVICE_NAME.append("mdb-default");

    public static final ServiceName DEFAULT_ENTITY_POOL_CONFIG_SERVICE_NAME = EJB_POOL_CONFIG_BASE_SERVICE_NAME.append("entity-default");

    private final StrictMaxPoolConfig poolConfig;

    private final InjectedValue<Integer> maxThreadsInjector = new InjectedValue<>();

    private volatile int declaredMaxSize;

    public enum Derive {NONE, FROM_WORKER_POOLS, FROM_CPU_COUNT}

    private volatile Derive derive;


    public StrictMaxPoolConfigService(final String poolName, int declaredMaxSize, Derive derive, long timeout, TimeUnit timeUnit) {
        this.declaredMaxSize = declaredMaxSize;
        this.derive = derive;
        this.poolConfig = new StrictMaxPoolConfig(poolName, declaredMaxSize, timeout, timeUnit);
    }

    @Override
    public void start(StartContext context) throws StartException {
        setDerive(derive);
    }

    private int calcMaxFromWorkPools() {
        Integer max = maxThreadsInjector.getOptionalValue();
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

    @Override
    public void stop(StopContext context) {

    }

    @Override
    public StrictMaxPoolConfig getValue() throws IllegalStateException, IllegalArgumentException {
        return this.poolConfig;
    }

    public Injector<Integer> getMaxThreadsInjector() {
        return maxThreadsInjector;
    }
}
