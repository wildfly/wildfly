/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.connections.database;

import static org.jboss.as.domain.management.DomainManagementLogger.ROOT_LOGGER;
import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import javax.sql.DataSource;

import org.jboss.as.domain.management.connections.ConnectionManager;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
/**
 * The Database connection manager maintain the Database connections. It can operate either it
 * "datasource mode" where it utilizes the data source sub system and look up the database driver through
 * JNDI. Or in JDBC connection mode where it connect to a database through JDBC driver interface.
 *
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class DatabaseConnectionManagerService implements Service<DatabaseConnectionManagerService>, ConnectionManager {

    public static final ServiceName BASE_SERVICE_NAME = ServiceName.JBOSS.append("server", "controller", "management", "connection_manager");
    private volatile ModelNode resolvedConfiguration;
    private DatabaseConnectionPool databaseConnectionPool;
    private final InjectedValue<DataSource> dataSource = new InjectedValue<DataSource>();

    public DatabaseConnectionManagerService(final ModelNode resolvedConfiguration) {
        setResolvedConfiguration(resolvedConfiguration);
    }

    void setResolvedConfiguration(final ModelNode resolvedConfiguration) {
        // Validate
        if (resolvedConfiguration.hasDefined(DatabaseConnectionResourceDefinition.DATA_SOURCE.getName())) {
            resolvedConfiguration.require(DatabaseConnectionResourceDefinition.DATA_SOURCE.getName());
        } else {
            resolvedConfiguration.require(DatabaseConnectionResourceDefinition.DATABASE_MODULE.getName());
            resolvedConfiguration.require(DatabaseConnectionResourceDefinition.DATABASE_DRIVE.getName());
            resolvedConfiguration.require(DatabaseConnectionResourceDefinition.DATABASE_URL.getName());
            resolvedConfiguration.require(DatabaseConnectionResourceDefinition.DATABASE_USERNAME.getName());
            resolvedConfiguration.require(DatabaseConnectionResourceDefinition.DATABASE_PASSWORD.getName());
            resolvedConfiguration.require(DatabaseConnectionResourceDefinition.DATABASE_MAX_POOL_SIZE.getName());
            resolvedConfiguration.require(DatabaseConnectionResourceDefinition.DATABASE_MIN_POOL_SIZE.getName());
        }
        // Store
        this.resolvedConfiguration = resolvedConfiguration;
    }

    /*
    *  Service Lifecycle Methods
    */

    public synchronized void start(StartContext context) throws StartException {
        try {
            if (resolvedConfiguration.hasDefined(DatabaseConnectionResourceDefinition.DATA_SOURCE.getName())) {
                databaseConnectionPool = new DatabaseConnectionPool(getDatasource().getValue());
            } else {
                databaseConnectionPool = new DatabaseConnectionPool(resolvedConfiguration.require(DatabaseConnectionResourceDefinition.DATABASE_MODULE.getName()).asString(),
                        resolvedConfiguration.require(DatabaseConnectionResourceDefinition.DATABASE_DRIVE.getName()).asString(),
                        resolvedConfiguration.require(DatabaseConnectionResourceDefinition.DATABASE_URL.getName()).asString(),
                        resolvedConfiguration.require(DatabaseConnectionResourceDefinition.DATABASE_USERNAME.getName()).asString(),
                        resolvedConfiguration.require(DatabaseConnectionResourceDefinition.DATABASE_PASSWORD.getName()).asString(),
                        resolvedConfiguration.require(DatabaseConnectionResourceDefinition.DATABASE_MIN_POOL_SIZE.getName()).asInt(),
                        resolvedConfiguration.require(DatabaseConnectionResourceDefinition.DATABASE_MAX_POOL_SIZE.getName()).asInt());
            }
        } catch (InstantiationException e) {
            throw MESSAGES.jdbcNotLoadedException(e,DatabaseConnectionResourceDefinition.DATABASE_DRIVE.getName());
        } catch (ClassNotFoundException e) {
            throw MESSAGES.jdbcDriverClassNotFoundException(e,DatabaseConnectionResourceDefinition.DATABASE_DRIVE.getName());
        } catch (Exception e) {
            throw MESSAGES.databaseConnectionManagerServiceStartupException(e);
        }
    }

    public synchronized void stop(StopContext context) {
        try {
            databaseConnectionPool.terminateReaper();
            databaseConnectionPool.closeConnections();
        } catch (Exception e) {
            ROOT_LOGGER.databaseConnectionManagerServiceShutdown();
        }
    }

    public synchronized DatabaseConnectionManagerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /*
     *  Connection Manager Methods
     */

    public Object getConnection() throws Exception {
        final ModelNode config = resolvedConfiguration;
        return getConnection(config);
    }

    public Object getConnection(String principal, String credential) throws Exception {
        return getConnection();
    }

    // TODO - Workaround to clear ContextClassLoader to allow access to System ClassLoader
    private Object getConnection(final ModelNode config) throws Exception {
        ClassLoader original = null;
        try {
            original = Thread.currentThread().getContextClassLoader();
            if (original != null) {
                Thread.currentThread().setContextClassLoader(null);
            }
            return databaseConnectionPool.getConnection();
        } finally {
            if (original != null) {
                Thread.currentThread().setContextClassLoader(original);
            }
        }
    }

    public InjectedValue<DataSource> getDatasource() {
        return dataSource;
    }

}
