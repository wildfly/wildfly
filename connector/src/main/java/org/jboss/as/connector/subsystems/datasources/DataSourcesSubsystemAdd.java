/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.connector.subsystems.datasources.Constants.*;
import static org.jboss.as.connector.subsystems.datasources.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.datasources.Constants.BACKGROUNDVALIDATION;
import static org.jboss.as.connector.subsystems.datasources.Constants.BACKGROUNDVALIDATIONMINUTES;
import static org.jboss.as.connector.subsystems.datasources.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.datasources.Constants.CHECKVALIDCONNECTIONSQL;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_URL;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCES;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_CLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.EXCEPTIONSORTERCLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.IDLETIMEOUTMINUTES;
import static org.jboss.as.connector.subsystems.datasources.Constants.INTERLIVING;
import static org.jboss.as.connector.subsystems.datasources.Constants.JNDINAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.MAX_POOL_SIZE;
import static org.jboss.as.connector.subsystems.datasources.Constants.MIN_POOL_SIZE;
import static org.jboss.as.connector.subsystems.datasources.Constants.MODULE;
import static org.jboss.as.connector.subsystems.datasources.Constants.NEW_CONNECTION_SQL;
import static org.jboss.as.connector.subsystems.datasources.Constants.NOTXSEPARATEPOOL;
import static org.jboss.as.connector.subsystems.datasources.Constants.PAD_XID;
import static org.jboss.as.connector.subsystems.datasources.Constants.PASSWORD;
import static org.jboss.as.connector.subsystems.datasources.Constants.POOLNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.POOL_PREFILL;
import static org.jboss.as.connector.subsystems.datasources.Constants.POOL_USE_STRICT_MIN;
import static org.jboss.as.connector.subsystems.datasources.Constants.PREPAREDSTATEMENTSCACHESIZE;
import static org.jboss.as.connector.subsystems.datasources.Constants.QUERYTIMEOUT;
import static org.jboss.as.connector.subsystems.datasources.Constants.SAME_RM_OVERRIDE;
import static org.jboss.as.connector.subsystems.datasources.Constants.SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.datasources.Constants.SETTXQUERYTIMEOUT;
import static org.jboss.as.connector.subsystems.datasources.Constants.SHAREPREPAREDSTATEMENTS;
import static org.jboss.as.connector.subsystems.datasources.Constants.SPY;
import static org.jboss.as.connector.subsystems.datasources.Constants.STALECONNECTIONCHECKERCLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.TRACKSTATEMENTS;
import static org.jboss.as.connector.subsystems.datasources.Constants.TRANSACTION_ISOLOATION;
import static org.jboss.as.connector.subsystems.datasources.Constants.URL_DELIMITER;
import static org.jboss.as.connector.subsystems.datasources.Constants.URL_SELECTOR_STRATEGY_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.USERNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.USETRYLOCK;
import static org.jboss.as.connector.subsystems.datasources.Constants.USE_FAST_FAIL;
import static org.jboss.as.connector.subsystems.datasources.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.connector.subsystems.datasources.Constants.VALIDATEONMATCH;
import static org.jboss.as.connector.subsystems.datasources.Constants.VALIDCONNECTIONCHECKERCLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.WRAP_XA_DATASOURCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.XADATASOURCECLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.XADATASOURCEPROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_DATASOURCES;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_RESOURCE_TIMEOUT;
import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.deployers.processors.DataSourcesAttachmentProcessor;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.BootOperationContext;
import org.jboss.as.server.BootOperationHandler;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.api.metadata.common.CommonPool;
import org.jboss.jca.common.api.metadata.common.CommonSecurity;
import org.jboss.jca.common.api.metadata.common.CommonXaPool;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.metadata.ds.DataSources;
import org.jboss.jca.common.api.metadata.ds.DsSecurity;
import org.jboss.jca.common.api.metadata.ds.JdbcAdapterExtension;
import org.jboss.jca.common.api.metadata.ds.Statement;
import org.jboss.jca.common.api.metadata.ds.Statement.TrackStatementsEnum;
import org.jboss.jca.common.api.metadata.ds.TimeOut;
import org.jboss.jca.common.api.metadata.ds.TransactionIsolation;
import org.jboss.jca.common.api.metadata.ds.Validation;
import org.jboss.jca.common.api.metadata.ds.XaDataSource;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.jca.common.metadata.common.CommonPoolImpl;
import org.jboss.jca.common.metadata.common.CommonSecurityImpl;
import org.jboss.jca.common.metadata.common.CommonXaPoolImpl;
import org.jboss.jca.common.metadata.ds.DataSourceImpl;
import org.jboss.jca.common.metadata.ds.DatasourcesImpl;
import org.jboss.jca.common.metadata.ds.DsSecurityImpl;
import org.jboss.jca.common.metadata.ds.StatementImpl;
import org.jboss.jca.common.metadata.ds.TimeOutImpl;
import org.jboss.jca.common.metadata.ds.ValidationImpl;
import org.jboss.jca.common.metadata.ds.XADataSourceImpl;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;

