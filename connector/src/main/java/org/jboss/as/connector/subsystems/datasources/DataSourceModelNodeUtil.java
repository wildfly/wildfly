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

import static org.jboss.as.connector.pool.Constants.BACKGROUNDVALIDATION;
import static org.jboss.as.connector.pool.Constants.BACKGROUNDVALIDATIONMINUTES;
import static org.jboss.as.connector.pool.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS;
import static org.jboss.as.connector.pool.Constants.IDLETIMEOUTMINUTES;
import static org.jboss.as.connector.pool.Constants.MAX_POOL_SIZE;
import static org.jboss.as.connector.pool.Constants.MIN_POOL_SIZE;
import static org.jboss.as.connector.pool.Constants.POOL_PREFILL;
import static org.jboss.as.connector.pool.Constants.POOL_USE_STRICT_MIN;
import static org.jboss.as.connector.pool.Constants.USE_FAST_FAIL;
import static org.jboss.as.connector.subsystems.datasources.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.datasources.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.datasources.Constants.CHECKVALIDCONNECTIONSQL;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_URL;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_DRIVER;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_DRIVER_CLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.EXCEPTIONSORTERCLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.EXCEPTIONSORTER_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.FLUSH_STRATEGY;
import static org.jboss.as.connector.subsystems.datasources.Constants.INTERLIVING;
import static org.jboss.as.connector.subsystems.datasources.Constants.JNDINAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.JTA;
import static org.jboss.as.connector.subsystems.datasources.Constants.NEW_CONNECTION_SQL;
import static org.jboss.as.connector.subsystems.datasources.Constants.NOTXSEPARATEPOOL;
import static org.jboss.as.connector.subsystems.datasources.Constants.NO_RECOVERY;
import static org.jboss.as.connector.subsystems.datasources.Constants.PAD_XID;
import static org.jboss.as.connector.subsystems.datasources.Constants.PASSWORD;
import static org.jboss.as.connector.subsystems.datasources.Constants.POOLNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.PREPAREDSTATEMENTSCACHESIZE;
import static org.jboss.as.connector.subsystems.datasources.Constants.QUERYTIMEOUT;
import static org.jboss.as.connector.subsystems.datasources.Constants.REAUTHPLUGIN_CLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.REAUTHPLUGIN_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERLUGIN_CLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERLUGIN_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_PASSWORD;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_USERNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.SAME_RM_OVERRIDE;
import static org.jboss.as.connector.subsystems.datasources.Constants.SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.datasources.Constants.SETTXQUERYTIMEOUT;
import static org.jboss.as.connector.subsystems.datasources.Constants.SHAREPREPAREDSTATEMENTS;
import static org.jboss.as.connector.subsystems.datasources.Constants.SPY;
import static org.jboss.as.connector.subsystems.datasources.Constants.STALECONNECTIONCHECKERCLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.STALECONNECTIONCHECKER_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.TRACKSTATEMENTS;
import static org.jboss.as.connector.subsystems.datasources.Constants.TRANSACTION_ISOLOATION;
import static org.jboss.as.connector.subsystems.datasources.Constants.URL_DELIMITER;
import static org.jboss.as.connector.subsystems.datasources.Constants.URL_SELECTOR_STRATEGY_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.USERNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.USETRYLOCK;
import static org.jboss.as.connector.subsystems.datasources.Constants.USE_CCM;
import static org.jboss.as.connector.subsystems.datasources.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.connector.subsystems.datasources.Constants.VALIDATEONMATCH;
import static org.jboss.as.connector.subsystems.datasources.Constants.VALIDCONNECTIONCHECKERCLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.VALIDCONNECTIONCHECKER_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.WRAP_XA_DATASOURCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.XADATASOURCECLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.XADATASOURCEPROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_RESOURCE_TIMEOUT;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.jca.common.api.metadata.common.CommonPool;
import org.jboss.jca.common.api.metadata.common.CommonXaPool;
import org.jboss.jca.common.api.metadata.common.Credential;
import org.jboss.jca.common.api.metadata.common.Extension;
import org.jboss.jca.common.api.metadata.common.FlushStrategy;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.metadata.ds.DsSecurity;
import org.jboss.jca.common.api.metadata.ds.Statement;
import org.jboss.jca.common.api.metadata.ds.TimeOut;
import org.jboss.jca.common.api.metadata.ds.TransactionIsolation;
import org.jboss.jca.common.api.metadata.ds.Validation;
import org.jboss.jca.common.api.metadata.ds.XaDataSource;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.jca.common.metadata.common.CommonPoolImpl;
import org.jboss.jca.common.metadata.common.CommonXaPoolImpl;
import org.jboss.jca.common.metadata.common.CredentialImpl;
import org.jboss.jca.common.metadata.ds.DataSourceImpl;
import org.jboss.jca.common.metadata.ds.DsSecurityImpl;
import org.jboss.jca.common.metadata.ds.StatementImpl;
import org.jboss.jca.common.metadata.ds.TimeOutImpl;
import org.jboss.jca.common.metadata.ds.ValidationImpl;
import org.jboss.jca.common.metadata.ds.XADataSourceImpl;

