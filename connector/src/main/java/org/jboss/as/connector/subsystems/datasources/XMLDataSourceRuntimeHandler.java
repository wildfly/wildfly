package org.jboss.as.connector.subsystems.datasources;

import java.util.Map;

import org.jboss.as.connector.logging.ConnectorMessages;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.api.metadata.common.CommonPool;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.metadata.ds.v11.DsPool;

/**
 * Runtime attribute handler for XML datasources
 *
 * @author Stuart Douglas
 */
public class XMLDataSourceRuntimeHandler extends AbstractXMLDataSourceRuntimeHandler<DataSource> {

    public static final XMLDataSourceRuntimeHandler INSTANCE = new XMLDataSourceRuntimeHandler();

    @Override
    protected void executeReadAttribute(final String attributeName, final OperationContext context, final DataSource dataSource, final PathAddress address) {

        final String target = address.getLastElement().getKey();
        if(target.equals(CONNECTION_PROPERTIES)) {
            handlePropertyAttribute(attributeName, context, dataSource, address.getLastElement().getValue());
        } else if(target.equals(DATA_SOURCE)) {
            handleDatasourceAttribute(attributeName, context, dataSource);
        }
    }

    private void handlePropertyAttribute(final String attributeName, final OperationContext context, final DataSource dataSource, final String propName) {
        if(attributeName.equals(ModelDescriptionConstants.VALUE)) {
            setStringIfNotNull(context, dataSource.getConnectionProperties().get(propName));
        } else {
            throw ConnectorMessages.MESSAGES.unknownAttribute(attributeName);
        }
    }

