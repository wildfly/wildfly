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
import static org.jboss.as.connector.pool.Constants.BACKGROUNDVALIDATIONMILLIS;
import static org.jboss.as.connector.pool.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS;
import static org.jboss.as.connector.pool.Constants.IDLETIMEOUTMINUTES;
import static org.jboss.as.connector.pool.Constants.MAX_POOL_SIZE;
import static org.jboss.as.connector.pool.Constants.MIN_POOL_SIZE;
import static org.jboss.as.connector.pool.Constants.POOL_FLUSH_STRATEGY;
import static org.jboss.as.connector.pool.Constants.POOL_PREFILL;
import static org.jboss.as.connector.pool.Constants.POOL_USE_STRICT_MIN;
import static org.jboss.as.connector.pool.Constants.USE_FAST_FAIL;
import static org.jboss.as.connector.subsystems.datasources.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.datasources.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.datasources.Constants.CHECKVALIDCONNECTIONSQL;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_URL;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_CLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_DRIVER;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_CLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.EXCEPTIONSORTERCLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.EXCEPTIONSORTER_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.INTERLEAVING;
import static org.jboss.as.connector.subsystems.datasources.Constants.JNDINAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.JTA;
import static org.jboss.as.connector.subsystems.datasources.Constants.NEW_CONNECTION_SQL;
import static org.jboss.as.connector.subsystems.datasources.Constants.NOTXSEPARATEPOOL;
import static org.jboss.as.connector.subsystems.datasources.Constants.NO_RECOVERY;
import static org.jboss.as.connector.subsystems.datasources.Constants.PAD_XID;
import static org.jboss.as.connector.subsystems.datasources.Constants.PASSWORD;
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
import static org.jboss.as.connector.subsystems.datasources.Constants.TRANSACTION_ISOLATION;
import static org.jboss.as.connector.subsystems.datasources.Constants.URL_DELIMITER;
import static org.jboss.as.connector.subsystems.datasources.Constants.URL_SELECTOR_STRATEGY_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.USERNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.USETRYLOCK;
import static org.jboss.as.connector.subsystems.datasources.Constants.USE_CCM;
import static org.jboss.as.connector.subsystems.datasources.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.connector.subsystems.datasources.Constants.VALIDATEONMATCH;
import static org.jboss.as.connector.subsystems.datasources.Constants.VALIDCONNECTIONCHECKERCLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.VALIDCONNECTIONCHECKER_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.WRAP_XA_RESOURCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.XADATASOURCECLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.XADATASOURCE_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_RESOURCE_TIMEOUT;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.jca.common.api.metadata.Defaults;
import org.jboss.jca.common.api.metadata.common.CommonPool;
import org.jboss.jca.common.api.metadata.common.CommonXaPool;
import org.jboss.jca.common.api.metadata.common.Credential;
import org.jboss.jca.common.api.metadata.common.Extension;
import org.jboss.jca.common.api.metadata.common.FlushStrategy;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.ds.DsSecurity;
import org.jboss.jca.common.api.metadata.ds.Statement;
import org.jboss.jca.common.api.metadata.ds.TimeOut;
import org.jboss.jca.common.api.metadata.ds.TransactionIsolation;
import org.jboss.jca.common.api.metadata.ds.Validation;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.jca.common.metadata.common.CommonPoolImpl;
import org.jboss.jca.common.metadata.common.CommonXaPoolImpl;
import org.jboss.jca.common.metadata.common.CredentialImpl;
import org.jboss.jca.common.metadata.ds.DsSecurityImpl;
import org.jboss.jca.common.metadata.ds.StatementImpl;
import org.jboss.jca.common.metadata.ds.TimeOutImpl;
import org.jboss.jca.common.metadata.ds.ValidationImpl;

/**
 * Utility used to help convert between JCA spi data-source instances and model
 * node representations and vice-versa.
 * @author John Bailey
 */
class DataSourceModelNodeUtil {

