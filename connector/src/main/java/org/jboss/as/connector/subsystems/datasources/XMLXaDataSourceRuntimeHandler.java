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

package org.jboss.as.connector.subsystems.datasources;

import java.util.Map;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.metadata.api.common.Credential;
import org.jboss.as.connector.metadata.api.ds.DsSecurity;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.api.metadata.common.XaPool;
import org.jboss.jca.common.api.metadata.ds.DsXaPool;
import org.jboss.jca.common.api.metadata.ds.XaDataSource;

/**
 * Runtime attribute handler for XA XML datasources
 *
 * @author Stuart Douglas
 */
public class XMLXaDataSourceRuntimeHandler extends AbstractXMLDataSourceRuntimeHandler<XaDataSource> {

    public static final XMLXaDataSourceRuntimeHandler INSTANCE = new XMLXaDataSourceRuntimeHandler();

    @Override
    protected void executeReadAttribute(final String attributeName, final OperationContext context, final XaDataSource dataSource, final PathAddress address) {

        final String target = address.getLastElement().getKey();
        if (target.equals(XA_DATASOURCE_PROPERTIES)) {
            handlePropertyAttribute(attributeName, context, dataSource, address.getLastElement().getValue());
        } else if (target.equals(XA_DATA_SOURCE)) {
            handleDatasourceAttribute(attributeName, context, dataSource);
        }
    }

    private void handlePropertyAttribute(final String attributeName, final OperationContext context, final XaDataSource dataSource, final String propName) {
        if (attributeName.equals(ModelDescriptionConstants.VALUE)) {
            setStringIfNotNull(context, dataSource.getXaDataSourceProperty().get(propName));
        } else {
            throw ConnectorLogger.ROOT_LOGGER.unknownAttribute(attributeName);
        }
    }