/**
 * Utility used to help convert between JCA spi data-source instances and model
 * node representations and vice-versa.
 * @author John Bailey
 */
class DataSourceModelNodeUtil {

    static void fillFrom(final ModelNode dataSourceModel, final DataSource dataSource) {
        for (Map.Entry<String, String> entry : dataSource.getConnectionProperties().entrySet()) {
            dataSourceModel.get(CONNECTION_PROPERTIES, entry.getKey()).set(entry.getValue());
        }
        setStringIfNotNull(dataSourceModel, CONNECTION_URL, dataSource.getConnectionUrl());
        setStringIfNotNull(dataSourceModel, DATASOURCE_DRIVER_CLASS, dataSource.getDriverClass());
        setStringIfNotNull(dataSourceModel, JNDINAME, dataSource.getJndiName());
        setStringIfNotNull(dataSourceModel, DATASOURCE_DRIVER, dataSource.getDriver());
        setStringIfNotNull(dataSourceModel, NEW_CONNECTION_SQL, dataSource.getNewConnectionSql());
        setStringIfNotNull(dataSourceModel, POOLNAME, dataSource.getPoolName());
        setStringIfNotNull(dataSourceModel, URL_DELIMITER, dataSource.getUrlDelimiter());
        setStringIfNotNull(dataSourceModel, URL_SELECTOR_STRATEGY_CLASS_NAME, dataSource.getUrlSelectorStrategyClassName());
        setBooleanIfNotNull(dataSourceModel, USE_JAVA_CONTEXT, dataSource.isUseJavaContext());
        setBooleanIfNotNull(dataSourceModel, ENABLED, dataSource.isEnabled());
        setBooleanIfNotNull(dataSourceModel, JTA, dataSource.isJTA());

        CommonPool pool = dataSource.getPool();
        if (pool != null) {
            setIntegerIfNotNull(dataSourceModel, MAX_POOL_SIZE, pool.getMaxPoolSize());
            setIntegerIfNotNull(dataSourceModel, MIN_POOL_SIZE, pool.getMinPoolSize());
            setBooleanIfNotNull(dataSourceModel, POOL_PREFILL, pool.isPrefill());
            setBooleanIfNotNull(dataSourceModel, POOL_USE_STRICT_MIN, pool.isUseStrictMin());
            if (pool.getFlushStrategy() != null) {
                setStringIfNotNull(dataSourceModel, FLUSH_STRATEGY, pool.getFlushStrategy().getName());
            }
        }
        DsSecurity security = dataSource.getSecurity();
        if (security != null) {
            setStringIfNotNull(dataSourceModel, USERNAME, security.getUserName());
            setStringIfNotNull(dataSourceModel, PASSWORD, security.getPassword());
            setStringIfNotNull(dataSourceModel, SECURITY_DOMAIN, security.getSecurityDomain());
            setExtensionIfNotNull(dataSourceModel, REAUTHPLUGIN_CLASSNAME, REAUTHPLUGIN_PROPERTIES, security.getReauthPlugin());
        }
        Statement statement = dataSource.getStatement();
        if (statement != null) {
            setLongIfNotNull(dataSourceModel, PREPAREDSTATEMENTSCACHESIZE, statement.getPreparedStatementsCacheSize());
            setBooleanIfTrue(dataSourceModel, SHAREPREPAREDSTATEMENTS, statement.isSharePreparedStatements());
            if (statement.getTrackStatements() != null) {
                setStringIfNotNull(dataSourceModel, TRACKSTATEMENTS, statement.getTrackStatements().name());
            }
        }
        TimeOut timeout = dataSource.getTimeOut();
        if (timeout != null) {
            setIntegerIfNotNull(dataSourceModel, ALLOCATION_RETRY, timeout.getAllocationRetry());
            setLongIfNotNull(dataSourceModel, ALLOCATION_RETRY_WAIT_MILLIS, timeout.getAllocationRetryWaitMillis());
            setLongIfNotNull(dataSourceModel, BLOCKING_TIMEOUT_WAIT_MILLIS, timeout.getBlockingTimeoutMillis());
            setLongIfNotNull(dataSourceModel, IDLETIMEOUTMINUTES, timeout.getIdleTimeoutMinutes());
            setLongIfNotNull(dataSourceModel, QUERYTIMEOUT, timeout.getQueryTimeout());
            setLongIfNotNull(dataSourceModel, USETRYLOCK, timeout.getUseTryLock());
            setBooleanIfTrue(dataSourceModel, SETTXQUERYTIMEOUT, timeout.isSetTxQueryTimeout());
        }
        if (dataSource.getTransactionIsolation() != null) {
            setStringIfNotNull(dataSourceModel, TRANSACTION_ISOLOATION, dataSource.getTransactionIsolation().name());
        }

        if (dataSource.isSpy()) {
            setBooleanIfNotNull(dataSourceModel, SPY, dataSource.isSpy());
        }

        if (dataSource.isUseCcm()) {
            setBooleanIfNotNull(dataSourceModel, USE_CCM, dataSource.isUseCcm());
        }

        Validation validation = dataSource.getValidation();
        if (validation != null) {
            setStringIfNotNull(dataSourceModel, CHECKVALIDCONNECTIONSQL, validation.getCheckValidConnectionSql());
            setExtensionIfNotNull(dataSourceModel, EXCEPTIONSORTERCLASSNAME, EXCEPTIONSORTER_PROPERTIES,
                    validation.getExceptionSorter());
            setExtensionIfNotNull(dataSourceModel, STALECONNECTIONCHECKERCLASSNAME, STALECONNECTIONCHECKER_PROPERTIES,
                    validation.getStaleConnectionChecker());
            setExtensionIfNotNull(dataSourceModel, VALIDCONNECTIONCHECKERCLASSNAME, VALIDCONNECTIONCHECKER_PROPERTIES,
                    validation.getValidConnectionChecker());
            setLongIfNotNull(dataSourceModel, BACKGROUNDVALIDATIONMINUTES, validation.getBackgroundValidationMinutes());
            setBooleanIfNotNull(dataSourceModel, BACKGROUNDVALIDATION, validation.isBackgroundValidation());
            setBooleanIfNotNull(dataSourceModel, USE_FAST_FAIL, validation.isUseFastFail());
            setBooleanIfNotNull(dataSourceModel, VALIDATEONMATCH, validation.isValidateOnMatch());
        }
    }