    static ModifiableDataSource from(final OperationContext operationContext, final ModelNode dataSourceNode, final String dsName) throws OperationFailedException, ValidateException {
        final Map<String, String> connectionProperties= Collections.emptyMap();

        final String connectionUrl = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, CONNECTION_URL, null);
        final String driverClass = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, DRIVER_CLASS, null);
        final String dataSourceClass = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, DATASOURCE_CLASS, null);
        final String jndiName = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, JNDINAME, null);
        final String driver = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, DATASOURCE_DRIVER, null);
        final String newConnectionSql = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, NEW_CONNECTION_SQL, null);
        final String poolName = dsName;
        final String urlDelimiter = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, URL_DELIMITER, null);
        final String urlSelectorStrategyClassName = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode,
                URL_SELECTOR_STRATEGY_CLASS_NAME, null);
        final boolean useJavaContext = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, USE_JAVA_CONTEXT, Defaults.USE_JAVA_CONTEXT);
        final boolean enabled = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, ENABLED, Defaults.ENABLED);
        final boolean jta = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, JTA, Defaults.JTA);
        final Integer maxPoolSize = getIntIfSetOrGetDefault(operationContext, dataSourceNode, MAX_POOL_SIZE, Defaults.MAX_POOL_SIZE);
        final Integer minPoolSize = getIntIfSetOrGetDefault(operationContext, dataSourceNode, MIN_POOL_SIZE, Defaults.MIN_POOL_SIZE);
        final boolean prefill = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, POOL_PREFILL, Defaults.PREFILL);
        final boolean useStrictMin = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, POOL_USE_STRICT_MIN, Defaults.USE_STRICT_MIN);
        final FlushStrategy flushStrategy = dataSourceNode.hasDefined(POOL_FLUSH_STRATEGY.getName()) ? FlushStrategy.forName(dataSourceNode
                .get(POOL_FLUSH_STRATEGY.getName()).asString()) : Defaults.FLUSH_STRATEGY;

        final CommonPool pool = new CommonPoolImpl(minPoolSize, maxPoolSize, prefill, useStrictMin, flushStrategy);

        final String username =getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, USERNAME, null);

        final String password = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, PASSWORD, null);
        final String securityDomain = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, SECURITY_DOMAIN, null);

        final Extension reauthPlugin = extractExtension(operationContext, dataSourceNode, REAUTHPLUGIN_CLASSNAME, REAUTHPLUGIN_PROPERTIES);

        final DsSecurity security = new DsSecurityImpl(username, password, securityDomain, reauthPlugin);

        final boolean sharePreparedStatements = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, SHAREPREPAREDSTATEMENTS, Defaults.SHARE_PREPARED_STATEMENTS);
        final Long preparedStatementsCacheSize = getLongIfSetOrGetDefault(operationContext, dataSourceNode, PREPAREDSTATEMENTSCACHESIZE, null);
        final Statement.TrackStatementsEnum trackStatements = dataSourceNode.hasDefined(TRACKSTATEMENTS.getName()) ? Statement.TrackStatementsEnum
                .valueOf(dataSourceNode.get(TRACKSTATEMENTS.getName()).asString()) : Defaults.TRACK_STATEMENTS;
        final Statement statement = new StatementImpl(sharePreparedStatements, preparedStatementsCacheSize, trackStatements);

        final Integer allocationRetry = getIntIfSetOrGetDefault(operationContext, dataSourceNode, ALLOCATION_RETRY, null);
        final Long allocationRetryWaitMillis = getLongIfSetOrGetDefault(operationContext, dataSourceNode, ALLOCATION_RETRY_WAIT_MILLIS, null);
        final Long blockingTimeoutMillis = getLongIfSetOrGetDefault(operationContext, dataSourceNode, BLOCKING_TIMEOUT_WAIT_MILLIS, null);
        final Long idleTimeoutMinutes = getLongIfSetOrGetDefault(operationContext, dataSourceNode, IDLETIMEOUTMINUTES, null);
        final Long queryTimeout = getLongIfSetOrGetDefault(operationContext, dataSourceNode, QUERYTIMEOUT, null);
        final Integer xaResourceTimeout = getIntIfSetOrGetDefault(operationContext, dataSourceNode, XA_RESOURCE_TIMEOUT, null);
        final Long useTryLock = getLongIfSetOrGetDefault(operationContext, dataSourceNode, USETRYLOCK, null);
        final boolean setTxQuertTimeout = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, SETTXQUERYTIMEOUT, Defaults.SET_TX_QUERY_TIMEOUT);
        final TimeOut timeOut = new TimeOutImpl(blockingTimeoutMillis, idleTimeoutMinutes, allocationRetry,
                allocationRetryWaitMillis, xaResourceTimeout, setTxQuertTimeout, queryTimeout, useTryLock);
        final TransactionIsolation transactionIsolation = dataSourceNode.hasDefined(TRANSACTION_ISOLATION.getName()) ? TransactionIsolation
                .valueOf(dataSourceNode.get(TRANSACTION_ISOLATION.getName()).asString()) : null;
        final String checkValidConnectionSql = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, CHECKVALIDCONNECTIONSQL, null);

        final Extension exceptionSorter = extractExtension(operationContext, dataSourceNode, EXCEPTIONSORTERCLASSNAME, EXCEPTIONSORTER_PROPERTIES);
        final Extension staleConnectionChecker = extractExtension(operationContext, dataSourceNode, STALECONNECTIONCHECKERCLASSNAME,
                STALECONNECTIONCHECKER_PROPERTIES);
        final Extension validConnectionChecker = extractExtension(operationContext, dataSourceNode, VALIDCONNECTIONCHECKERCLASSNAME,
                VALIDCONNECTIONCHECKER_PROPERTIES);

        Long backgroundValidationMillis = getLongIfSetOrGetDefault(operationContext, dataSourceNode, BACKGROUNDVALIDATIONMILLIS, null);
        final boolean backgroundValidation = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, BACKGROUNDVALIDATION, Defaults.BACKGROUND_VALIDATION);
        boolean useFastFail = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, USE_FAST_FAIL, Defaults.USE_FAST_FAIl);
        final boolean validateOnMatch = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, VALIDATEONMATCH, Defaults.VALIDATE_ON_MATCH);
        final boolean spy = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, SPY, Defaults.SPY);
        final boolean useCcm = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, USE_CCM, Defaults.USE_CCM);

        final Validation validation = new ValidationImpl(backgroundValidation, backgroundValidationMillis, useFastFail,
                validConnectionChecker, checkValidConnectionSql, validateOnMatch, staleConnectionChecker, exceptionSorter);

        return new ModifiableDataSource(connectionUrl, driverClass, dataSourceClass, driver, transactionIsolation, connectionProperties, timeOut,
                security, statement, validation, urlDelimiter, urlSelectorStrategyClassName, newConnectionSql, useJavaContext,
                poolName, enabled, jndiName, spy, useCcm, jta, pool);
    }

    static ModifiableXaDataSource xaFrom(final OperationContext operationContext, final ModelNode dataSourceNode, final String dsName) throws OperationFailedException, ValidateException {
        final Map<String, String> xaDataSourceProperty;
        xaDataSourceProperty = Collections.emptyMap();

        final String xaDataSourceClass = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, XADATASOURCECLASS, null);
        final String jndiName = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, JNDINAME, null);
        final String module = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, DATASOURCE_DRIVER, null);
        final String newConnectionSql = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, NEW_CONNECTION_SQL, null);
        final String poolName = dsName;
        final String urlDelimiter = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, URL_DELIMITER, null);
        final String urlSelectorStrategyClassName = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode,
                URL_SELECTOR_STRATEGY_CLASS_NAME, null);
        final Boolean useJavaContext = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, USE_JAVA_CONTEXT, Defaults.USE_JAVA_CONTEXT);
        final Boolean enabled = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, ENABLED, Defaults.ENABLED);
        final Integer maxPoolSize = getIntIfSetOrGetDefault(operationContext, dataSourceNode, MAX_POOL_SIZE, Defaults.MAX_POOL_SIZE);
        final Integer minPoolSize = getIntIfSetOrGetDefault(operationContext, dataSourceNode, MIN_POOL_SIZE, Defaults.MIN_POOL_SIZE);
        final Boolean prefill = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, POOL_PREFILL, Defaults.PREFILL);
        final Boolean useStrictMin = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, POOL_USE_STRICT_MIN, Defaults.USE_STRICT_MIN);
        final Boolean interleaving = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, INTERLEAVING, Defaults.INTERLEAVING);
        final Boolean noTxSeparatePool = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, NOTXSEPARATEPOOL, Defaults.NO_TX_SEPARATE_POOL);
        final Boolean padXid = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, PAD_XID, Defaults.PAD_XID);
        final Boolean isSameRmOverride = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, SAME_RM_OVERRIDE, Defaults.IS_SAME_RM_OVERRIDE);
        final Boolean wrapXaDataSource = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, WRAP_XA_RESOURCE, Defaults.WRAP_XA_RESOURCE);
        final FlushStrategy flushStrategy = dataSourceNode.hasDefined(POOL_FLUSH_STRATEGY.getName()) ? FlushStrategy.forName(dataSourceNode
                .get(POOL_FLUSH_STRATEGY.getName()).asString()) : Defaults.FLUSH_STRATEGY;

        final CommonXaPool xaPool = new CommonXaPoolImpl(minPoolSize, maxPoolSize, prefill, useStrictMin, flushStrategy,
                isSameRmOverride, interleaving, padXid, wrapXaDataSource, noTxSeparatePool);

        final String username = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, USERNAME, null);
        final String password = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, PASSWORD, null);
        final String securityDomain = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, SECURITY_DOMAIN, null);

        final Extension reauthPlugin = extractExtension(operationContext, dataSourceNode, REAUTHPLUGIN_CLASSNAME, REAUTHPLUGIN_PROPERTIES);

        final DsSecurity security = new DsSecurityImpl(username, password, securityDomain, reauthPlugin);

        final Boolean sharePreparedStatements = dataSourceNode.hasDefined(SHAREPREPAREDSTATEMENTS.getName()) ? dataSourceNode.get(
                SHAREPREPAREDSTATEMENTS.getName()).asBoolean() : Defaults.SHARE_PREPARED_STATEMENTS;
        final Long preparedStatementsCacheSize = getLongIfSetOrGetDefault(operationContext, dataSourceNode, PREPAREDSTATEMENTSCACHESIZE, null);
        final Statement.TrackStatementsEnum trackStatements = dataSourceNode.hasDefined(TRACKSTATEMENTS.getName()) ? Statement.TrackStatementsEnum
                .valueOf(dataSourceNode.get(TRACKSTATEMENTS.getName()).asString()) : Defaults.TRACK_STATEMENTS;
        final Statement statement = new StatementImpl(sharePreparedStatements, preparedStatementsCacheSize, trackStatements);

        final Integer allocationRetry = getIntIfSetOrGetDefault(operationContext, dataSourceNode, ALLOCATION_RETRY, null);
        final Long allocationRetryWaitMillis = getLongIfSetOrGetDefault(operationContext, dataSourceNode, ALLOCATION_RETRY_WAIT_MILLIS, null);
        final Long blockingTimeoutMillis = getLongIfSetOrGetDefault(operationContext, dataSourceNode, BLOCKING_TIMEOUT_WAIT_MILLIS, null);
        final Long idleTimeoutMinutes = getLongIfSetOrGetDefault(operationContext, dataSourceNode, IDLETIMEOUTMINUTES, null);
        final Long queryTimeout = getLongIfSetOrGetDefault(operationContext, dataSourceNode, QUERYTIMEOUT, null);
        final Integer xaResourceTimeout = getIntIfSetOrGetDefault(operationContext, dataSourceNode, XA_RESOURCE_TIMEOUT, null);
        final Long useTryLock = getLongIfSetOrGetDefault(operationContext, dataSourceNode, USETRYLOCK, null);
        final Boolean setTxQuertTimeout = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, SETTXQUERYTIMEOUT, Defaults.SET_TX_QUERY_TIMEOUT);
        final TimeOut timeOut = new TimeOutImpl(blockingTimeoutMillis, idleTimeoutMinutes, allocationRetry,
                allocationRetryWaitMillis, xaResourceTimeout, setTxQuertTimeout, queryTimeout, useTryLock);
        final TransactionIsolation transactionIsolation = dataSourceNode.hasDefined(TRANSACTION_ISOLATION.getName()) ? TransactionIsolation
                .valueOf(dataSourceNode.get(TRANSACTION_ISOLATION.getName()).asString()) : null;
        final String checkValidConnectionSql = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, CHECKVALIDCONNECTIONSQL, null);

        final Extension exceptionSorter = extractExtension(operationContext, dataSourceNode, EXCEPTIONSORTERCLASSNAME, EXCEPTIONSORTER_PROPERTIES);
        final Extension staleConnectionChecker = extractExtension(operationContext, dataSourceNode, STALECONNECTIONCHECKERCLASSNAME,
                STALECONNECTIONCHECKER_PROPERTIES);
        final Extension validConnectionChecker = extractExtension(operationContext, dataSourceNode, VALIDCONNECTIONCHECKERCLASSNAME,
                VALIDCONNECTIONCHECKER_PROPERTIES);

        Long backgroundValidationMillis = getLongIfSetOrGetDefault(operationContext, dataSourceNode, BACKGROUNDVALIDATIONMILLIS, null);
        final Boolean backgroundValidation = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, BACKGROUNDVALIDATION, Defaults.BACKGROUND_VALIDATION);
        boolean useFastFail = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, USE_FAST_FAIL, Defaults.USE_FAST_FAIl);
        final Boolean validateOnMatch = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, VALIDATEONMATCH, Defaults.VALIDATE_ON_MATCH);
        final Boolean spy = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, SPY, Defaults.SPY);
        final Boolean useCcm = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, USE_CCM, Defaults.USE_CCM);
        final Validation validation = new ValidationImpl(backgroundValidation, backgroundValidationMillis, useFastFail,
                validConnectionChecker, checkValidConnectionSql, validateOnMatch, staleConnectionChecker, exceptionSorter);

        final String recoveryUsername = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, RECOVERY_USERNAME, null);
        final String recoveryPassword = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, RECOVERY_PASSWORD, null);
        final String recoverySecurityDomain = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, RECOVERY_SECURITY_DOMAIN, null);
        Boolean noRecovery = getBooleanIfSetOrGetDefault(operationContext, dataSourceNode, NO_RECOVERY, null);

        Recovery recovery = null;
        if ((recoveryUsername != null && recoveryPassword != null) || recoverySecurityDomain != null || noRecovery != null) {
            Credential credential = null;

            if ((recoveryUsername != null && recoveryPassword != null) || recoverySecurityDomain != null)
               credential = new CredentialImpl(recoveryUsername, recoveryPassword, recoverySecurityDomain);

            Extension recoverPlugin = extractExtension(operationContext, dataSourceNode, RECOVERLUGIN_CLASSNAME, RECOVERLUGIN_PROPERTIES);

            if (noRecovery == null)
                noRecovery = Boolean.FALSE;

            recovery = new Recovery(credential, recoverPlugin, noRecovery);
        }
        return new ModifiableXaDataSource(transactionIsolation, timeOut, security, statement, validation, urlDelimiter,
                urlSelectorStrategyClassName, useJavaContext, poolName, enabled, jndiName, spy, useCcm, xaDataSourceProperty,
                xaDataSourceClass, module, newConnectionSql, xaPool, recovery);
    }

    private static Long getLongIfSetOrGetDefault(final OperationContext context, final ModelNode dataSourceNode, final SimpleAttributeDefinition key, final Long defaultValue) throws OperationFailedException {
        if (dataSourceNode.hasDefined(key.getName())) {
            if (key.isAllowExpression()) {
                return context.resolveExpressions(dataSourceNode.get(key.getName())).asLong();
            } else {
                return dataSourceNode.get(key.getName()).asLong();
            }
        } else {
            return defaultValue;
        }
    }

    private static Integer getIntIfSetOrGetDefault(final OperationContext context, final ModelNode dataSourceNode, final SimpleAttributeDefinition key, final Integer defaultValue) throws OperationFailedException {
        if (dataSourceNode.hasDefined(key.getName())) {
            if (key.isAllowExpression()) {
                return context.resolveExpressions(dataSourceNode.get(key.getName())).asInt();
            } else {
                return dataSourceNode.get(key.getName()).asInt();
            }
        } else {
            return defaultValue;
        }
    }

    private static Boolean getBooleanIfSetOrGetDefault(final OperationContext context, final ModelNode dataSourceNode, final SimpleAttributeDefinition key,
            final Boolean defaultValue) throws OperationFailedException {
        if (dataSourceNode.hasDefined(key.getName())) {
            if (key.isAllowExpression()) {
                return context.resolveExpressions(dataSourceNode.get(key.getName())).asBoolean();
            } else {
                return dataSourceNode.get(key.getName()).asBoolean();
            }
        } else {
            return defaultValue;
        }
    }

    private static String getResolvedStringIfSetOrGetDefault(final OperationContext context, final ModelNode dataSourceNode, final SimpleAttributeDefinition key, final String defaultValue) throws OperationFailedException {
        if (dataSourceNode.hasDefined(key.getName())) {
            if (key.isAllowExpression()) {
                return context.resolveExpressions(dataSourceNode.get(key.getName())).asString();
            } else {
                String returnValue = dataSourceNode.get(key.getName()).asString();
                return (returnValue != null && returnValue.trim().length() != 0) ? returnValue : null;
            }
        } else {
            return defaultValue;
        }
    }

    private static Extension extractExtension(final OperationContext operationContext, final ModelNode dataSourceNode, final SimpleAttributeDefinition className, final SimpleAttributeDefinition propertyName)
            throws ValidateException, OperationFailedException {
        if (dataSourceNode.hasDefined(className.getName())) {
            String exceptionSorterClassName = dataSourceNode.get(className.getName()).asString();

            getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, className, null);

            Map<String, String> exceptionSorterProperty = null;
            if (dataSourceNode.hasDefined(propertyName.getName())) {
                exceptionSorterProperty = new HashMap<String, String>(dataSourceNode.get(propertyName.getName()).asList().size());
                for (Property property : dataSourceNode.get(propertyName.getName()).asPropertyList()) {
                    exceptionSorterProperty.put(property.getName(), property.getValue().asString());
                }
            }

            return new Extension(exceptionSorterClassName, exceptionSorterProperty);
        } else {
            return null;
        }
    }
}