    private void handleDatasourceAttribute(final String attributeName, final OperationContext context, final DataSource dataSource) {
        if (attributeName.equals(Constants.CONNECTION_URL.getName())) {
            setStringIfNotNull(context, dataSource.getConnectionUrl());
        } else if (attributeName.equals(Constants.DRIVER_CLASS.getName())) {
            setStringIfNotNull(context, dataSource.getDriverClass());
        } else if (attributeName.equals(Constants.DATASOURCE_CLASS.getName())) {
            setStringIfNotNull(context, dataSource.getDataSourceClass());
        } else if (attributeName.equals(Constants.JNDINAME.getName())) {
            setStringIfNotNull(context, dataSource.getJndiName());
        } else if (attributeName.equals(Constants.DATASOURCE_DRIVER.getName())) {
            setStringIfNotNull(context, dataSource.getDriver());
        } else if (attributeName.equals(Constants.NEW_CONNECTION_SQL.getName())) {
            setStringIfNotNull(context, dataSource.getNewConnectionSql());
        } else if (attributeName.equals(Constants.URL_DELIMITER.getName())) {
            setStringIfNotNull(context, dataSource.getUrlDelimiter());
        } else if (attributeName.equals(Constants.URL_SELECTOR_STRATEGY_CLASS_NAME.getName())) {
            setStringIfNotNull(context, dataSource.getUrlSelectorStrategyClassName());
        } else if (attributeName.equals(Constants.USE_JAVA_CONTEXT.getName())) {
            setBooleanIfNotNull(context, dataSource.isUseJavaContext());
        } else if (attributeName.equals(Constants.JTA.getName())) {
            setBooleanIfNotNull(context, dataSource.isJTA());
        } else if (attributeName.equals(org.jboss.as.connector.subsystems.common.pool.Constants.MAX_POOL_SIZE.getName())) {
            if (dataSource.getPool() == null) {
                return;
            }
            setIntIfNotNull(context, dataSource.getPool().getMaxPoolSize());
        } else if (attributeName.equals(org.jboss.as.connector.subsystems.common.pool.Constants.MIN_POOL_SIZE.getName())) {
            if (dataSource.getPool() == null) {
                return;
            }
            setIntIfNotNull(context, dataSource.getPool().getMinPoolSize());
        } else if (attributeName.equals(org.jboss.as.connector.subsystems.common.pool.Constants.POOL_PREFILL.getName())) {
            if (dataSource.getPool() == null) {
                return;
            }
            setBooleanIfNotNull(context, dataSource.getPool().isPrefill());
        } else if (attributeName.equals(org.jboss.as.connector.subsystems.common.pool.Constants.POOL_USE_STRICT_MIN.getName())) {
            if (dataSource.getPool() == null) {
                return;
            }
            setBooleanIfNotNull(context, dataSource.getPool().isUseStrictMin());
        } else if (attributeName.equals(Constants.USERNAME.getName())) {
            if (dataSource.getSecurity() == null) {
                return;
            }
            setStringIfNotNull(context, dataSource.getSecurity().getUserName());
        } else if (attributeName.equals(Constants.PASSWORD.getName())) {
            //don't give out the password
        } else if (attributeName.equals(Constants.SECURITY_DOMAIN.getName())) {
            if (dataSource.getSecurity() == null) {
                return;
            }
            setStringIfNotNull(context, dataSource.getSecurity().getSecurityDomain());
        } else if (attributeName.equals(Constants.REAUTHPLUGIN_CLASSNAME.getName())) {
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
        } else if (attributeName.equals(org.jboss.as.connector.subsystems.common.pool.Constants.POOL_FLUSH_STRATEGY.getName())) {
            if (dataSource.getPool() == null) {
                return;
            }
            setStringIfNotNull(context, dataSource.getPool().getFlushStrategy().getName());
        } else if (attributeName.equals(Constants.PREPAREDSTATEMENTSCACHESIZE.getName())) {
            if (dataSource.getStatement() == null) {
                return;
            }
            setLongIfNotNull(context, dataSource.getStatement().getPreparedStatementsCacheSize());
        } else if (attributeName.equals(Constants.SHAREPREPAREDSTATEMENTS.getName())) {
            if (dataSource.getStatement() == null) {
                return;
            }
            setBooleanIfNotNull(context, dataSource.getStatement().isSharePreparedStatements());
        } else if (attributeName.equals(Constants.TRACKSTATEMENTS.getName())) {
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
        } else if (attributeName.equals(Constants.QUERYTIMEOUT.getName())) {
            if (dataSource.getTimeOut() == null) {
                return;
            }
            setLongIfNotNull(context, dataSource.getTimeOut().getQueryTimeout());
        } else if (attributeName.equals(Constants.USETRYLOCK.getName())) {
            if (dataSource.getTimeOut() == null) {
                return;
            }
            setLongIfNotNull(context, dataSource.getTimeOut().getUseTryLock());
        } else if (attributeName.equals(Constants.SETTXQUERYTIMEOUT.getName())) {
            if (dataSource.getTimeOut() == null) {
                return;
            }
            setBooleanIfNotNull(context, dataSource.getTimeOut().isSetTxQueryTimeout());
        } else if (attributeName.equals(Constants.TRANSACTION_ISOLATION.getName())) {
            if (dataSource.getTransactionIsolation() == null) {
                return;
            }
            setStringIfNotNull(context, dataSource.getTransactionIsolation().name());
        } else if (attributeName.equals(Constants.CHECKVALIDCONNECTIONSQL.getName())) {
            if (dataSource.getValidation() == null) {
                return;
            }
            setStringIfNotNull(context, dataSource.getValidation().getCheckValidConnectionSql());
        } else if (attributeName.equals(Constants.EXCEPTIONSORTERCLASSNAME.getName())) {
            if (dataSource.getValidation() == null) {
                return;
            }
            if (dataSource.getValidation().getExceptionSorter() == null) {
                return;
            }
            setStringIfNotNull(context, dataSource.getValidation().getExceptionSorter().getClassName());
        } else if (attributeName.equals(Constants.EXCEPTIONSORTER_PROPERTIES.getName())) {
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
        } else if (attributeName.equals(Constants.STALECONNECTIONCHECKERCLASSNAME.getName())) {
            if (dataSource.getValidation() == null) {
                return;
            }
            if (dataSource.getValidation().getStaleConnectionChecker() == null) {
                return;
            }
            setStringIfNotNull(context, dataSource.getValidation().getStaleConnectionChecker().getClassName());
        } else if (attributeName.equals(Constants.STALECONNECTIONCHECKER_PROPERTIES.getName())) {
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
        } else if (attributeName.equals(Constants.VALIDCONNECTIONCHECKERCLASSNAME.getName())) {
            if (dataSource.getValidation() == null) {
                return;
            }
            if (dataSource.getValidation().getValidConnectionChecker() == null) {
                return;
            }
            setStringIfNotNull(context, dataSource.getValidation().getValidConnectionChecker().getClassName());
        } else if (attributeName.equals(Constants.VALIDCONNECTIONCHECKER_PROPERTIES.getName())) {
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
        } else if (attributeName.equals(Constants.VALIDATEONMATCH.getName())) {
            if (dataSource.getValidation() == null) {
                return;
            }
            setBooleanIfNotNull(context, dataSource.getValidation().isValidateOnMatch());
        } else if (attributeName.equals(Constants.SPY.getName())) {
            setBooleanIfNotNull(context, dataSource.isSpy());
        } else if (attributeName.equals(Constants.USE_CCM.getName())) {
            setBooleanIfNotNull(context, dataSource.isUseCcm());
        } else if (attributeName.equals(Constants.ALLOW_MULTIPLE_USERS.getName())) {
            CommonPool pool = dataSource.getPool();
            if (!(pool instanceof DsPool)) {
                return;
            }
            setBooleanIfNotNull(context, ((DsPool) pool).isAllowMultipleUsers());
        } else {
            throw ConnectorMessages.MESSAGES.unknownAttribute(attributeName);
        }
    }

}
