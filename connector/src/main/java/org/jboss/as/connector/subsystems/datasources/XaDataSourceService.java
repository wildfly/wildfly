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

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.TransactionSupport;

import org.jboss.jca.adapters.jdbc.BaseWrapperManagedConnectionFactory;
import org.jboss.jca.adapters.jdbc.spi.ClassLoaderPlugin;
import org.jboss.jca.adapters.jdbc.xa.XAManagedConnectionFactory;
import org.jboss.jca.common.api.metadata.common.Extension;
import org.jboss.jca.common.api.metadata.ds.DsSecurity;
import org.jboss.jca.common.api.metadata.ds.Statement;
import org.jboss.jca.common.api.metadata.ds.TimeOut;
import org.jboss.jca.common.api.metadata.ds.Validation;
import org.jboss.jca.common.api.metadata.ds.XaDataSource;
import org.jboss.jca.core.api.connectionmanager.pool.PoolConfiguration;
import org.jboss.jca.core.connectionmanager.ConnectionManager;
import org.jboss.jca.core.connectionmanager.ConnectionManagerFactory;
import org.jboss.jca.core.connectionmanager.pool.api.Pool;
import org.jboss.jca.core.connectionmanager.pool.api.PoolFactory;
import org.jboss.jca.core.connectionmanager.pool.api.PoolStrategy;
import org.jboss.msc.service.StartException;

/**
 * XA data-source service implementation.
 * @author John Bailey
 */
public class XaDataSourceService extends AbstractDataSourceService {
    private final XaDataSource dataSourceConfig;

    public XaDataSourceService(final String jndiName, final XaDataSource dataSourceConfig) {
        super(jndiName);
        this.dataSourceConfig = dataSourceConfig;
    }

    protected final BaseWrapperManagedConnectionFactory createManagedConnectionFactory(final String jndiName,
            final Driver driver) throws ResourceException, StartException {
        final XAManagedConnectionFactory xaManagedConnectionFactory = new XAManagedConnectionFactory();

        try {
            xaManagedConnectionFactory.setClassLoaderPlugin(new ClassLoaderPlugin() {

                @Override
                public ClassLoader getClassLoader() {
                    return driver.getClass().getClassLoader();
                }
            });
            xaManagedConnectionFactory.setXADataSourceClass(dataSourceConfig.getXaDataSourceClass());
        } catch (Exception e) {
            throw new StartException("Failed to load XA DataSource class - " + dataSourceConfig.getXaDataSourceClass());
        }

        xaManagedConnectionFactory.setJndiName(jndiName);
        xaManagedConnectionFactory.setSpy(true);

        if (dataSourceConfig.getNewConnectionSql() != null) {
            xaManagedConnectionFactory.setNewConnectionSQL(dataSourceConfig.getNewConnectionSql());
        }
        if (dataSourceConfig.getTransactionIsolation() != null) {
            xaManagedConnectionFactory.setTransactionIsolation(dataSourceConfig.getTransactionIsolation().name());
        }
        if (dataSourceConfig.getUrlDelimiter() != null) {
            xaManagedConnectionFactory.setURLDelimiter(dataSourceConfig.getUrlDelimiter());
        }
        if (dataSourceConfig.getUrlSelectorStrategyClassName() != null) {
            xaManagedConnectionFactory.setUrlSelectorStrategyClassName(dataSourceConfig.getUrlSelectorStrategyClassName());
        }

        final DsSecurity security = dataSourceConfig.getSecurity();
        if (security != null) {
            if (security.getUserName() != null) {
                xaManagedConnectionFactory.setUserName(security.getUserName());
            }
            if (security.getPassword() != null) {
                xaManagedConnectionFactory.setPassword(security.getPassword());
            }
        }

        final TimeOut timeOut = dataSourceConfig.getTimeOut();
        if (timeOut != null) {
            if (timeOut.getUseTryLock() != null) {
                xaManagedConnectionFactory.setUseTryLock(timeOut.getUseTryLock().intValue());
            }
            if (timeOut.getQueryTimeout() != null) {
                xaManagedConnectionFactory.setQueryTimeout(timeOut.getQueryTimeout().intValue());
            }
        }

        final Statement statement = dataSourceConfig.getStatement();
        if (statement != null) {
            if (statement.getTrackStatements() != null) {
                xaManagedConnectionFactory.setTrackStatements(statement.getTrackStatements().name());
            }
            if (statement.isSharePreparedStatements() != null) {
                xaManagedConnectionFactory.setSharePreparedStatements(statement.isSharePreparedStatements());
            }
            if (statement.getPreparedStatementsCacheSize() != null) {
                xaManagedConnectionFactory.setPreparedStatementCacheSize(statement.getPreparedStatementsCacheSize().intValue());
            }
        }

        final Validation validation = dataSourceConfig.getValidation();
        if (validation != null) {
            if (validation.isValidateOnMatch()) {
                xaManagedConnectionFactory.setValidateOnMatch(validation.isValidateOnMatch());
            }
            if (validation.getCheckValidConnectionSql() != null) {
                xaManagedConnectionFactory.setCheckValidConnectionSQL(validation.getCheckValidConnectionSql());
            }
            final Extension validConnectionChecker = validation.getValidConnectionChecker();
            if (validConnectionChecker != null) {
                if (validConnectionChecker.getClassName() != null) {
                    xaManagedConnectionFactory.setValidConnectionCheckerClassName(validConnectionChecker.getClassName());
                }
                if (validConnectionChecker.getConfigPropertiesMap() != null) {
                    xaManagedConnectionFactory
                            .setValidConnectionCheckerProperties(buildConfigPropsString(validConnectionChecker
                                    .getConfigPropertiesMap()));
                }
            }
            final Extension exceptionSorter = validation.getExceptionSorter();
            if (exceptionSorter != null) {
                if (exceptionSorter.getClassName() != null) {
                    xaManagedConnectionFactory.setExceptionSorterClassName(exceptionSorter.getClassName());
                }
                if (exceptionSorter.getConfigPropertiesMap() != null) {
                    xaManagedConnectionFactory.setExceptionSorterProperties(buildConfigPropsString(exceptionSorter
                            .getConfigPropertiesMap()));
                }
            }
            final Extension staleConnectionChecker = validation.getStaleConnectionChecker();
            if (staleConnectionChecker != null) {
                if (staleConnectionChecker.getClassName() != null) {
                    xaManagedConnectionFactory.setStaleConnectionCheckerClassName(staleConnectionChecker.getClassName());
                }
                if (staleConnectionChecker.getConfigPropertiesMap() != null) {
                    xaManagedConnectionFactory
                            .setStaleConnectionCheckerProperties(buildConfigPropsString(staleConnectionChecker
                                    .getConfigPropertiesMap()));
                }
            }
        }
        return xaManagedConnectionFactory;
    }

