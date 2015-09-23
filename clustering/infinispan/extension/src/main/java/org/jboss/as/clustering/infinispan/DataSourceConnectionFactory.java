/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.infinispan.persistence.jdbc.configuration.ConnectionFactoryConfiguration;
import org.infinispan.persistence.jdbc.connectionfactory.ConnectionFactory;
import org.infinispan.persistence.spi.PersistenceException;

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
