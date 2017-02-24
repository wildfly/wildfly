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

import static org.jboss.as.connector.subsystems.common.pool.Constants.BACKGROUNDVALIDATION;
import static org.jboss.as.connector.subsystems.common.pool.Constants.BACKGROUNDVALIDATIONMILLIS;
import static org.jboss.as.connector.subsystems.common.pool.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_DECREMENTER_CLASS;
import static org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_DECREMENTER_PROPERTIES;
import static org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_INCREMENTER_CLASS;
import static org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_INCREMENTER_PROPERTIES;
import static org.jboss.as.connector.subsystems.common.pool.Constants.IDLETIMEOUTMINUTES;
import static org.jboss.as.connector.subsystems.common.pool.Constants.INITIAL_POOL_SIZE;
import static org.jboss.as.connector.subsystems.common.pool.Constants.MAX_POOL_SIZE;
import static org.jboss.as.connector.subsystems.common.pool.Constants.MIN_POOL_SIZE;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_FAIR;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_FLUSH_STRATEGY;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_PREFILL;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_USE_STRICT_MIN;
import static org.jboss.as.connector.subsystems.common.pool.Constants.USE_FAST_FAIL;
import static org.jboss.as.connector.subsystems.datasources.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.datasources.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.datasources.Constants.ALLOW_MULTIPLE_USERS;
import static org.jboss.as.connector.subsystems.datasources.Constants.AUTHENTICATION_CONTEXT;
import static org.jboss.as.connector.subsystems.datasources.Constants.CHECK_VALID_CONNECTION_SQL;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTABLE;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_LISTENER_CLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_LISTENER_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_URL;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_CLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_DRIVER;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_CLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.ELYTRON_ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.ENLISTMENT_TRACE;
import static org.jboss.as.connector.subsystems.datasources.Constants.EXCEPTION_SORTER_CLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.EXCEPTION_SORTER_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.INTERLEAVING;
import static org.jboss.as.connector.subsystems.datasources.Constants.JNDI_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.JTA;
import static org.jboss.as.connector.subsystems.datasources.Constants.MCP;
import static org.jboss.as.connector.subsystems.datasources.Constants.NEW_CONNECTION_SQL;
import static org.jboss.as.connector.subsystems.datasources.Constants.NO_RECOVERY;
import static org.jboss.as.connector.subsystems.datasources.Constants.NO_TX_SEPARATE_POOL;
import static org.jboss.as.connector.subsystems.datasources.Constants.PAD_XID;
import static org.jboss.as.connector.subsystems.datasources.Constants.PASSWORD;
import static org.jboss.as.connector.subsystems.datasources.Constants.PREPARED_STATEMENTS_CACHE_SIZE;
import static org.jboss.as.connector.subsystems.datasources.Constants.QUERY_TIMEOUT;
import static org.jboss.as.connector.subsystems.datasources.Constants.REAUTHPLUGIN_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.REAUTH_PLUGIN_CLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_AUTHENTICATION_CONTEXT;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_ELYTRON_ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_PASSWORD;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_USERNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVER_PLUGIN_CLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVER_PLUGIN_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.SAME_RM_OVERRIDE;
import static org.jboss.as.connector.subsystems.datasources.Constants.SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.datasources.Constants.SET_TX_QUERY_TIMEOUT;
import static org.jboss.as.connector.subsystems.datasources.Constants.SHARE_PREPARED_STATEMENTS;
import static org.jboss.as.connector.subsystems.datasources.Constants.SPY;
import static org.jboss.as.connector.subsystems.datasources.Constants.STALE_CONNECTION_CHECKER_CLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.STALE_CONNECTION_CHECKER_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.TRACKING;
import static org.jboss.as.connector.subsystems.datasources.Constants.TRACK_STATEMENTS;
import static org.jboss.as.connector.subsystems.datasources.Constants.TRANSACTION_ISOLATION;
import static org.jboss.as.connector.subsystems.datasources.Constants.URL_DELIMITER;
import static org.jboss.as.connector.subsystems.datasources.Constants.URL_PROPERTY;
import static org.jboss.as.connector.subsystems.datasources.Constants.URL_SELECTOR_STRATEGY_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.USERNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.USE_CCM;
import static org.jboss.as.connector.subsystems.datasources.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.connector.subsystems.datasources.Constants.USE_TRY_LOCK;
import static org.jboss.as.connector.subsystems.datasources.Constants.VALIDATE_ON_MATCH;
import static org.jboss.as.connector.subsystems.datasources.Constants.VALID_CONNECTION_CHECKER_CLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.VALID_CONNECTION_CHECKER_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.WRAP_XA_RESOURCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_DATASOURCE_CLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_RESOURCE_TIMEOUT;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.jboss.as.connector.metadata.api.common.Credential;
import org.jboss.as.connector.metadata.api.ds.DsSecurity;
import org.jboss.as.connector.metadata.common.CredentialImpl;
import org.jboss.as.connector.metadata.ds.DsSecurityImpl;
import org.jboss.as.connector.util.ModelNodeUtil;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.api.metadata.Defaults;
import org.jboss.jca.common.api.metadata.common.Capacity;
import org.jboss.jca.common.api.metadata.common.Extension;
import org.jboss.jca.common.api.metadata.common.FlushStrategy;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.ds.DsPool;
import org.jboss.jca.common.api.metadata.ds.DsXaPool;
import org.jboss.jca.common.api.metadata.ds.Statement;
import org.jboss.jca.common.api.metadata.ds.TimeOut;
import org.jboss.jca.common.api.metadata.ds.TransactionIsolation;
import org.jboss.jca.common.api.metadata.ds.Validation;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.jca.common.metadata.ds.DsPoolImpl;
import org.jboss.jca.common.metadata.ds.DsXaPoolImpl;
import org.jboss.jca.common.metadata.ds.StatementImpl;
import org.jboss.jca.common.metadata.ds.TimeOutImpl;
import org.jboss.jca.common.metadata.ds.ValidationImpl;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.security.credential.source.CredentialSource;

