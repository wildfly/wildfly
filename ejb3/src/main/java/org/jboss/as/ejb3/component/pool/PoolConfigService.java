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

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

/**
 * User: jpai
 */
public class PoolConfigService implements Service<PoolConfig> {

    public static final ServiceName EJB_POOL_CONFIG_BASE_SERVICE_NAME = ServiceName.JBOSS.append("ejb").append("pool-config");

    public static final ServiceName DEFAULT_SLSB_POOL_CONFIG_SERVICE_NAME = EJB_POOL_CONFIG_BASE_SERVICE_NAME.append("slsb-default");

    public static final ServiceName DEFAULT_MDB_POOL_CONFIG_SERVICE_NAME = EJB_POOL_CONFIG_BASE_SERVICE_NAME.append("mdb-default");

    public static final ServiceName DEFAULT_ENTITY_POOL_CONFIG_SERVICE_NAME = EJB_POOL_CONFIG_BASE_SERVICE_NAME.append("entity-default");

    private final PoolConfig poolConfig;

    public PoolConfigService(final PoolConfig poolConfig) {
        if (poolConfig == null) {
            throw MESSAGES.poolConfigIsNull();
        }
        this.poolConfig = poolConfig;
    }

    @Override
    public void start(StartContext context) throws StartException {

    }

    @Override
    public void stop(StopContext context) {

    }

    @Override
    public PoolConfig getValue() throws IllegalStateException, IllegalArgumentException {
        return this.poolConfig;
    }
}