    static void fillFrom(final ModelNode xaDataSourceModel, final XaDataSource xaDataSource) {
        for (Map.Entry<String, String> entry : xaDataSource.getXaDataSourceProperty().entrySet()) {
            xaDataSourceModel.get(XADATASOURCEPROPERTIES).add(entry.getKey(), entry.getValue());
        }
        setStringIfNotNull(xaDataSourceModel, XADATASOURCECLASS, xaDataSource.getXaDataSourceClass());
        setStringIfNotNull(xaDataSourceModel, JNDINAME, xaDataSource.getJndiName());
        setStringIfNotNull(xaDataSourceModel, DATASOURCE_DRIVER, xaDataSource.getDriver());
        setStringIfNotNull(xaDataSourceModel, NEW_CONNECTION_SQL, xaDataSource.getNewConnectionSql());
        setStringIfNotNull(xaDataSourceModel, POOLNAME, xaDataSource.getPoolName());
        setStringIfNotNull(xaDataSourceModel, URL_DELIMITER, xaDataSource.getUrlDelimiter());
        setStringIfNotNull(xaDataSourceModel, URL_SELECTOR_STRATEGY_CLASS_NAME, xaDataSource.getUrlSelectorStrategyClassName());
        setBooleanIfNotNull(xaDataSourceModel, USE_JAVA_CONTEXT, xaDataSource.isUseJavaContext());
        setBooleanIfNotNull(xaDataSourceModel, ENABLED, xaDataSource.isEnabled());
        final CommonXaPool pool = xaDataSource.getXaPool();
        if (pool != null) {
            setIntegerIfNotNull(xaDataSourceModel, MAX_POOL_SIZE, pool.getMaxPoolSize());
            setIntegerIfNotNull(xaDataSourceModel, MIN_POOL_SIZE, pool.getMinPoolSize());
            setBooleanIfNotNull(xaDataSourceModel, POOL_PREFILL, pool.isPrefill());
            setBooleanIfNotNull(xaDataSourceModel, POOL_USE_STRICT_MIN, pool.isUseStrictMin());
            if (pool.getFlushStrategy() != null) {
                setStringIfNotNull(xaDataSourceModel, FLUSH_STRATEGY, pool.getFlushStrategy().getName());
            }
            setBooleanIfNotNull(xaDataSourceModel, INTERLIVING, pool.isInterleaving());
            setBooleanIfNotNull(xaDataSourceModel, NOTXSEPARATEPOOL, pool.isNoTxSeparatePool());
            setBooleanIfNotNull(xaDataSourceModel, PAD_XID, pool.isPadXid());
            setBooleanIfNotNull(xaDataSourceModel, SAME_RM_OVERRIDE, pool.isSameRmOverride());
            setBooleanIfNotNull(xaDataSourceModel, WRAP_XA_DATASOURCE, pool.isWrapXaDataSource());
        }
        final DsSecurity security = xaDataSource.getSecurity();
        if (security != null) {
            setStringIfNotNull(xaDataSourceModel, USERNAME, security.getUserName());
            setStringIfNotNull(xaDataSourceModel, PASSWORD, security.getPassword());
            setStringIfNotNull(xaDataSourceModel, SECURITY_DOMAIN, security.getSecurityDomain());
            setExtensionIfNotNull(xaDataSourceModel, REAUTHPLUGIN_CLASSNAME, REAUTHPLUGIN_PROPERTIES,
                    security.getReauthPlugin());
        }
        final Statement statement = xaDataSource.getStatement();
        if (statement != null) {
            setLongIfNotNull(xaDataSourceModel, PREPAREDSTATEMENTSCACHESIZE, statement.getPreparedStatementsCacheSize());
            setBooleanIfNotNull(xaDataSourceModel, SHAREPREPAREDSTATEMENTS, statement.isSharePreparedStatements());
            if (statement.getTrackStatements() != null) {
                setStringIfNotNull(xaDataSourceModel, TRACKSTATEMENTS, statement.getTrackStatements().name());
            }
        }
        final TimeOut timeout = xaDataSource.getTimeOut();
        if (timeout != null) {
            setIntegerIfNotNull(xaDataSourceModel, ALLOCATION_RETRY, timeout.getAllocationRetry());
            setLongIfNotNull(xaDataSourceModel, ALLOCATION_RETRY_WAIT_MILLIS, timeout.getAllocationRetryWaitMillis());
            setLongIfNotNull(xaDataSourceModel, BLOCKING_TIMEOUT_WAIT_MILLIS, timeout.getBlockingTimeoutMillis());
            setLongIfNotNull(xaDataSourceModel, IDLETIMEOUTMINUTES, timeout.getIdleTimeoutMinutes());
            setLongIfNotNull(xaDataSourceModel, QUERYTIMEOUT, timeout.getQueryTimeout());
            setLongIfNotNull(xaDataSourceModel, USETRYLOCK, timeout.getUseTryLock());
            setBooleanIfTrue(xaDataSourceModel, SETTXQUERYTIMEOUT, timeout.isSetTxQueryTimeout());
            setIntegerIfNotNull(xaDataSourceModel, XA_RESOURCE_TIMEOUT, timeout.getXaResourceTimeout());
        }
        if (xaDataSource.getTransactionIsolation() != null) {
            setStringIfNotNull(xaDataSourceModel, TRANSACTION_ISOLOATION, xaDataSource.getTransactionIsolation().name());
        }

        if (xaDataSource.isSpy()) {
            setBooleanIfNotNull(xaDataSourceModel, SPY, xaDataSource.isSpy());
        }

        if (xaDataSource.isUseCcm()) {
            setBooleanIfNotNull(xaDataSourceModel, USE_CCM, xaDataSource.isUseCcm());
        }

        final Validation validation = xaDataSource.getValidation();
        if (xaDataSource.getValidation() != null) {
            setStringIfNotNull(xaDataSourceModel, CHECKVALIDCONNECTIONSQL, validation.getCheckValidConnectionSql());
            setExtensionIfNotNull(xaDataSourceModel, EXCEPTIONSORTERCLASSNAME, EXCEPTIONSORTER_PROPERTIES,
                    validation.getExceptionSorter());
            setExtensionIfNotNull(xaDataSourceModel, STALECONNECTIONCHECKERCLASSNAME, STALECONNECTIONCHECKER_PROPERTIES,
                    validation.getStaleConnectionChecker());
            setExtensionIfNotNull(xaDataSourceModel, VALIDCONNECTIONCHECKERCLASSNAME, VALIDCONNECTIONCHECKER_PROPERTIES,
                    validation.getValidConnectionChecker());
            setLongIfNotNull(xaDataSourceModel, BACKGROUNDVALIDATIONMINUTES, validation.getBackgroundValidationMinutes());
            setBooleanIfNotNull(xaDataSourceModel, BACKGROUNDVALIDATION, validation.isBackgroundValidation());
            setBooleanIfNotNull(xaDataSourceModel, USE_FAST_FAIL, validation.isUseFastFail());
            setBooleanIfNotNull(xaDataSourceModel, VALIDATEONMATCH, validation.isValidateOnMatch());
        }

        if (xaDataSource.getRecovery() != null) {
            final Recovery recovery = xaDataSource.getRecovery();
            setStringIfNotNull(xaDataSourceModel, RECOVERY_USERNAME, recovery.getCredential() != null ? recovery
                    .getCredential().getUserName() : null);
            setStringIfNotNull(xaDataSourceModel, RECOVERY_PASSWORD, recovery.getCredential() != null ? recovery
                    .getCredential().getPassword() : null);
            setStringIfNotNull(xaDataSourceModel, RECOVERY_SECURITY_DOMAIN, recovery.getCredential() != null ? recovery
                    .getCredential().getSecurityDomain() : null);
            setExtensionIfNotNull(xaDataSourceModel, RECOVERLUGIN_CLASSNAME, RECOVERLUGIN_PROPERTIES,
                    recovery.getRecoverPlugin());
            setBooleanIfNotNull(xaDataSourceModel, NO_RECOVERY, recovery.getNoRecovery());
        }
    }