/**
 * Handler for adding the datasource subsystem.
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
class DataSourcesSubsystemAdd implements ModelAddOperationHandler, BootOperationHandler {

    static final DataSourcesSubsystemAdd INSTANCE = new DataSourcesSubsystemAdd();
    public static final Logger log = Logger.getLogger("org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemAdd");

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {
        // Populate subModel
        final ModelNode subModel = context.getSubModel();
        subModel.setEmptyObject();

        // Workaround to populate domain model.

        boolean workaround = true;

        if (workaround) {
            if (operation.has(DATASOURCES)) {
                ModelNode datasources = operation.get(DATASOURCES);
                subModel.get(DATASOURCES).set(datasources);
            }
        } else {

            if (operation.has(DATASOURCES)) {
                for (ModelNode dataSourceNode : operation.get(DATASOURCES).asList()) {
                    if (dataSourceNode.has(CONNECTION_PROPERTIES)) {
                        for (ModelNode property : dataSourceNode.get(CONNECTION_PROPERTIES).asList()) {
                            subModel.get(CONNECTION_PROPERTIES, property.asProperty().getName()).set(property.asString());
                        }
                    }
                    for (final String attribute : DataSourcesSubsystemProviders.DATASOURCE_ATTRIBUTE) {
                        if (operation.get(attribute).isDefined()) {
                            subModel.get(attribute).set(operation.get(attribute));
                        }
                    }
                }
            }

            if (operation.has(XA_DATASOURCES)) {
                for (ModelNode dataSourceNode : operation.get(XA_DATASOURCES).asList()) {
                    Map<String, String> connectionProperties;
                    if (dataSourceNode.has(XADATASOURCEPROPERTIES)) {
                        connectionProperties = new HashMap<String, String>(dataSourceNode.get(XADATASOURCEPROPERTIES).asList()
                                .size());
                        for (ModelNode property : dataSourceNode.get(XADATASOURCEPROPERTIES).asList()) {
                            subModel.get(XADATASOURCEPROPERTIES, property.asProperty().getName()).set(property.asString());
                        }
                    } else {
                        connectionProperties = Collections.emptyMap();
                    }

                    for (final String attribute : DataSourcesSubsystemProviders.XA_DATASOURCE_ATTRIBUTE) {
                        if (operation.get(attribute).isDefined()) {
                            subModel.get(attribute).set(operation.get(attribute));
                        }
                    }
                }
            }
        }

        if (context instanceof BootOperationContext) {
            final BootOperationContext updateContext = BootOperationContext.class.cast(context);
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceTarget serviceTarget = context.getServiceTarget();

                    DataSources datasources = null;
                    try {
                        datasources = buildDataSourcesObject(operation);
                    } catch (ValidateException e) {
                        throw new OperationFailedException(e, operation);
                    }
                    serviceTarget.addService(ConnectorServices.DATASOURCES_SERVICE, new DataSourcesService(datasources))
                            .setInitialMode(Mode.ACTIVE).install();

                    updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_DATA_SOURCES,
                            new DataSourcesAttachmentProcessor(datasources));
                    resultHandler.handleResultComplete();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(REMOVE);
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));
        return new BasicOperationResult(compensatingOperation);
    }

    private DataSources buildDataSourcesObject(ModelNode operation) throws ValidateException {
        List<DataSource> datasourceList = new ArrayList<DataSource>();
        List<XaDataSource> xadatasourceList = new ArrayList<XaDataSource>();
        try {
            if (operation.hasDefined(DATASOURCES)) {
                for (ModelNode dataSourceNode : operation.get(DATASOURCES).asList()) {
                    Map<String, String> connectionProperties;
                    if (dataSourceNode.has(CONNECTION_PROPERTIES)) {
                        connectionProperties = new HashMap<String, String>(dataSourceNode.get(CONNECTION_PROPERTIES).asList()
                                .size());
                        for (ModelNode property : dataSourceNode.get(CONNECTION_PROPERTIES).asList()) {
                            connectionProperties.put(property.asProperty().getName(), property.asString());
                        }
                    } else {
                        connectionProperties = Collections.EMPTY_MAP;
                    }
                    String connectionUrl = getStringIfSetOrGetDefault(dataSourceNode, CONNECTION_URL, null);
                    String driverClass = getStringIfSetOrGetDefault(dataSourceNode, DRIVER_CLASS, null);
                    String jndiName = getStringIfSetOrGetDefault(dataSourceNode, JNDINAME, null);
                    String module = getStringIfSetOrGetDefault(dataSourceNode, MODULE, null);
                    String newConnectionSql = getStringIfSetOrGetDefault(dataSourceNode, NEW_CONNECTION_SQL, null);
                    String poolName = getStringIfSetOrGetDefault(dataSourceNode, POOLNAME, null);
                    String urlDelimiter = getStringIfSetOrGetDefault(dataSourceNode, URL_DELIMITER, null);
                    String urlSelectorStrategyClassName = getStringIfSetOrGetDefault(dataSourceNode,
                            URL_SELECTOR_STRATEGY_CLASS_NAME, null);
                    boolean useJavaContext = getBooleanIfSetOrGetDefault(dataSourceNode, USE_JAVA_CONTEXT, false);
                    boolean enabled = getBooleanIfSetOrGetDefault(dataSourceNode, ENABLED, false);
                    Integer maxPoolSize = getIntIfSetOrGetDefault(dataSourceNode, MAX_POOL_SIZE, null);
                    Integer minPoolSize = getIntIfSetOrGetDefault(dataSourceNode, MIN_POOL_SIZE, null);
                    boolean prefill = getBooleanIfSetOrGetDefault(dataSourceNode, POOL_PREFILL, false);
                    boolean useStrictMin = getBooleanIfSetOrGetDefault(dataSourceNode, POOL_USE_STRICT_MIN, false);
                    CommonPool pool = new CommonPoolImpl(minPoolSize, maxPoolSize, prefill, useStrictMin);

                    String username = getStringIfSetOrGetDefault(dataSourceNode, USERNAME, null);
                    String password = getStringIfSetOrGetDefault(dataSourceNode, PASSWORD, null);
                    String securityDomain = getStringIfSetOrGetDefault(dataSourceNode, SECURITY_DOMAIN, null);

                    DsSecurity security = new DsSecurityImpl(username, password, securityDomain);

                    boolean sharePreparedStatements = getBooleanIfSetOrGetDefault(dataSourceNode, USE_JAVA_CONTEXT, false);
                    Long preparedStatementsCacheSize = getLongIfSetOrGetDefault(dataSourceNode, PREPAREDSTATEMENTSCACHESIZE,
                            null);
                    TrackStatementsEnum trackStatements = dataSourceNode.hasDefined(TRACKSTATEMENTS) ? TrackStatementsEnum
                            .valueOf(dataSourceNode.get(TRACKSTATEMENTS).asString()) : TrackStatementsEnum.NOWARN;
                    Statement statement = new StatementImpl(sharePreparedStatements, preparedStatementsCacheSize,
                            trackStatements);

                    Integer allocationRetry = getIntIfSetOrGetDefault(dataSourceNode, ALLOCATION_RETRY, null);
                    Long allocationRetryWaitMillis = getLongIfSetOrGetDefault(dataSourceNode, ALLOCATION_RETRY_WAIT_MILLIS,
                            null);
                    Long blockingTimeoutMillis = getLongIfSetOrGetDefault(dataSourceNode, BLOCKING_TIMEOUT_WAIT_MILLIS, null);
                    Long idleTimeoutMinutes = getLongIfSetOrGetDefault(dataSourceNode, IDLETIMEOUTMINUTES, null);
                    Long queryTimeout = getLongIfSetOrGetDefault(dataSourceNode, QUERYTIMEOUT, null);
                    Integer xaResourceTimeout = getIntIfSetOrGetDefault(dataSourceNode, XA_RESOURCE_TIMEOUT, null);
                    Long useTryLock = getLongIfSetOrGetDefault(dataSourceNode, USETRYLOCK, null);
                    boolean setTxQuertTimeout = getBooleanIfSetOrGetDefault(dataSourceNode, SETTXQUERYTIMEOUT, false);
                    TimeOut timeOut = new TimeOutImpl(blockingTimeoutMillis, idleTimeoutMinutes, allocationRetry,
                            allocationRetryWaitMillis, xaResourceTimeout, setTxQuertTimeout, queryTimeout, useTryLock);
                    TransactionIsolation transactionIsolation = dataSourceNode.has(TRANSACTION_ISOLOATION) ? TransactionIsolation
                            .valueOf(dataSourceNode.get(TRANSACTION_ISOLOATION).asString()) : null;
                    String checkValidConnectionSql = getStringIfSetOrGetDefault(dataSourceNode, CHECKVALIDCONNECTIONSQL, null);

                    JdbcAdapterExtension exceptionSorter = extractJdbcAdapterExtension(dataSourceNode,
                            EXCEPTIONSORTERCLASSNAME, EXCEPTIONSORTER_PROPERTIES);
                    JdbcAdapterExtension staleConnectionChecker = extractJdbcAdapterExtension(dataSourceNode,
                            STALECONNECTIONCHECKERCLASSNAME, STALECONNECTIONCHECKER_PROPERTIES);
                    JdbcAdapterExtension validConnectionChecker = extractJdbcAdapterExtension(dataSourceNode,
                            VALIDCONNECTIONCHECKERCLASSNAME, VALIDCONNECTIONCHECKER_PROPERTIES);

                    Long backgroundValidationMinutes = getLongIfSetOrGetDefault(dataSourceNode, BACKGROUNDVALIDATIONMINUTES,
                            null);
                    boolean backgroundValidation = getBooleanIfSetOrGetDefault(dataSourceNode, BACKGROUNDVALIDATION, false);
                    boolean useFastFail = getBooleanIfSetOrGetDefault(dataSourceNode, USE_FAST_FAIL, false);
                    boolean validateOnMatch = getBooleanIfSetOrGetDefault(dataSourceNode, VALIDATEONMATCH, false);
                    boolean spy = getBooleanIfSetOrGetDefault(dataSourceNode, SPY, false);
                    Validation validation = new ValidationImpl(backgroundValidation, backgroundValidationMinutes, useFastFail,
                            validConnectionChecker, checkValidConnectionSql, validateOnMatch, staleConnectionChecker,
                            exceptionSorter);
                    DataSource ds = new DataSourceImpl(connectionUrl, driverClass, module, transactionIsolation,
                            connectionProperties, timeOut, security, statement, validation, urlDelimiter,
                            urlSelectorStrategyClassName, newConnectionSql, useJavaContext, poolName, enabled, jndiName, spy,
                            pool);
                    datasourceList.add(ds);
                }
            }

            if (operation.hasDefined(XA_DATASOURCES)) {
                for (ModelNode dataSourceNode : operation.get(XA_DATASOURCES).asList()) {
                    Map<String, String> xaDataSourceProperty = new HashMap<String, String>(dataSourceNode
                            .get(XADATASOURCEPROPERTIES).asList().size());
                    for (ModelNode property : dataSourceNode.get(XADATASOURCEPROPERTIES).asList()) {
                        xaDataSourceProperty.put(property.asProperty().getName(), property.asString());
                    }
                    String xaDataSourceClass = getStringIfSetOrGetDefault(dataSourceNode, XADATASOURCECLASS, null);
                    String jndiName = getStringIfSetOrGetDefault(dataSourceNode, JNDINAME, null);
                    String module = getStringIfSetOrGetDefault(dataSourceNode, MODULE, null);
                    String newConnectionSql = getStringIfSetOrGetDefault(dataSourceNode, NEW_CONNECTION_SQL, null);
                    String poolName = getStringIfSetOrGetDefault(dataSourceNode, POOLNAME, null);
                    String urlDelimiter = getStringIfSetOrGetDefault(dataSourceNode, URL_DELIMITER, null);
                    String urlSelectorStrategyClassName = getStringIfSetOrGetDefault(dataSourceNode,
                            URL_SELECTOR_STRATEGY_CLASS_NAME, null);
                    boolean useJavaContext = getBooleanIfSetOrGetDefault(dataSourceNode, USE_JAVA_CONTEXT, false);
                    boolean enabled = getBooleanIfSetOrGetDefault(dataSourceNode, ENABLED, false);
                    Integer maxPoolSize = getIntIfSetOrGetDefault(dataSourceNode, MAX_POOL_SIZE, null);
                    Integer minPoolSize = getIntIfSetOrGetDefault(dataSourceNode, MIN_POOL_SIZE, null);
                    boolean prefill = getBooleanIfSetOrGetDefault(dataSourceNode, POOL_PREFILL, false);
                    boolean useStrictMin = getBooleanIfSetOrGetDefault(dataSourceNode, POOL_USE_STRICT_MIN, false);
                    boolean interleaving = getBooleanIfSetOrGetDefault(dataSourceNode, INTERLIVING, false);
                    boolean noTxSeparatePool = getBooleanIfSetOrGetDefault(dataSourceNode, NOTXSEPARATEPOOL, false);
                    boolean padXid = getBooleanIfSetOrGetDefault(dataSourceNode, PAD_XID, false);
                    boolean isSameRmOverride = getBooleanIfSetOrGetDefault(dataSourceNode, SAME_RM_OVERRIDE, false);
                    boolean wrapXaDataSource = getBooleanIfSetOrGetDefault(dataSourceNode, WRAP_XA_DATASOURCE, false);
                    CommonXaPool xaPool = new CommonXaPoolImpl(minPoolSize, maxPoolSize, prefill, useStrictMin,
                            isSameRmOverride, interleaving, padXid, wrapXaDataSource, noTxSeparatePool);

                    String username = getStringIfSetOrGetDefault(dataSourceNode, USERNAME, null);
                    String password = getStringIfSetOrGetDefault(dataSourceNode, PASSWORD, null);
                    String securityDomain = getStringIfSetOrGetDefault(dataSourceNode, SECURITY_DOMAIN, null);

                    DsSecurity security = new DsSecurityImpl(username, password, securityDomain);

                    boolean sharePreparedStatements = dataSourceNode.has(SHAREPREPAREDSTATEMENTS) ? dataSourceNode.get(
                            SHAREPREPAREDSTATEMENTS).asBoolean() : false;
                    Long preparedStatementsCacheSize = dataSourceNode.get(PREPAREDSTATEMENTSCACHESIZE).asLong();
                    TrackStatementsEnum trackStatements = TrackStatementsEnum.valueOf(dataSourceNode.get(TRACKSTATEMENTS)
                            .asString());
                    Statement statement = new StatementImpl(sharePreparedStatements, preparedStatementsCacheSize,
                            trackStatements);

                    Integer allocationRetry = getIntIfSetOrGetDefault(dataSourceNode, ALLOCATION_RETRY, null);
                    Long allocationRetryWaitMillis = getLongIfSetOrGetDefault(dataSourceNode, ALLOCATION_RETRY_WAIT_MILLIS,
                            null);
                    Long blockingTimeoutMillis = getLongIfSetOrGetDefault(dataSourceNode, BLOCKING_TIMEOUT_WAIT_MILLIS, null);
                    Long idleTimeoutMinutes = getLongIfSetOrGetDefault(dataSourceNode, IDLETIMEOUTMINUTES, null);
                    Long queryTimeout = getLongIfSetOrGetDefault(dataSourceNode, QUERYTIMEOUT, null);
                    Integer xaResourceTimeout = getIntIfSetOrGetDefault(dataSourceNode, XA_RESOURCE_TIMEOUT, null);
                    Long useTryLock = getLongIfSetOrGetDefault(dataSourceNode, USETRYLOCK, null);
                    boolean setTxQuertTimeout = getBooleanIfSetOrGetDefault(dataSourceNode, SETTXQUERYTIMEOUT, false);
                    TimeOut timeOut = new TimeOutImpl(blockingTimeoutMillis, idleTimeoutMinutes, allocationRetry,
                            allocationRetryWaitMillis, xaResourceTimeout, setTxQuertTimeout, queryTimeout, useTryLock);
                    TransactionIsolation transactionIsolation = dataSourceNode.has(TRANSACTION_ISOLOATION) ? TransactionIsolation
                            .valueOf(dataSourceNode.get(TRANSACTION_ISOLOATION).asString()) : null;
                    String checkValidConnectionSql = getStringIfSetOrGetDefault(dataSourceNode, CHECKVALIDCONNECTIONSQL, null);

                    JdbcAdapterExtension exceptionSorter = extractJdbcAdapterExtension(dataSourceNode,
                            EXCEPTIONSORTERCLASSNAME, EXCEPTIONSORTER_PROPERTIES);
                    JdbcAdapterExtension staleConnectionChecker = extractJdbcAdapterExtension(dataSourceNode,
                            STALECONNECTIONCHECKERCLASSNAME, STALECONNECTIONCHECKER_PROPERTIES);
                    JdbcAdapterExtension validConnectionChecker = extractJdbcAdapterExtension(dataSourceNode,
                            VALIDCONNECTIONCHECKERCLASSNAME, VALIDCONNECTIONCHECKER_PROPERTIES);

                    Long backgroundValidationMinutes = getLongIfSetOrGetDefault(dataSourceNode, BACKGROUNDVALIDATIONMINUTES,
                            null);
                    boolean backgroundValidation = getBooleanIfSetOrGetDefault(dataSourceNode, BACKGROUNDVALIDATION, false);
                    boolean useFastFail = getBooleanIfSetOrGetDefault(dataSourceNode, USE_FAST_FAIL, false);
                    boolean validateOnMatch = getBooleanIfSetOrGetDefault(dataSourceNode, VALIDATEONMATCH, false);
                    boolean spy = getBooleanIfSetOrGetDefault(dataSourceNode, SPY, false);
                    Validation validation = new ValidationImpl(backgroundValidation, backgroundValidationMinutes, useFastFail,
                            validConnectionChecker, checkValidConnectionSql, validateOnMatch, staleConnectionChecker,
                            exceptionSorter);
                    XaDataSource ds = new XADataSourceImpl(transactionIsolation, timeOut, security, statement, validation,
                            urlDelimiter, urlSelectorStrategyClassName, useJavaContext, poolName, enabled, jndiName, spy,
                            xaDataSourceProperty, xaDataSourceClass, module, newConnectionSql, xaPool);

                    xadatasourceList.add(ds);
                }
            }

            return new DatasourcesImpl(datasourceList, xadatasourceList);
        } catch (ValidateException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private JdbcAdapterExtension extractJdbcAdapterExtension(ModelNode dataSourceNode, String className, String propertyName)
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

            JdbcAdapterExtension exceptionSorter = new JdbcAdapterExtension(exceptionSorterClassName, exceptionSorterProperty);
            return exceptionSorter;
        } else {
            return null;
        }
    }

    private Long getLongIfSetOrGetDefault(ModelNode dataSourceNode, String key, Long defaultValue) {
        if (dataSourceNode.hasDefined(key)) {
            return dataSourceNode.get(key).asLong();
        } else {
            return defaultValue;
        }
    }

    private Integer getIntIfSetOrGetDefault(ModelNode dataSourceNode, String key, Integer defaultValue) {
        if (dataSourceNode.hasDefined(key)) {
            return dataSourceNode.get(key).asInt();
        } else {
            return defaultValue;
        }
    }

    private boolean getBooleanIfSetOrGetDefault(ModelNode dataSourceNode, String key, boolean defaultValue) {
        if (dataSourceNode.hasDefined(key)) {
            return dataSourceNode.get(key).asBoolean();
        } else {
            return defaultValue;
        }
    }

    private String getStringIfSetOrGetDefault(ModelNode dataSourceNode, String key, String defaultValue) {
        if (dataSourceNode.hasDefined(key)) {
            return dataSourceNode.get(key).asString();
        } else {
            return defaultValue;
        }
    }
}