/**
 * Utility used to help convert between JCA spi data-source instances and model
 * node representations and vice-versa.
 * @author John Bailey
 */
class DataSourceModelNodeUtil {

    static ModifiableDataSource from(final OperationContext operationContext, final ModelNode dataSourceNode, final String dsName, final ExceptionSupplier<CredentialSource, Exception> credentialSourceSupplier) throws OperationFailedException, ValidateException {
        final Map<String, String> connectionProperties= Collections.emptyMap();

        final String connectionUrl = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, CONNECTION_URL);
        final String driverClass = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, DRIVER_CLASS);
        final String dataSourceClass = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, DATASOURCE_CLASS);
        final String jndiName = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, JNDI_NAME);
        final String driver = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, DATASOURCE_DRIVER);
        final String newConnectionSql = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, NEW_CONNECTION_SQL);
        final String poolName = dsName;
        final String urlDelimiter = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, URL_DELIMITER);
        final String urlSelectorStrategyClassName = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode,
                URL_SELECTOR_STRATEGY_CLASS_NAME);
        final boolean useJavaContext = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, USE_JAVA_CONTEXT);
        final boolean enabled = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, ENABLED);
        final boolean connectable = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, CONNECTABLE);
        final Boolean tracking = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, TRACKING);
        final Boolean enlistmentTrace = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, ENLISTMENT_TRACE);
        final String mcp = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, MCP);
        final boolean jta = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, JTA);
        final Integer maxPoolSize = ModelNodeUtil.getIntIfSetOrGetDefault(operationContext, dataSourceNode, MAX_POOL_SIZE);
        final Integer minPoolSize = ModelNodeUtil.getIntIfSetOrGetDefault(operationContext, dataSourceNode, MIN_POOL_SIZE);
        final Integer initialPoolSize = ModelNodeUtil.getIntIfSetOrGetDefault(operationContext, dataSourceNode, INITIAL_POOL_SIZE);
        final boolean prefill = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, POOL_PREFILL);
        final boolean fair = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, POOL_FAIR);
        final boolean useStrictMin = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, POOL_USE_STRICT_MIN);
        final String flushStrategyString = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, POOL_FLUSH_STRATEGY);
        final FlushStrategy flushStrategy = FlushStrategy.forName(flushStrategyString); // TODO relax case sensitivity
        final Boolean allowMultipleUsers = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, ALLOW_MULTIPLE_USERS);
        Extension incrementer = ModelNodeUtil.extractExtension(operationContext, dataSourceNode, CAPACITY_INCREMENTER_CLASS, CAPACITY_INCREMENTER_PROPERTIES);
        Extension decrementer = ModelNodeUtil.extractExtension(operationContext, dataSourceNode, CAPACITY_DECREMENTER_CLASS, CAPACITY_DECREMENTER_PROPERTIES);
        final Capacity capacity = new Capacity(incrementer, decrementer);
        final Extension connectionListener = ModelNodeUtil.extractExtension(operationContext, dataSourceNode, CONNECTION_LISTENER_CLASS, CONNECTION_LISTENER_PROPERTIES);

        final DsPool pool = new DsPoolImpl(minPoolSize, initialPoolSize, maxPoolSize, prefill, useStrictMin, flushStrategy, allowMultipleUsers, capacity, fair, connectionListener);

        final String username = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, USERNAME);
        final String password = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, PASSWORD);


        final String securityDomain = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, SECURITY_DOMAIN);
        final boolean elytronEnabled = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, ELYTRON_ENABLED);
        final String authenticationContext = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, AUTHENTICATION_CONTEXT);

        final Extension reauthPlugin = ModelNodeUtil.extractExtension(operationContext, dataSourceNode, REAUTH_PLUGIN_CLASSNAME, REAUTHPLUGIN_PROPERTIES);

        final DsSecurity security = new DsSecurityImpl(username, password,
                elytronEnabled? authenticationContext: securityDomain, elytronEnabled, credentialSourceSupplier, reauthPlugin);

        final boolean sharePreparedStatements = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, SHARE_PREPARED_STATEMENTS);
        final Long preparedStatementsCacheSize = ModelNodeUtil.getLongIfSetOrGetDefault(operationContext, dataSourceNode, PREPARED_STATEMENTS_CACHE_SIZE);
        final String trackStatementsString = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, TRACK_STATEMENTS);
        final Statement.TrackStatementsEnum trackStatements = Statement.TrackStatementsEnum.valueOf(trackStatementsString.toUpperCase(Locale.ENGLISH));
        final Statement statement = new StatementImpl(sharePreparedStatements, preparedStatementsCacheSize, trackStatements);

        final Integer allocationRetry = ModelNodeUtil.getIntIfSetOrGetDefault(operationContext, dataSourceNode, ALLOCATION_RETRY);
        final Long allocationRetryWaitMillis = ModelNodeUtil.getLongIfSetOrGetDefault(operationContext, dataSourceNode, ALLOCATION_RETRY_WAIT_MILLIS);
        final Long blockingTimeoutMillis = ModelNodeUtil.getLongIfSetOrGetDefault(operationContext, dataSourceNode, BLOCKING_TIMEOUT_WAIT_MILLIS);
        final Long idleTimeoutMinutes = ModelNodeUtil.getLongIfSetOrGetDefault(operationContext, dataSourceNode, IDLETIMEOUTMINUTES);
        final Long queryTimeout = ModelNodeUtil.getLongIfSetOrGetDefault(operationContext, dataSourceNode, QUERY_TIMEOUT);
        final Integer xaResourceTimeout = ModelNodeUtil.getIntIfSetOrGetDefault(operationContext, dataSourceNode, XA_RESOURCE_TIMEOUT);
        final Long useTryLock = ModelNodeUtil.getLongIfSetOrGetDefault(operationContext, dataSourceNode, USE_TRY_LOCK);
        final boolean setTxQueryTimeout = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, SET_TX_QUERY_TIMEOUT);
        final TimeOut timeOut = new TimeOutImpl(blockingTimeoutMillis, idleTimeoutMinutes, allocationRetry,
                allocationRetryWaitMillis, xaResourceTimeout, setTxQueryTimeout, queryTimeout, useTryLock);
        final String transactionIsolationString = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, TRANSACTION_ISOLATION);
        TransactionIsolation transactionIsolation = null;
        if (transactionIsolationString != null) {
            transactionIsolation = TransactionIsolation.forName(transactionIsolationString); // TODO relax case sensitivity
            if (transactionIsolation == null) {
                transactionIsolation = TransactionIsolation.customLevel(transactionIsolationString);
            }
        }

        final String checkValidConnectionSql = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, CHECK_VALID_CONNECTION_SQL);

        final Extension exceptionSorter = ModelNodeUtil.extractExtension(operationContext, dataSourceNode, EXCEPTION_SORTER_CLASSNAME, EXCEPTION_SORTER_PROPERTIES);
        final Extension staleConnectionChecker = ModelNodeUtil.extractExtension(operationContext, dataSourceNode, STALE_CONNECTION_CHECKER_CLASSNAME,
                STALE_CONNECTION_CHECKER_PROPERTIES);
        final Extension validConnectionChecker = ModelNodeUtil.extractExtension(operationContext, dataSourceNode, VALID_CONNECTION_CHECKER_CLASSNAME,
                VALID_CONNECTION_CHECKER_PROPERTIES);

        Long backgroundValidationMillis = ModelNodeUtil.getLongIfSetOrGetDefault(operationContext, dataSourceNode, BACKGROUNDVALIDATIONMILLIS);
        final Boolean backgroundValidation = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, BACKGROUNDVALIDATION);
        boolean useFastFail = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, USE_FAST_FAIL);
        final Boolean validateOnMatch = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, VALIDATE_ON_MATCH);
        final boolean spy = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, SPY);
        final boolean useCcm = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, USE_CCM);

        final Validation validation = new ValidationImpl(backgroundValidation, backgroundValidationMillis, useFastFail,
                validConnectionChecker, checkValidConnectionSql, validateOnMatch, staleConnectionChecker, exceptionSorter);

        return new ModifiableDataSource(connectionUrl, driverClass, dataSourceClass, driver, transactionIsolation, connectionProperties, timeOut,
                security, statement, validation, urlDelimiter, urlSelectorStrategyClassName, newConnectionSql, useJavaContext,
                poolName, enabled, jndiName, spy, useCcm, jta, connectable, tracking, mcp, enlistmentTrace,  pool);
    }

    static ModifiableXaDataSource xaFrom(final OperationContext operationContext, final ModelNode dataSourceNode, final String dsName,
                                         final ExceptionSupplier<CredentialSource, Exception> credentialSourceSupplier, final ExceptionSupplier<CredentialSource, Exception> recoveryCredentialSourceSupplier) throws OperationFailedException, ValidateException {
        final Map<String, String> xaDataSourceProperty;
        xaDataSourceProperty = Collections.emptyMap();

        final String xaDataSourceClass = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, XA_DATASOURCE_CLASS);
        final String jndiName = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, JNDI_NAME);
        final String module = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, DATASOURCE_DRIVER);
        final String newConnectionSql = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, NEW_CONNECTION_SQL);
        final String poolName = dsName;
        final String urlDelimiter = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, URL_DELIMITER);
        final String urlSelectorStrategyClassName = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode,
                URL_SELECTOR_STRATEGY_CLASS_NAME);
        final Boolean useJavaContext = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, USE_JAVA_CONTEXT);
        final Boolean enabled = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, ENABLED);
        final boolean connectable = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, CONNECTABLE);
        final Boolean tracking = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, TRACKING);
        final Boolean enlistmentTrace = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, ENLISTMENT_TRACE);
        final String mcp = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, MCP);
        final Integer maxPoolSize = ModelNodeUtil.getIntIfSetOrGetDefault(operationContext, dataSourceNode, MAX_POOL_SIZE);
        final Integer minPoolSize = ModelNodeUtil.getIntIfSetOrGetDefault(operationContext, dataSourceNode, MIN_POOL_SIZE);
        final Integer initialPoolSize = ModelNodeUtil.getIntIfSetOrGetDefault(operationContext, dataSourceNode, INITIAL_POOL_SIZE);
        final Boolean prefill = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, POOL_PREFILL);
        final Boolean fair = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, POOL_FAIR);
        final Boolean useStrictMin = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, POOL_USE_STRICT_MIN);
        final Boolean interleaving = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, INTERLEAVING);
        final Boolean noTxSeparatePool = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, NO_TX_SEPARATE_POOL);
        final Boolean padXid = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, PAD_XID);
        final Boolean isSameRmOverride = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, SAME_RM_OVERRIDE);
        final Boolean wrapXaDataSource = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, WRAP_XA_RESOURCE);
        final String flushStrategyString = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, POOL_FLUSH_STRATEGY);
        final FlushStrategy flushStrategy = FlushStrategy.forName(flushStrategyString); // TODO relax case sensitivity

        final Boolean allowMultipleUsers = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, ALLOW_MULTIPLE_USERS);
        Extension incrementer = ModelNodeUtil.extractExtension(operationContext, dataSourceNode, CAPACITY_INCREMENTER_CLASS, CAPACITY_INCREMENTER_PROPERTIES);
        Extension decrementer = ModelNodeUtil.extractExtension(operationContext, dataSourceNode, CAPACITY_DECREMENTER_CLASS, CAPACITY_DECREMENTER_PROPERTIES);
        final Capacity capacity = new Capacity(incrementer, decrementer);
        final Extension connectionListener = ModelNodeUtil.extractExtension(operationContext, dataSourceNode, CONNECTION_LISTENER_CLASS, CONNECTION_LISTENER_PROPERTIES);

        final DsXaPool xaPool = new DsXaPoolImpl(minPoolSize, initialPoolSize, maxPoolSize, prefill, useStrictMin, flushStrategy,
                isSameRmOverride, interleaving, padXid, wrapXaDataSource, noTxSeparatePool, allowMultipleUsers, capacity, fair, connectionListener);

        final String username = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, USERNAME);
        final String password= ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, PASSWORD);
        final String securityDomain = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, SECURITY_DOMAIN);
        final boolean elytronEnabled = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, ELYTRON_ENABLED);
        final String authenticationContext = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, AUTHENTICATION_CONTEXT);

        final Extension reauthPlugin = ModelNodeUtil.extractExtension(operationContext, dataSourceNode, REAUTH_PLUGIN_CLASSNAME, REAUTHPLUGIN_PROPERTIES);

        final DsSecurity security = new DsSecurityImpl(username, password,
                elytronEnabled? authenticationContext: securityDomain, elytronEnabled, credentialSourceSupplier, reauthPlugin);

        final Boolean sharePreparedStatements = dataSourceNode.hasDefined(SHARE_PREPARED_STATEMENTS.getName()) ? dataSourceNode.get(
                SHARE_PREPARED_STATEMENTS.getName()).asBoolean() : Defaults.SHARE_PREPARED_STATEMENTS;
        final Long preparedStatementsCacheSize = ModelNodeUtil.getLongIfSetOrGetDefault(operationContext, dataSourceNode, PREPARED_STATEMENTS_CACHE_SIZE);
        final String trackStatementsString = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, TRACK_STATEMENTS);
        final Statement.TrackStatementsEnum trackStatements = Statement.TrackStatementsEnum.valueOf(trackStatementsString.toUpperCase(Locale.ENGLISH));
        final Statement statement = new StatementImpl(sharePreparedStatements, preparedStatementsCacheSize, trackStatements);

        final Integer allocationRetry = ModelNodeUtil.getIntIfSetOrGetDefault(operationContext, dataSourceNode, ALLOCATION_RETRY);
        final Long allocationRetryWaitMillis = ModelNodeUtil.getLongIfSetOrGetDefault(operationContext, dataSourceNode, ALLOCATION_RETRY_WAIT_MILLIS);
        final Long blockingTimeoutMillis = ModelNodeUtil.getLongIfSetOrGetDefault(operationContext, dataSourceNode, BLOCKING_TIMEOUT_WAIT_MILLIS);
        final Long idleTimeoutMinutes = ModelNodeUtil.getLongIfSetOrGetDefault(operationContext, dataSourceNode, IDLETIMEOUTMINUTES);
        final Long queryTimeout = ModelNodeUtil.getLongIfSetOrGetDefault(operationContext, dataSourceNode, QUERY_TIMEOUT);
        final Integer xaResourceTimeout = ModelNodeUtil.getIntIfSetOrGetDefault(operationContext, dataSourceNode, XA_RESOURCE_TIMEOUT);
        final Long useTryLock = ModelNodeUtil.getLongIfSetOrGetDefault(operationContext, dataSourceNode, USE_TRY_LOCK);
        final Boolean setTxQueryTimeout = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, SET_TX_QUERY_TIMEOUT);
        final TimeOut timeOut = new TimeOutImpl(blockingTimeoutMillis, idleTimeoutMinutes, allocationRetry,
                allocationRetryWaitMillis, xaResourceTimeout, setTxQueryTimeout, queryTimeout, useTryLock);
        final String transactionIsolationString = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, TRANSACTION_ISOLATION);
        TransactionIsolation transactionIsolation = null;
        if (transactionIsolationString != null) {
            transactionIsolation = TransactionIsolation.forName(transactionIsolationString); // TODO relax case sensitivity
            if (transactionIsolation == null) {
                transactionIsolation = TransactionIsolation.customLevel(transactionIsolationString);
            }
        }
        final String checkValidConnectionSql = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, CHECK_VALID_CONNECTION_SQL);

        final Extension exceptionSorter = ModelNodeUtil.extractExtension(operationContext, dataSourceNode, EXCEPTION_SORTER_CLASSNAME, EXCEPTION_SORTER_PROPERTIES);
        final Extension staleConnectionChecker = ModelNodeUtil.extractExtension(operationContext, dataSourceNode, STALE_CONNECTION_CHECKER_CLASSNAME,
                STALE_CONNECTION_CHECKER_PROPERTIES);
        final Extension validConnectionChecker = ModelNodeUtil.extractExtension(operationContext, dataSourceNode, VALID_CONNECTION_CHECKER_CLASSNAME,
                VALID_CONNECTION_CHECKER_PROPERTIES);

        Long backgroundValidationMillis = ModelNodeUtil.getLongIfSetOrGetDefault(operationContext, dataSourceNode, BACKGROUNDVALIDATIONMILLIS);
        final Boolean backgroundValidation = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, BACKGROUNDVALIDATION);
        boolean useFastFail = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, USE_FAST_FAIL);
        final Boolean validateOnMatch = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, VALIDATE_ON_MATCH);
        final Boolean spy = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, SPY);
        final Boolean useCcm = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, USE_CCM);
        final Validation validation = new ValidationImpl(backgroundValidation, backgroundValidationMillis, useFastFail,
                validConnectionChecker, checkValidConnectionSql, validateOnMatch, staleConnectionChecker, exceptionSorter);

        final String recoveryUsername = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, RECOVERY_USERNAME);
        final String recoveryPassword = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, RECOVERY_PASSWORD);
        final String recoverySecurityDomain = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, RECOVERY_SECURITY_DOMAIN);
        final boolean recoveryElytronEnabled = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, RECOVERY_ELYTRON_ENABLED);
        final String recoveryAuthenticationContext = ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, RECOVERY_AUTHENTICATION_CONTEXT);
        Boolean noRecovery = ModelNodeUtil.getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, NO_RECOVERY);
        final String urlProperty =   ModelNodeUtil.getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, URL_PROPERTY);
        Recovery recovery = null;
        if ((recoveryUsername != null && (recoveryPassword != null || recoveryCredentialSourceSupplier != null)) || recoverySecurityDomain != null || noRecovery != null) {
            Credential credential = null;

            if ((recoveryUsername != null && (recoveryPassword != null || recoveryCredentialSourceSupplier != null)) || recoverySecurityDomain != null)
               credential = new CredentialImpl(recoveryUsername, recoveryPassword,
                       recoveryElytronEnabled? recoveryAuthenticationContext: recoverySecurityDomain, elytronEnabled, recoveryCredentialSourceSupplier);

            Extension recoverPlugin = ModelNodeUtil.extractExtension(operationContext, dataSourceNode, RECOVER_PLUGIN_CLASSNAME, RECOVER_PLUGIN_PROPERTIES);

            if (noRecovery == null)
                noRecovery = Boolean.FALSE;

            recovery = new Recovery(credential, recoverPlugin, noRecovery);
        }
        return new ModifiableXaDataSource(transactionIsolation, timeOut, security, statement, validation, urlDelimiter, urlProperty,
                urlSelectorStrategyClassName, useJavaContext, poolName, enabled, jndiName, spy, useCcm,
                connectable, tracking, mcp, enlistmentTrace, xaDataSourceProperty,
                xaDataSourceClass, module, newConnectionSql, xaPool, recovery);
    }

}