    static DataSource from(final ModelNode dataSourceNode) throws ValidateException {
        final Map<String, String> connectionProperties;
        if (dataSourceNode.hasDefined(CONNECTION_PROPERTIES)) {
            connectionProperties = new HashMap<String, String>(dataSourceNode.get(CONNECTION_PROPERTIES).asList().size());
            for (Property property : dataSourceNode.get(CONNECTION_PROPERTIES).asPropertyList()) {
                connectionProperties.put(property.getName(), property.getValue().asString());
            }
        } else {
            connectionProperties = Collections.emptyMap();
        }
        final String connectionUrl = getStringIfSetOrGetDefault(dataSourceNode, CONNECTION_URL, null);
        final String driverClass = getStringIfSetOrGetDefault(dataSourceNode, DATASOURCE_DRIVER_CLASS, null);
        final String jndiName = getStringIfSetOrGetDefault(dataSourceNode, JNDINAME, null);
        final String driver = getStringIfSetOrGetDefault(dataSourceNode, DATASOURCE_DRIVER, null);
        final String newConnectionSql = getStringIfSetOrGetDefault(dataSourceNode, NEW_CONNECTION_SQL, null);
        final String poolName = getStringIfSetOrGetDefault(dataSourceNode, POOLNAME, null);
        final String urlDelimiter = getStringIfSetOrGetDefault(dataSourceNode, URL_DELIMITER, null);
        final String urlSelectorStrategyClassName = getStringIfSetOrGetDefault(dataSourceNode,
                URL_SELECTOR_STRATEGY_CLASS_NAME, null);
        final boolean useJavaContext = getBooleanIfSetOrGetDefault(dataSourceNode, USE_JAVA_CONTEXT, false);
        final boolean enabled = getBooleanIfSetOrGetDefault(dataSourceNode, ENABLED, true);
        final boolean jta = getBooleanIfSetOrGetDefault(dataSourceNode, JTA, true);
        final Integer maxPoolSize = getIntIfSetOrGetDefault(dataSourceNode, MAX_POOL_SIZE, null);
        final Integer minPoolSize = getIntIfSetOrGetDefault(dataSourceNode, MIN_POOL_SIZE, null);
        final boolean prefill = getBooleanIfSetOrGetDefault(dataSourceNode, POOL_PREFILL, false);
        final boolean useStrictMin = getBooleanIfSetOrGetDefault(dataSourceNode, POOL_USE_STRICT_MIN, false);
        final FlushStrategy flushStrategy = dataSourceNode.hasDefined(FLUSH_STRATEGY) ? FlushStrategy.forName(dataSourceNode
                .get(FLUSH_STRATEGY).asString()) : FlushStrategy.FAILING_CONNECTION_ONLY;

        final CommonPool pool = new CommonPoolImpl(minPoolSize, maxPoolSize, prefill, useStrictMin, flushStrategy);

        final String username = getStringIfSetOrGetDefault(dataSourceNode, USERNAME, null);
        final String password = getStringIfSetOrGetDefault(dataSourceNode, PASSWORD, null);
        final String securityDomain = getStringIfSetOrGetDefault(dataSourceNode, SECURITY_DOMAIN, null);

        final Extension reauthPlugin = extractExtension(dataSourceNode, REAUTHPLUGIN_CLASSNAME, REAUTHPLUGIN_PROPERTIES);

        final DsSecurity security = new DsSecurityImpl(username, password, securityDomain, reauthPlugin);

        final boolean sharePreparedStatements = getBooleanIfSetOrGetDefault(dataSourceNode, USE_JAVA_CONTEXT, false);
        final Long preparedStatementsCacheSize = getLongIfSetOrGetDefault(dataSourceNode, PREPAREDSTATEMENTSCACHESIZE, null);
        final Statement.TrackStatementsEnum trackStatements = dataSourceNode.hasDefined(TRACKSTATEMENTS) ? Statement.TrackStatementsEnum
                .valueOf(dataSourceNode.get(TRACKSTATEMENTS).asString()) : Statement.TrackStatementsEnum.NOWARN;
        final Statement statement = new StatementImpl(sharePreparedStatements, preparedStatementsCacheSize, trackStatements);

        final Integer allocationRetry = getIntIfSetOrGetDefault(dataSourceNode, ALLOCATION_RETRY, null);
        final Long allocationRetryWaitMillis = getLongIfSetOrGetDefault(dataSourceNode, ALLOCATION_RETRY_WAIT_MILLIS, null);
        final Long blockingTimeoutMillis = getLongIfSetOrGetDefault(dataSourceNode, BLOCKING_TIMEOUT_WAIT_MILLIS, null);
        final Long idleTimeoutMinutes = getLongIfSetOrGetDefault(dataSourceNode, IDLETIMEOUTMINUTES, null);
        final Long queryTimeout = getLongIfSetOrGetDefault(dataSourceNode, QUERYTIMEOUT, null);
        final Integer xaResourceTimeout = getIntIfSetOrGetDefault(dataSourceNode, XA_RESOURCE_TIMEOUT, null);
        final Long useTryLock = getLongIfSetOrGetDefault(dataSourceNode, USETRYLOCK, null);
        final boolean setTxQuertTimeout = getBooleanIfSetOrGetDefault(dataSourceNode, SETTXQUERYTIMEOUT, false);
        final TimeOut timeOut = new TimeOutImpl(blockingTimeoutMillis, idleTimeoutMinutes, allocationRetry,
                allocationRetryWaitMillis, xaResourceTimeout, setTxQuertTimeout, queryTimeout, useTryLock);
        final TransactionIsolation transactionIsolation = dataSourceNode.hasDefined(TRANSACTION_ISOLOATION) ? TransactionIsolation
                .valueOf(dataSourceNode.get(TRANSACTION_ISOLOATION).asString()) : null;
        final String checkValidConnectionSql = getStringIfSetOrGetDefault(dataSourceNode, CHECKVALIDCONNECTIONSQL, null);

        final Extension exceptionSorter = extractExtension(dataSourceNode, EXCEPTIONSORTERCLASSNAME, EXCEPTIONSORTER_PROPERTIES);
        final Extension staleConnectionChecker = extractExtension(dataSourceNode, STALECONNECTIONCHECKERCLASSNAME,
                STALECONNECTIONCHECKER_PROPERTIES);
        final Extension validConnectionChecker = extractExtension(dataSourceNode, VALIDCONNECTIONCHECKERCLASSNAME,
                VALIDCONNECTIONCHECKER_PROPERTIES);

        final Long backgroundValidationMinutes = getLongIfSetOrGetDefault(dataSourceNode, BACKGROUNDVALIDATIONMINUTES, null);
        final boolean backgroundValidation = getBooleanIfSetOrGetDefault(dataSourceNode, BACKGROUNDVALIDATION, false);
        final boolean useFastFail = getBooleanIfSetOrGetDefault(dataSourceNode, USE_FAST_FAIL, false);
        final boolean validateOnMatch = getBooleanIfSetOrGetDefault(dataSourceNode, VALIDATEONMATCH, false);
        final boolean spy = getBooleanIfSetOrGetDefault(dataSourceNode, SPY, false);
        final boolean useCcm = getBooleanIfSetOrGetDefault(dataSourceNode, USE_CCM, true);

        final Validation validation = new ValidationImpl(backgroundValidation, backgroundValidationMinutes, useFastFail,
                validConnectionChecker, checkValidConnectionSql, validateOnMatch, staleConnectionChecker, exceptionSorter);

        return new DataSourceImpl(connectionUrl, driverClass, driver, transactionIsolation, connectionProperties, timeOut,
                security, statement, validation, urlDelimiter, urlSelectorStrategyClassName, newConnectionSql, useJavaContext,
                poolName, enabled, jndiName, spy, useCcm, jta, pool);
    }

