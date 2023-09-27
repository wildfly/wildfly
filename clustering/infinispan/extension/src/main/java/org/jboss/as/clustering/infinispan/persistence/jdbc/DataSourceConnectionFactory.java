/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.persistence.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.infinispan.persistence.jdbc.common.configuration.ConnectionFactoryConfiguration;
import org.infinispan.persistence.jdbc.common.connectionfactory.ConnectionFactory;
import org.infinispan.persistence.spi.PersistenceException;
import org.jboss.as.clustering.infinispan.logging.InfinispanLogger;

/**
 * A connection factory using an injected {@link DataSource}.
 * @author Paul Ferraro
 */
public class DataSourceConnectionFactory extends ConnectionFactory {

    private volatile DataSource factory;

    @Override
    public void start(ConnectionFactoryConfiguration configuration, ClassLoader classLoader) throws PersistenceException {
        this.factory = ((DataSourceConnectionFactoryConfiguration) configuration).getDataSource();
    }

    @Override
    public void stop() {
        this.factory = null;
    }

    @Override
    public Connection getConnection() throws PersistenceException {
        try {
            return this.factory.getConnection();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void releaseConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                InfinispanLogger.ROOT_LOGGER.debug(e.getMessage(), e);
            }
        }
    }
}