    private void handleDatasourceAttribute(final String attributeName, final OperationContext context, final XaDataSource dataSource) {
        if (attributeName.equals(Constants.XA_DATASOURCE_CLASS.getName())) {
            setStringIfNotNull(context, dataSource.getXaDataSourceClass());
        } else if (attributeName.equals(Constants.JNDI_NAME.getName())) {
            setStringIfNotNull(context, dataSource.getJndiName());
        } else if (attributeName.equals(Constants.DATASOURCE_DRIVER.getName())) {
            setStringIfNotNull(context, dataSource.getDriver());
        } else if (attributeName.equals(Constants.NEW_CONNECTION_SQL.getName())) {
            setStringIfNotNull(context, dataSource.getNewConnectionSql());
        } else if (attributeName.equals(Constants.URL_DELIMITER.getName())) {
            setStringIfNotNull(context, dataSource.getUrlDelimiter());
        } else if (attributeName.equals(Constants.URL_PROPERTY.getName())) {
            setStringIfNotNull(context, dataSource.getUrlProperty());
        } else if (attributeName.equals(Constants.URL_SELECTOR_STRATEGY_CLASS_NAME.getName())) {
            setStringIfNotNull(context, dataSource.getUrlSelectorStrategyClassName());
        } else if (attributeName.equals(Constants.USE_JAVA_CONTEXT.getName())) {
            setBooleanIfNotNull(context, dataSource.isUseJavaContext());
        } else if (attributeName.equals(Constants.ENABLED.getName())) {
            setBooleanIfNotNull(context, dataSource.isEnabled());
        } else if (attributeName.equals(org.jboss.as.connector.subsystems.common.pool.Constants.MAX_POOL_SIZE.getName())) {
            if (dataSource.getXaPool() == null) {
                return;
            }
            setIntIfNotNull(context, dataSource.getXaPool().getMaxPoolSize());
        } else if (attributeName.equals(org.jboss.as.connector.subsystems.common.pool.Constants.INITIAL_POOL_SIZE.getName())) {
            if (dataSource.getXaPool() == null) {
                return;
            }
            setIntIfNotNull(context, dataSource.getXaPool().getInitialPoolSize());
        } else if (attributeName.equals(org.jboss.as.connector.subsystems.common.pool.Constants.MIN_POOL_SIZE.getName())) {
            if (dataSource.getXaPool() == null) {
                return;
            }
            setIntIfNotNull(context, dataSource.getXaPool().getMinPoolSize());
        } else if (attributeName.equals(org.jboss.as.connector.subsystems.common.pool.Constants.POOL_PREFILL.getName())) {
            if (dataSource.getXaPool() == null) {
                return;
            }
            setBooleanIfNotNull(context, dataSource.getXaPool().isPrefill());
        } else if (attributeName.equals(org.jboss.as.connector.subsystems.common.pool.Constants.POOL_FAIR.getName())) {
            if (dataSource.getXaPool() == null) {
                return;
            }
            setBooleanIfNotNull(context, dataSource.getXaPool().isFair());
        } else if (attributeName.equals(org.jboss.as.connector.subsystems.common.pool.Constants.POOL_USE_STRICT_MIN.getName())) {
            if (dataSource.getXaPool() == null) {
                return;
            }
            setBooleanIfNotNull(context, dataSource.getXaPool().isUseStrictMin());
        } else if (attributeName.equals(org.jboss.as.connector.subsystems.common.pool.Constants.POOL_FLUSH_STRATEGY.getName())) {
            if (dataSource.getXaPool() == null) {
                return;
            }
            if (dataSource.getXaPool().getFlushStrategy() == null) {
                return;
            }
            setStringIfNotNull(context, dataSource.getXaPool().getFlushStrategy().getName());

        } else if (attributeName.equals(org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_INCREMENTER_CLASS.getName())) {
            if (dataSource.getXaPool() == null || dataSource.getXaPool().getCapacity() == null || dataSource.getXaPool().getCapacity().getIncrementer() == null) {
                return;
            }
            setStringIfNotNull(context, dataSource.getXaPool().getCapacity().getIncrementer().getClassName());
        } else if (attributeName.equals(org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_DECREMENTER_CLASS.getName())) {
            if (dataSource.getXaPool() == null || dataSource.getXaPool().getCapacity() == null || dataSource.getXaPool().getCapacity().getDecrementer() == null) {
                return;
            }
            setStringIfNotNull(context, dataSource.getXaPool().getCapacity().getDecrementer().getClassName());

        } else if (attributeName.equals(org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_INCREMENTER_PROPERTIES.getName())) {
            XaPool pool = dataSource.getXaPool();
            if (pool == null || pool.getCapacity() == null || pool.getCapacity().getIncrementer() == null)
                return;

            final Map<String, String> propertiesMap = pool.getCapacity().getIncrementer().getConfigPropertiesMap();
            if (propertiesMap == null) {
                return;
            }
            for (final Map.Entry<String, String> entry : propertiesMap.entrySet()) {
                context.getResult().asPropertyList().add(new ModelNode().set(entry.getKey(), entry.getValue()).asProperty());
            }


        } else if (attributeName.equals(org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_DECREMENTER_PROPERTIES.getName())) {
            XaPool pool = dataSource.getXaPool();
            if (pool == null || pool.getCapacity() == null || pool.getCapacity().getDecrementer() == null)
                return;

            final Map<String, String> propertiesMap = pool.getCapacity().getDecrementer().getConfigPropertiesMap();
            if (propertiesMap == null) {
                return;
            }
            for (final Map.Entry<String, String> entry : propertiesMap.entrySet()) {
                context.getResult().asPropertyList().add(new ModelNode().set(entry.getKey(), entry.getValue()).asProperty());
            }

        }
        else if (attributeName.equals(Constants.INTERLEAVING.getName())) {
            if(dataSource.getXaPool() == null) {
                return;
            }
            setBooleanIfNotNull(context, dataSource.getXaPool().isInterleaving());
        } else if (attributeName.equals(Constants.NO_TX_SEPARATE_POOL.getName())) {
            if(dataSource.getXaPool() == null) {
                return;
            }
            setBooleanIfNotNull(context, dataSource.getXaPool().isNoTxSeparatePool());
        } else if (attributeName.equals(Constants.PAD_XID.getName())) {
            if(dataSource.getXaPool() == null) {
                return;
            }
            setBooleanIfNotNull(context, dataSource.getXaPool().isPadXid());
        } else if (attributeName.equals(Constants.SAME_RM_OVERRIDE.getName())) {
            if(dataSource.getXaPool() == null) {
                return;
            }
            setBooleanIfNotNull(context, dataSource.getXaPool().isSameRmOverride());
        } else if (attributeName.equals(Constants.WRAP_XA_RESOURCE.getName())) {
            if(dataSource.getXaPool() == null) {
                return;
            }
            setBooleanIfNotNull(context, dataSource.getXaPool().isWrapXaResource());
        } else if (attributeName.equals(Constants.PREPARED_STATEMENTS_CACHE_SIZE.getName())) {
            if (dataSource.getStatement() == null) {
                return;
            }
            setLongIfNotNull(context, dataSource.getStatement().getPreparedStatementsCacheSize());
        } else if (attributeName.equals(Constants.SHARE_PREPARED_STATEMENTS.getName())) {
            if(dataSource.getStatement() == null) {
                return;
            }
            setBooleanIfNotNull(context, dataSource.getStatement().isSharePreparedStatements());
        } else if (attributeName.equals(Constants.TRACK_STATEMENTS.getName())) {
            if(dataSource.getStatement() == null) {
                return;
            }
            if(dataSource.getStatement().getTrackStatements() == null) {
                return;
            }
            setStringIfNotNull(context, dataSource.getStatement().getTrackStatements().name());
        } else if (attributeName.equals(Constants.ALLOCATION_RETRY.getName())) {
            if(dataSource.getTimeOut() == null) {
                return;
            }
            setIntIfNotNull(context, dataSource.getTimeOut().getAllocationRetry());
        } else if (attributeName.equals(Constants.ALLOCATION_RETRY_WAIT_MILLIS.getName())) {
            if(dataSource.getTimeOut() == null) {
                return;
            }
            setLongIfNotNull(context, dataSource.getTimeOut().getAllocationRetryWaitMillis());
        } else if (attributeName.equals(org.jboss.as.connector.subsystems.common.pool.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS.getName())) {
            if(dataSource.getTimeOut() == null) {
                return;
            }
            setLongIfNotNull(context, dataSource.getTimeOut().getBlockingTimeoutMillis());
        } else if (attributeName.equals(org.jboss.as.connector.subsystems.common.pool.Constants.IDLETIMEOUTMINUTES.getName())) {
            if(dataSource.getTimeOut() == null) {
                return;
            }
            setLongIfNotNull(context, dataSource.getTimeOut().getIdleTimeoutMinutes());
        } else if (attributeName.equals(Constants.XA_RESOURCE_TIMEOUT.getName())) {
            if(dataSource.getTimeOut() == null) {
                return;
            }
            setIntIfNotNull(context, dataSource.getTimeOut().getXaResourceTimeout());
        } else if (attributeName.equals(Constants.RECOVERY_USERNAME.getName())) {
            if(dataSource.getRecovery() == null) {
                return;
            }
            if(dataSource.getRecovery().getCredential() == null) {
                return;
            }
            setStringIfNotNull(context, dataSource.getRecovery().getCredential().getUserName());
        } else if (attributeName.equals(Constants.RECOVERY_PASSWORD.getName())) {
            //don't display the password
        } else if (attributeName.equals(Constants.RECOVERY_SECURITY_DOMAIN.getName())) {
            if(dataSource.getRecovery() == null) {
                return;
            }
            if(dataSource.getRecovery().getCredential() == null) {
                return;
            }
            // safe assertion because all parsers create jboss Credential
            assert dataSource.getRecovery().getCredential() instanceof Credential;
            if (((Credential) dataSource.getRecovery().getCredential()).isElytronEnabled()) {
                return;
            }
            setStringIfNotNull(context, dataSource.getRecovery().getCredential().getSecurityDomain());

        } else if (attributeName.equals(Constants.RECOVERY_ELYTRON_ENABLED.getName())) {
            if(dataSource.getRecovery() == null) {
                return;
            }
            if(dataSource.getRecovery().getCredential() == null) {
                return;
            }
            // safe assertion because all parsers create jboss Credential
            assert dataSource.getRecovery().getCredential() instanceof Credential;
            if (!((Credential) dataSource.getRecovery().getCredential()).isElytronEnabled()) {
                return;
            }
            setBooleanIfNotNull(context, ((Credential) dataSource.getRecovery().getCredential()).isElytronEnabled());
        } else if (attributeName.equals(Constants.RECOVERY_CREDENTIAL_REFERENCE.getName())) {
            //don't give out the credential-reference
        } else if (attributeName.equals(Constants.RECOVERY_AUTHENTICATION_CONTEXT.getName())) {
            if(dataSource.getRecovery() == null) {
                return;
            }
            if(dataSource.getRecovery().getCredential() == null) {
                return;
            }
            // safe assertion because all parsers create jboss Credential
            assert dataSource.getRecovery().getCredential() instanceof Credential;
            if (!((Credential) dataSource.getRecovery().getCredential()).isElytronEnabled()) {
                return;
            }
            setStringIfNotNull(context, dataSource.getRecovery().getCredential().getSecurityDomain());

        } else if (attributeName.equals(Constants.RECOVER_PLUGIN_CLASSNAME.getName())) {
            if(dataSource.getRecovery() == null) {
                return;
            }
            if(dataSource.getRecovery().getRecoverPlugin() == null) {
                return;
            }
            setStringIfNotNull(context, dataSource.getRecovery().getRecoverPlugin().getClassName());
        } else if (attributeName.equals(Constants.RECOVER_PLUGIN_PROPERTIES.getName())) {
            if(dataSource.getRecovery() == null) {
                return;
            }
            if(dataSource.getRecovery().getRecoverPlugin() == null) {
                return;
            }

            final Map<String, String> propertiesMap = dataSource.getRecovery().getRecoverPlugin().getConfigPropertiesMap();
            if (propertiesMap == null) {
                return;
            }
            for (final Map.Entry<String, String> entry : propertiesMap.entrySet()) {
                context.getResult().asPropertyList().add(new ModelNode().set(entry.getKey(), entry.getValue()).asProperty());
            }
        } else if (attributeName.equals(Constants.NO_RECOVERY.getName())) {
            if(dataSource.getRecovery() == null) {
                return;
            }
            setBooleanIfNotNull(context, dataSource.getRecovery().getNoRecovery());
        } else if (attributeName.equals(Constants.CHECK_VALID_CONNECTION_SQL.getName())) {
            if (dataSource.getValidation() == null) {
                return;
            }
            setStringIfNotNull(context, dataSource.getValidation().getCheckValidConnectionSql());
        } else if (attributeName.equals(Constants.EXCEPTION_SORTER_CLASSNAME.getName())) {
            if (dataSource.getValidation() == null) {
                return;
            }
            if (dataSource.getValidation().getExceptionSorter() == null) {
                return;
            }
            setStringIfNotNull(context, dataSource.getValidation().getExceptionSorter().getClassName());
        } else if (attributeName.equals(Constants.EXCEPTION_SORTER_PROPERTIES.getName())) {
            if (dataSource.getValidation() == null) {
                return;
            }
            if (dataSource.getValidation().getExceptionSorter() == null) {
                return;
            }
            final Map<String, String> propertiesMap = dataSource.getValidation().getExceptionSorter().getConfigPropertiesMap();
            if (propertiesMap == null) {
                return;
            }
            for (final Map.Entry<String, String> entry : propertiesMap.entrySet()) {
                context.getResult().asPropertyList().add(new ModelNode().set(entry.getKey(), entry.getValue()).asProperty());
            }
        } else if (attributeName.equals(Constants.STALE_CONNECTION_CHECKER_CLASSNAME.getName())) {
            if (dataSource.getValidation() == null) {
                return;
            }
            if (dataSource.getValidation().getStaleConnectionChecker() == null) {
                return;
            }
            setStringIfNotNull(context, dataSource.getValidation().getStaleConnectionChecker().getClassName());
        } else if (attributeName.equals(Constants.STALE_CONNECTION_CHECKER_PROPERTIES.getName())) {
            if (dataSource.getValidation() == null) {
                return;
            }
            if (dataSource.getValidation().getStaleConnectionChecker() == null) {
                return;
            }
            final Map<String, String> propertiesMap = dataSource.getValidation().getStaleConnectionChecker().getConfigPropertiesMap();
            if (propertiesMap == null) {
                return;
            }
            for (final Map.Entry<String, String> entry : propertiesMap.entrySet()) {
                context.getResult().asPropertyList().add(new ModelNode().set(entry.getKey(), entry.getValue()).asProperty());
            }
        } else if (attributeName.equals(Constants.VALID_CONNECTION_CHECKER_CLASSNAME.getName())) {
            if (dataSource.getValidation() == null) {
                return;
            }
            if (dataSource.getValidation().getValidConnectionChecker() == null) {
                return;
            }
            setStringIfNotNull(context, dataSource.getValidation().getValidConnectionChecker().getClassName());
        } else if (attributeName.equals(Constants.VALID_CONNECTION_CHECKER_PROPERTIES.getName())) {
            if (dataSource.getValidation() == null) {
                return;
            }
            if (dataSource.getValidation().getValidConnectionChecker() == null) {
                return;
            }
            final Map<String, String> propertiesMap = dataSource.getValidation().getValidConnectionChecker().getConfigPropertiesMap();
            if (propertiesMap == null) {
                return;
            }
            for (final Map.Entry<String, String> entry : propertiesMap.entrySet()) {
                context.getResult().asPropertyList().add(new ModelNode().set(entry.getKey(), entry.getValue()).asProperty());
            }
        } else if (attributeName.equals(org.jboss.as.connector.subsystems.common.pool.Constants.BACKGROUNDVALIDATIONMILLIS.getName())) {
            if (dataSource.getValidation() == null) {
                return;
            }
            setLongIfNotNull(context, dataSource.getValidation().getBackgroundValidationMillis());
        } else if (attributeName.equals(org.jboss.as.connector.subsystems.common.pool.Constants.BACKGROUNDVALIDATION.getName())) {
            if (dataSource.getValidation() == null) {
                return;
            }
            setBooleanIfNotNull(context, dataSource.getValidation().isBackgroundValidation());
        } else if (attributeName.equals(org.jboss.as.connector.subsystems.common.pool.Constants.USE_FAST_FAIL.getName())) {
            if (dataSource.getValidation() == null) {
                return;
            }
            setBooleanIfNotNull(context, dataSource.getValidation().isUseFastFail());
        } else if (attributeName.equals(Constants.VALIDATE_ON_MATCH.getName())) {
            if (dataSource.getValidation() == null) {
                return;
            }
            setBooleanIfNotNull(context, dataSource.getValidation().isValidateOnMatch());
        } else if (attributeName.equals(Constants.USERNAME.getName())) {
            if (dataSource.getSecurity() == null) {
                return;
            }
            setStringIfNotNull(context, dataSource.getSecurity().getUserName());
        } else if (attributeName.equals(Constants.PASSWORD.getName())) {
            //don't give out the password
        } else if (attributeName.equals(Constants.CREDENTIAL_REFERENCE.getName())) {
            //don't give out the credential-reference
        } else if (attributeName.equals(Constants.SECURITY_DOMAIN.getName())) {
            if (dataSource.getSecurity() == null) {
                return;
            }
            // this is a safe assert because DsXmlParser will always create a wildfly DsSecurity metadata
            assert dataSource.getSecurity() instanceof DsSecurity;
            if (((DsSecurity) dataSource.getSecurity()).isElytronEnabled()) {
                return;
            }
            setStringIfNotNull(context, dataSource.getSecurity().getSecurityDomain());
        } else if (attributeName.equals(Constants.ELYTRON_ENABLED.getName())) {
            if (dataSource.getSecurity() == null) {
                return;
            }
            // this is a safe assert because DsXmlParser will always create a wildfly DsSecurity metadata
            assert dataSource.getSecurity() instanceof DsSecurity;
            if (!((DsSecurity) dataSource.getSecurity()).isElytronEnabled()) {
                return;
            }
            setBooleanIfNotNull(context, ((DsSecurity) dataSource.getSecurity()).isElytronEnabled());
        } else if (attributeName.equals(Constants.AUTHENTICATION_CONTEXT.getName())) {
            if (dataSource.getSecurity() == null) {
                return;
            }
            // this is a safe assert because DsXmlParser will always create a wildfly DsSecurity metadata
            assert dataSource.getSecurity() instanceof DsSecurity;
            if (!((DsSecurity) dataSource.getSecurity()).isElytronEnabled()) {
                return;
            }
            setStringIfNotNull(context, dataSource.getSecurity().getSecurityDomain());
        } else if (attributeName.equals(Constants.REAUTH_PLUGIN_CLASSNAME.getName())) {
            if (dataSource.getSecurity() == null) {
                return;
            }
            if (dataSource.getSecurity().getReauthPlugin() == null) {
                return;
            }
            setStringIfNotNull(context, dataSource.getSecurity().getReauthPlugin().getClassName());
        } else if (attributeName.equals(Constants.REAUTHPLUGIN_PROPERTIES.getName())) {
            if (dataSource.getSecurity() == null) {
                return;
            }
            if (dataSource.getSecurity().getReauthPlugin() == null) {
                return;
            }
            final Map<String, String> propertiesMap = dataSource.getSecurity().getReauthPlugin().getConfigPropertiesMap();
            if (propertiesMap == null) {
                return;
            }
            for (final Map.Entry<String, String> entry : propertiesMap.entrySet()) {
                context.getResult().asPropertyList().add(new ModelNode().set(entry.getKey(), entry.getValue()).asProperty());
            }
        } else if (attributeName.equals(Constants.PREPARED_STATEMENTS_CACHE_SIZE.getName())) {
            if (dataSource.getStatement() == null) {
                return;
            }
            setLongIfNotNull(context, dataSource.getStatement().getPreparedStatementsCacheSize());
        } else if (attributeName.equals(Constants.SHARE_PREPARED_STATEMENTS.getName())) {
            if (dataSource.getStatement() == null) {
                return;
            }
            setBooleanIfNotNull(context, dataSource.getStatement().isSharePreparedStatements());
        } else if (attributeName.equals(Constants.TRACK_STATEMENTS.getName())) {
            if (dataSource.getStatement() == null) {
                return;
            }
            if (dataSource.getStatement().getTrackStatements() == null) {
                return;
            }
            setStringIfNotNull(context, dataSource.getStatement().getTrackStatements().name());
        } else if (attributeName.equals(Constants.ALLOCATION_RETRY.getName())) {
            if (dataSource.getTimeOut() == null) {
                return;
            }
            setIntIfNotNull(context, dataSource.getTimeOut().getAllocationRetry());
        } else if (attributeName.equals(Constants.ALLOCATION_RETRY_WAIT_MILLIS.getName())) {
            if (dataSource.getTimeOut() == null) {
                return;
            }
            setLongIfNotNull(context, dataSource.getTimeOut().getAllocationRetryWaitMillis());
        } else if (attributeName.equals(org.jboss.as.connector.subsystems.common.pool.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS.getName())) {
            if (dataSource.getTimeOut() == null) {
                return;
            }
            setLongIfNotNull(context, dataSource.getTimeOut().getBlockingTimeoutMillis());
        } else if (attributeName.equals(org.jboss.as.connector.subsystems.common.pool.Constants.IDLETIMEOUTMINUTES.getName())) {
            if (dataSource.getTimeOut() == null) {
                return;
            }
            setLongIfNotNull(context, dataSource.getTimeOut().getIdleTimeoutMinutes());
        } else if (attributeName.equals(Constants.QUERY_TIMEOUT.getName())) {
            if (dataSource.getTimeOut() == null) {
                return;
            }
            setLongIfNotNull(context, dataSource.getTimeOut().getQueryTimeout());
        } else if (attributeName.equals(Constants.USE_TRY_LOCK.getName())) {
            if (dataSource.getTimeOut() == null) {
                return;
            }
            setLongIfNotNull(context, dataSource.getTimeOut().getUseTryLock());
        } else if (attributeName.equals(Constants.SET_TX_QUERY_TIMEOUT.getName())) {
            if (dataSource.getTimeOut() == null) {
                return;
            }
            setBooleanIfNotNull(context, dataSource.getTimeOut().isSetTxQueryTimeout());
        } else if (attributeName.equals(Constants.TRANSACTION_ISOLATION.getName())) {
            if (dataSource.getTransactionIsolation() == null) {
                return;
            }
            setStringIfNotNull(context, dataSource.getTransactionIsolation().name());
        } else if (attributeName.equals(Constants.SPY.getName())) {
            setBooleanIfNotNull(context, dataSource.isSpy());
        } else if (attributeName.equals(Constants.USE_CCM.getName())) {
            setBooleanIfNotNull(context, dataSource.isUseCcm());
        } else if (attributeName.equals(Constants.JTA.getName())) {
            setBooleanIfNotNull(context, true);
        } else if (attributeName.equals(Constants.ALLOW_MULTIPLE_USERS.getName())) {
            XaPool pool = dataSource.getXaPool();
            if (!(pool instanceof DsXaPool)) {
                return;
            }
            setBooleanIfNotNull(context, ((DsXaPool) pool).isAllowMultipleUsers());
        } else if (attributeName.equals(Constants.CONNECTION_LISTENER_CLASS.getName())) {
            XaPool pool = dataSource.getXaPool();
            if (!(pool instanceof DsXaPool) || ((DsXaPool) pool).getConnectionListener() == null) {
                return;
            }
            setStringIfNotNull(context, ((DsXaPool) pool).getConnectionListener().getClassName());

        } else if (attributeName.equals(Constants.CONNECTION_LISTENER_PROPERTIES.getName())) {
            XaPool pool = dataSource.getXaPool();
            if (!(pool instanceof DsXaPool) || ((DsXaPool) pool).getConnectionListener() == null) {
                return;
            }
            final Map<String, String> propertiesMap = ((DsXaPool) pool).getConnectionListener().getConfigPropertiesMap();
            if (propertiesMap == null) {
                return;
            }
            for (final Map.Entry<String, String> entry : propertiesMap.entrySet()) {
                context.getResult().asPropertyList().add(new ModelNode().set(entry.getKey(), entry.getValue()).asProperty());
            }
        } else if (attributeName.equals(Constants.CONNECTABLE.getName())) {
            //Just set to false
            context.getResult().set(false);
        } else if (attributeName.equals(Constants.STATISTICS_ENABLED.getName())) {
            //Just set to false
            context.getResult().set(false);

        } else if (attributeName.equals(Constants.TRACKING.getName())) {
            //Just return w/o setting a result
            return;
        } else if (attributeName.equals(Constants.MCP.getName())) {
            //Just return w/o setting a result
            return;
        } else if (attributeName.equals(Constants.ENLISTMENT_TRACE.getName())) {
            //Just return w/o setting a result
            return;
        } else {
            throw ConnectorLogger.ROOT_LOGGER.unknownAttribute(attributeName);
        }

    }

}