    static XaDataSource xaFrom(final ModelNode dataSourceNode) throws ValidateException {
        List<Property> propertyList = dataSourceNode.get(XADATASOURCEPROPERTIES).asPropertyList();
        final Map<String, String> xaDataSourceProperty = new HashMap<String, String>(propertyList.size());
        for (Property property : propertyList) {
            xaDataSourceProperty.put(property.getName(), property.getValue().asString());
        }
        final String xaDataSourceClass = getStringIfSetOrGetDefault(dataSourceNode, XADATASOURCECLASS, null);
        final String jndiName = getStringIfSetOrGetDefault(dataSourceNode, JNDINAME, null);
        final String module = getStringIfSetOrGetDefault(dataSourceNode, DATASOURCE_DRIVER, null);
        final String newConnectionSql = getStringIfSetOrGetDefault(dataSourceNode, NEW_CONNECTION_SQL, null);
        final String poolName = getStringIfSetOrGetDefault(dataSourceNode, POOLNAME, null);
        final String urlDelimiter = getStringIfSetOrGetDefault(dataSourceNode, URL_DELIMITER, null);
        final String urlSelectorStrategyClassName = getStringIfSetOrGetDefault(dataSourceNode,
                URL_SELECTOR_STRATEGY_CLASS_NAME, null);
        final boolean useJavaContext = getBooleanIfSetOrGetDefault(dataSourceNode, USE_JAVA_CONTEXT, false);
        final boolean enabled = getBooleanIfSetOrGetDefault(dataSourceNode, ENABLED, true);
        final Integer maxPoolSize = getIntIfSetOrGetDefault(dataSourceNode, MAX_POOL_SIZE, null);
        final Integer minPoolSize = getIntIfSetOrGetDefault(dataSourceNode, MIN_POOL_SIZE, null);
        final boolean prefill = getBooleanIfSetOrGetDefault(dataSourceNode, POOL_PREFILL, false);
        final boolean useStrictMin = getBooleanIfSetOrGetDefault(dataSourceNode, POOL_USE_STRICT_MIN, false);
        final boolean interleaving = getBooleanIfSetOrGetDefault(dataSourceNode, INTERLIVING, false);
        final boolean noTxSeparatePool = getBooleanIfSetOrGetDefault(dataSourceNode, NOTXSEPARATEPOOL, false);
        final boolean padXid = getBooleanIfSetOrGetDefault(dataSourceNode, PAD_XID, false);
        final boolean isSameRmOverride = getBooleanIfSetOrGetDefault(dataSourceNode, SAME_RM_OVERRIDE, false);
        final boolean wrapXaDataSource = getBooleanIfSetOrGetDefault(dataSourceNode, WRAP_XA_DATASOURCE, false);
        final FlushStrategy flushStrategy = dataSourceNode.hasDefined(FLUSH_STRATEGY) ? FlushStrategy.forName(dataSourceNode
                .get(FLUSH_STRATEGY).asString()) : FlushStrategy.FAILING_CONNECTION_ONLY;

        final CommonXaPool xaPool = new CommonXaPoolImpl(minPoolSize, maxPoolSize, prefill, useStrictMin, flushStrategy,
                isSameRmOverride, interleaving, padXid, wrapXaDataSource, noTxSeparatePool);

        final String username = getStringIfSetOrGetDefault(dataSourceNode, USERNAME, null);
        final String password = getStringIfSetOrGetDefault(dataSourceNode, PASSWORD, null);
        final String securityDomain = getStringIfSetOrGetDefault(dataSourceNode, SECURITY_DOMAIN, null);

        final Extension reauthPlugin = extractExtension(dataSourceNode, REAUTHPLUGIN_CLASSNAME, REAUTHPLUGIN_PROPERTIES);

        final DsSecurity security = new DsSecurityImpl(username, password, securityDomain, reauthPlugin);

        final boolean sharePreparedStatements = dataSourceNode.hasDefined(SHAREPREPAREDSTATEMENTS) ? dataSourceNode.get(
                SHAREPREPAREDSTATEMENTS).asBoolean() : false;
        final Long preparedStatementsCacheSize = getLongIfSetOrGetDefault(dataSourceNode, PREPAREDSTATEMENTSCACHESIZE, null);
        final Statement.TrackStatementsEnum trackStatements = dataSourceNode.hasDefined(TRACKSTATEMENTS) ? Statement.TrackStatementsEnum
                .valueOf(dataSourceNode.get(TRACKSTATEMENTS).asString()) : Statement.TrackStatementsEnum.NOWARN;
        final Statement statement = new StatementImpl(sharePreparedStatements, preparedStatementsCacheSize, trackStatements);

        final Integer allocationRetry = getIntIfSetOrGetDefault(dataSourceNode, ALLOCATION_RETRY, null);
        final Long allocationRetryWaitMillis = getLongIfSetOrGetDefault(dataSourceNode, ALLOCATION_RETRY_WAIT_MILLIS, null);
        final Long blockingTimeoutMillis = getLongIfSetOrGetDefault(dataSourceNode, BLOCKING_TIMEOUT_WAIT_MILLIS, null);
        final Long idleTimeoutMinutes = getLongIfSetOrGetDefault(dataSourceNode, IDLETIMEOUTMINUTES, null);
        final Long queryTimeout = getLongIfSetOrGetDefault(dataSourceNode, QUERYTIMEOUT, null);
        final Integer xaResourceTimeout = getIntIfSetOrGetDefault(dataSourceNode, XA_RESOURCE_TIMEOUT, null);
        final Long useTryLock = getLongIfSetOrGetDefault(dataSourceNode, USETRYLOCK, null);
        final boolean setTxQuertTimeout = getBooleanIfSetOrGetDefault(dataSourceNode, SETTXQUERYTIMEOUT, false);
        final TimeOut timeOut = new TimeOutImpl(blockingTimeoutMillis, idleTimeoutMinutes, allocationRetry,
                allocationRetryWaitMillis, xaResourceTimeout, setTxQuertTimeout, queryTimeout, useTryLock);
        final TransactionIsolation transactionIsolation = dataSourceNode.hasDefined(TRANSACTION_ISOLOATION) ? TransactionIsolation
                .valueOf(dataSourceNode.get(TRANSACTION_ISOLOATION).asString()) : null;
        final String checkValidConnectionSql = getStringIfSetOrGetDefault(dataSourceNode, CHECKVALIDCONNECTIONSQL, null);

        final Extension exceptionSorter = extractExtension(dataSourceNode, EXCEPTIONSORTERCLASSNAME, EXCEPTIONSORTER_PROPERTIES);
        final Extension staleConnectionChecker = extractExtension(dataSourceNode, STALECONNECTIONCHECKERCLASSNAME,
                STALECONNECTIONCHECKER_PROPERTIES);
        final Extension validConnectionChecker = extractExtension(dataSourceNode, VALIDCONNECTIONCHECKERCLASSNAME,
                VALIDCONNECTIONCHECKER_PROPERTIES);

        final Long backgroundValidationMinutes = getLongIfSetOrGetDefault(dataSourceNode, BACKGROUNDVALIDATIONMINUTES, null);
        final boolean backgroundValidation = getBooleanIfSetOrGetDefault(dataSourceNode, BACKGROUNDVALIDATION, false);
        final boolean useFastFail = getBooleanIfSetOrGetDefault(dataSourceNode, USE_FAST_FAIL, false);
        final boolean validateOnMatch = getBooleanIfSetOrGetDefault(dataSourceNode, VALIDATEONMATCH, false);
        final boolean spy = getBooleanIfSetOrGetDefault(dataSourceNode, SPY, false);
        final boolean useCcm = getBooleanIfSetOrGetDefault(dataSourceNode, USE_CCM, true);
        final Validation validation = new ValidationImpl(backgroundValidation, backgroundValidationMinutes, useFastFail,
                validConnectionChecker, checkValidConnectionSql, validateOnMatch, staleConnectionChecker, exceptionSorter);

        final String recoveryUsername = getStringIfSetOrGetDefault(dataSourceNode, RECOVERY_USERNAME, null);
        final String recoveryPassword = getStringIfSetOrGetDefault(dataSourceNode, RECOVERY_PASSWORD, null);
        final String recoverySecurityDomain = getStringIfSetOrGetDefault(dataSourceNode, RECOVERY_SECURITY_DOMAIN, null);

        final Credential credential = new CredentialImpl(recoveryUsername, recoveryPassword, recoverySecurityDomain);

        final Extension recoverPlugin = extractExtension(dataSourceNode, RECOVERLUGIN_CLASSNAME, RECOVERLUGIN_PROPERTIES);
        final boolean noRecovery = getBooleanIfSetOrGetDefault(dataSourceNode, NO_RECOVERY, false);
        Recovery recovery = new Recovery(credential, recoverPlugin, noRecovery);
        return new XADataSourceImpl(transactionIsolation, timeOut, security, statement, validation, urlDelimiter,
                urlSelectorStrategyClassName, useJavaContext, poolName, enabled, jndiName, spy, useCcm, xaDataSourceProperty,
                xaDataSourceClass, module, newConnectionSql, xaPool, recovery);
    }

