/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
