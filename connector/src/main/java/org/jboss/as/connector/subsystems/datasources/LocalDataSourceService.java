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

package org.jboss.as.connector.subsystems.datasources;

import java.sql.Driver;

import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.TransactionSupport;

import org.jboss.jca.adapters.jdbc.BaseWrapperManagedConnectionFactory;
import org.jboss.jca.adapters.jdbc.local.LocalManagedConnectionFactory;
import org.jboss.jca.adapters.jdbc.spi.ClassLoaderPlugin;
import org.jboss.jca.common.api.metadata.common.Extension;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.metadata.ds.DsSecurity;
import org.jboss.jca.common.api.metadata.ds.Statement;
import org.jboss.jca.common.api.metadata.ds.TimeOut;
import org.jboss.jca.common.api.metadata.ds.Validation;
import org.jboss.jca.core.api.connectionmanager.pool.PoolConfiguration;
import org.jboss.jca.core.connectionmanager.ConnectionManager;
import org.jboss.jca.core.connectionmanager.ConnectionManagerFactory;
import org.jboss.jca.core.connectionmanager.pool.api.Pool;
import org.jboss.jca.core.connectionmanager.pool.api.PoolFactory;
import org.jboss.jca.core.connectionmanager.pool.api.PoolStrategy;

/**
 * Local data-source service implementation.
 * @author John Bailey
 */
public class LocalDataSourceService extends AbstractDataSourceService {

    private final DataSource dataSourceConfig;

    public LocalDataSourceService(final String jndiName, final DataSource dataSourceConfig) {
        super(jndiName);
        this.dataSourceConfig = dataSourceConfig;
    }