    private static void setBooleanIfNotNull(final ModelNode node, final String identifier, final Boolean value) {
        if (value != null) {
            node.get(identifier).set(value);
        }
    }

    private static void setBooleanIfTrue(final ModelNode node, final String identifier, final Boolean value) {
        if (value != null && value.booleanValue() == true) {
            node.get(identifier).set(value);
        }
    }

    private static void setIntegerIfNotNull(final ModelNode node, final String identifier, final Integer value) {
        if (value != null) {
            node.get(identifier).set(value);
        }
    }

    private static void setLongIfNotNull(final ModelNode node, final String identifier, final Long value) {
        if (value != null) {
            node.get(identifier).set(value);
        }
    }

    private static void setStringIfNotNull(final ModelNode node, final String identifier, final String value) {
        if (value != null) {
            node.get(identifier).set(value);
        }
    }

    private static void setExtensionIfNotNull(final ModelNode dsModel, final String extensionclassname,
            final String extensionProperties, final Extension extension) {
        if (extension != null) {
            setStringIfNotNull(dsModel, extensionclassname, extension.getClassName());
            if (extension.getConfigPropertiesMap() != null) {
                for (Map.Entry<String, String> entry : extension.getConfigPropertiesMap().entrySet()) {
                    dsModel.get(extensionProperties, entry.getKey()).set(entry.getValue());
                }
            }
        }

    }