    protected Pool createPool(final String jndiName, final ManagedConnectionFactory mcf) {
        final PoolConfiguration pc = createPoolConfiguration(dataSourceConfig.getXaPool(), dataSourceConfig.getTimeOut(),
                dataSourceConfig.getValidation());

        Boolean noTxSeparatePool = Boolean.FALSE;

        if (dataSourceConfig.getXaPool() != null && dataSourceConfig.getXaPool().isNoTxSeparatePool() != null) {
            noTxSeparatePool = dataSourceConfig.getXaPool().isNoTxSeparatePool();
        }

        final PoolFactory pf = new PoolFactory();
        final Pool pool = pf.create(PoolStrategy.ONE_POOL, mcf, pc, noTxSeparatePool.booleanValue());

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
        Integer allocationRetry = null;
        Long allocationRetryWaitMillis = null;
        Boolean interleaving = null;
        Integer xaResourceTimeout = null;
        Boolean isSameRMOverride = null;
        Boolean wrapXAResource = null;
        Boolean padXid = null;

        if (dataSourceConfig.getTimeOut() != null) {
            allocationRetry = dataSourceConfig.getTimeOut().getAllocationRetry();
            allocationRetryWaitMillis = dataSourceConfig.getTimeOut().getAllocationRetryWaitMillis();
            xaResourceTimeout = dataSourceConfig.getTimeOut().getXaResourceTimeout();
        }

        if (dataSourceConfig.getXaPool() != null) {
            interleaving = dataSourceConfig.getXaPool().isInterleaving();
            isSameRMOverride = dataSourceConfig.getXaPool().isSameRmOverride();
            wrapXAResource = dataSourceConfig.getXaPool().isWrapXaDataSource();
            padXid = dataSourceConfig.getXaPool().isPadXid();
        }

        // TODO
        String securityDomain = null;
        // Select the correct connection manager
        TransactionSupport.TransactionSupportLevel tsl = TransactionSupport.TransactionSupportLevel.XATransaction;
        ConnectionManagerFactory cmf = new ConnectionManagerFactory();
        ConnectionManager cm = cmf.createTransactional(tsl, pool, null, securityDomain, allocationRetry,
                allocationRetryWaitMillis, getTransactionManager(), interleaving, xaResourceTimeout, isSameRMOverride,
                wrapXAResource, padXid);

        cm.setJndiName(jndiName);

        return cm;
    }
}