    protected final BaseWrapperManagedConnectionFactory createManagedConnectionFactory(final String jndiName,
            final Driver driver) {
        final LocalManagedConnectionFactory managedConnectionFactory = new LocalManagedConnectionFactory();
        managedConnectionFactory.setClassLoaderPlugin(new ClassLoaderPlugin() {

            @Override
            public ClassLoader getClassLoader() {
                return driver.getClass().getClassLoader();
            }
        });
        managedConnectionFactory.setUserTransactionJndiName("java:comp/UserTransaction");
        managedConnectionFactory.setDriverClass(dataSourceConfig.getDriverClass());
        managedConnectionFactory.setJndiName(jndiName);
        managedConnectionFactory.setSpy(true);

        if (dataSourceConfig.getConnectionProperties() != null) {
            managedConnectionFactory
                    .setConnectionProperties(buildConfigPropsString(dataSourceConfig.getConnectionProperties()));
        }
        if (dataSourceConfig.getConnectionUrl() != null) {
            managedConnectionFactory.setConnectionURL(dataSourceConfig.getConnectionUrl());
        }
        if (dataSourceConfig.getNewConnectionSql() != null) {
            managedConnectionFactory.setNewConnectionSQL(dataSourceConfig.getNewConnectionSql());
        }
        if (dataSourceConfig.getTransactionIsolation() != null) {
            managedConnectionFactory.setTransactionIsolation(dataSourceConfig.getTransactionIsolation().name());
        }
        if (dataSourceConfig.getUrlDelimiter() != null) {
            managedConnectionFactory.setURLDelimiter(dataSourceConfig.getUrlDelimiter());
        }
        if (dataSourceConfig.getUrlSelectorStrategyClassName() != null) {
            managedConnectionFactory.setUrlSelectorStrategyClassName(dataSourceConfig.getUrlSelectorStrategyClassName());
        }

        final DsSecurity security = dataSourceConfig.getSecurity();
        if (security != null) {
            if (security.getUserName() != null) {
                managedConnectionFactory.setUserName(security.getUserName());
            }
            if (security.getPassword() != null) {
                managedConnectionFactory.setPassword(security.getPassword());
            }
        }

        final TimeOut timeOut = dataSourceConfig.getTimeOut();
        if (timeOut != null) {
            if (timeOut.getUseTryLock() != null) {
                managedConnectionFactory.setUseTryLock(timeOut.getUseTryLock().intValue());
            }
            if (timeOut.getQueryTimeout() != null) {
                managedConnectionFactory.setQueryTimeout(timeOut.getQueryTimeout().intValue());
            }
        }

        final Statement statement = dataSourceConfig.getStatement();
        if (statement != null) {
            if (statement.getTrackStatements() != null) {
                managedConnectionFactory.setTrackStatements(statement.getTrackStatements().name());
            }
            if (statement.isSharePreparedStatements() != null) {
                managedConnectionFactory.setSharePreparedStatements(statement.isSharePreparedStatements());
            }
            if (statement.getPreparedStatementsCacheSize() != null) {
                managedConnectionFactory.setPreparedStatementCacheSize(statement.getPreparedStatementsCacheSize().intValue());
            }
        }

        final Validation validation = dataSourceConfig.getValidation();
        if (validation != null) {
            if (validation.isValidateOnMatch()) {
                managedConnectionFactory.setValidateOnMatch(validation.isValidateOnMatch());
            }
            if (validation.getCheckValidConnectionSql() != null) {
                managedConnectionFactory.setCheckValidConnectionSQL(validation.getCheckValidConnectionSql());
            }
            final Extension validConnectionChecker = validation.getValidConnectionChecker();
            if (validConnectionChecker != null) {
                if (validConnectionChecker.getClassName() != null) {
                    managedConnectionFactory.setValidConnectionCheckerClassName(validConnectionChecker.getClassName());
                }
                if (validConnectionChecker.getConfigPropertiesMap() != null) {
                    managedConnectionFactory.setValidConnectionCheckerProperties(buildConfigPropsString(validConnectionChecker
                            .getConfigPropertiesMap()));
                }
            }
            final Extension exceptionSorter = validation.getExceptionSorter();
            if (exceptionSorter != null) {
                if (exceptionSorter.getClassName() != null) {
                    managedConnectionFactory.setExceptionSorterClassName(exceptionSorter.getClassName());
                }
                if (exceptionSorter.getConfigPropertiesMap() != null) {
                    managedConnectionFactory.setExceptionSorterProperties(buildConfigPropsString(exceptionSorter
                            .getConfigPropertiesMap()));
                }
            }
            final Extension staleConnectionChecker = validation.getStaleConnectionChecker();
            if (staleConnectionChecker != null) {
                if (staleConnectionChecker.getClassName() != null) {
                    managedConnectionFactory.setStaleConnectionCheckerClassName(staleConnectionChecker.getClassName());
                }
                if (staleConnectionChecker.getConfigPropertiesMap() != null) {
                    managedConnectionFactory.setStaleConnectionCheckerProperties(buildConfigPropsString(staleConnectionChecker
                            .getConfigPropertiesMap()));
                }
            }
        }

        return managedConnectionFactory;
    }

    protected Pool createPool(final String jndiName, final ManagedConnectionFactory mcf) {
        final PoolConfiguration pc = createPoolConfiguration(dataSourceConfig.getPool(), dataSourceConfig.getTimeOut(),
                dataSourceConfig.getValidation());
        PoolFactory pf = new PoolFactory();
        final Pool pool = pf.create(PoolStrategy.ONE_POOL, mcf, pc, false);

        String poolName = null;
        if (dataSourceConfig.getPoolName() != null) {
            poolName = dataSourceConfig.getPoolName();
        }

        if (poolName == null) {
            poolName = jndiName;
        }
        pool.setName(poolName);
        return pool;
    }

    protected ConnectionManager createConnectionManager(final String jndiName, final Pool pool) {
        // Connection manager properties
        Integer allocationRetry = null;
        Long allocationRetryWaitMillis = null;

        if (dataSourceConfig.getTimeOut() != null) {
            allocationRetry = dataSourceConfig.getTimeOut().getAllocationRetry();
            allocationRetryWaitMillis = dataSourceConfig.getTimeOut().getAllocationRetryWaitMillis();
        }

        // Select the correct connection manager
        final TransactionSupport.TransactionSupportLevel tsl = TransactionSupport.TransactionSupportLevel.LocalTransaction;
        final ConnectionManagerFactory cmf = new ConnectionManagerFactory();
        final ConnectionManager cm = cmf.createTransactional(tsl, pool, null, null, allocationRetry, allocationRetryWaitMillis,
                getTransactionIntegration(), null, null, null, null, null);

        cm.setJndiName(jndiName);
        return cm;
    }

}