    private static Long getLongIfSetOrGetDefault(final ModelNode dataSourceNode, final String key, final Long defaultValue) {
        if (dataSourceNode.hasDefined(key)) {
            return dataSourceNode.get(key).asLong();
        } else {
            return defaultValue;
        }
    }

    private static Integer getIntIfSetOrGetDefault(final ModelNode dataSourceNode, final String key, final Integer defaultValue) {
        if (dataSourceNode.hasDefined(key)) {
            return dataSourceNode.get(key).asInt();
        } else {
            return defaultValue;
        }
    }

    private static boolean getBooleanIfSetOrGetDefault(final ModelNode dataSourceNode, final String key,
            final boolean defaultValue) {
        if (dataSourceNode.hasDefined(key)) {
            return dataSourceNode.get(key).asBoolean();
        } else {
            return defaultValue;
        }
    }

    private static String getStringIfSetOrGetDefault(final ModelNode dataSourceNode, final String key, final String defaultValue) {
        if (dataSourceNode.hasDefined(key)) {
            return dataSourceNode.get(key).asString();
        } else {
            return defaultValue;
        }
    }

    private static Extension extractExtension(final ModelNode dataSourceNode, final String className, final String propertyName)
            throws ValidateException {
        if (dataSourceNode.hasDefined(className)) {
            String exceptionSorterClassName = dataSourceNode.get(className).asString();

            getStringIfSetOrGetDefault(dataSourceNode, className, null);

            Map<String, String> exceptionSorterProperty = null;
            if (dataSourceNode.hasDefined(propertyName)) {
                exceptionSorterProperty = new HashMap<String, String>(dataSourceNode.get(propertyName).asList().size());
                for (ModelNode property : dataSourceNode.get(propertyName).asList()) {
                    exceptionSorterProperty.put(property.asProperty().getName(), property.asString());
                }
            }

            return new Extension(exceptionSorterClassName, exceptionSorterProperty);
        } else {
            return null;
        }
    }
}
