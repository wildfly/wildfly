/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.datasources.agroal.logging;

import io.agroal.api.AgroalDataSourceListener;

import java.sql.Connection;

/**
 * Provides log for important DataSource actions
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class LoggingDataSourceListener implements AgroalDataSourceListener {

    private final String datasourceName;

    public LoggingDataSourceListener(String name) {
        this.datasourceName = name;
    }

    @Override
    public void beforeConnectionLeak(Connection connection) {
        AgroalLogger.POOL_LOGGER.debugv("{0}: Leak test on connection {1}", datasourceName, connection);
    }

    @Override
    public void beforeConnectionReap(Connection connection) {
        AgroalLogger.POOL_LOGGER.debugv("{0}: Reap test on connection {1}", datasourceName, connection);
    }

    @Override
    public void beforeConnectionValidation(Connection connection) {
        AgroalLogger.POOL_LOGGER.debugv("{0}: Validation test on connection {1}", datasourceName, connection);
    }

    @Override
    public void onConnectionAcquire(Connection connection) {
        AgroalLogger.POOL_LOGGER.debugv("{0}: Acquire connection {1}", datasourceName, connection);
    }

    @Override
    public void onConnectionCreation(Connection connection) {
        AgroalLogger.POOL_LOGGER.debugv("{0}: Created connection {1}", datasourceName, connection);
    }

    @Override
    public void onConnectionReap(Connection connection) {
        AgroalLogger.POOL_LOGGER.debugv("{0}: Closing idle connection {1}", datasourceName, connection);
    }

    @Override
    public void onConnectionReturn(Connection connection) {
        AgroalLogger.POOL_LOGGER.debugv("{0}: Returning connection {1}", datasourceName, connection);
    }

    @Override
    public void onConnectionDestroy(Connection connection) {
        AgroalLogger.POOL_LOGGER.debugv("{0}: Destroyed connection {1}", datasourceName, connection);
    }

    @Override
    public void onWarning(String warning) {
        AgroalLogger.POOL_LOGGER.poolWarning(datasourceName, warning);
    }

    @Override
    public void onWarning(Throwable throwable) {
        AgroalLogger.POOL_LOGGER.poolWarning(datasourceName, throwable.getMessage());
        AgroalLogger.POOL_LOGGER.debug("Cause: ", throwable);
    }
}
