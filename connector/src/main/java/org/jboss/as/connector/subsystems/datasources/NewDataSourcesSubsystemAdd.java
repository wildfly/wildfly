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

import static org.jboss.as.connector.subsystems.datasources.Constants.ALLOCATION_RETRY;
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
import static org.jboss.as.connector.subsystems.datasources.Constants.SETTXQUERTTIMEOUT;
import static org.jboss.as.connector.subsystems.datasources.Constants.SHAREPREPAREDSTATEMENTS;
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
import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.NewBootOperationContext;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.NewServerOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.api.metadata.common.CommonPool;
import org.jboss.jca.common.api.metadata.common.CommonSecurity;
import org.jboss.jca.common.api.metadata.common.CommonXaPool;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.metadata.ds.DataSources;
import org.jboss.jca.common.api.metadata.ds.Statement;
import org.jboss.jca.common.api.metadata.ds.Statement.TrackStatementsEnum;
import org.jboss.jca.common.api.metadata.ds.TimeOut;
import org.jboss.jca.common.api.metadata.ds.TransactionIsolation;
import org.jboss.jca.common.api.metadata.ds.Validation;
import org.jboss.jca.common.api.metadata.ds.XaDataSource;
import org.jboss.jca.common.metadata.common.CommonPoolImpl;
import org.jboss.jca.common.metadata.common.CommonSecurityImpl;
import org.jboss.jca.common.metadata.common.CommonXaPoolImpl;
import org.jboss.jca.common.metadata.ds.DataSourceImpl;
import org.jboss.jca.common.metadata.ds.DatasourcesImpl;
import org.jboss.jca.common.metadata.ds.StatementImpl;
import org.jboss.jca.common.metadata.ds.TimeOutImpl;
import org.jboss.jca.common.metadata.ds.ValidationImpl;
import org.jboss.jca.common.metadata.ds.XADataSourceImpl;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
class NewDataSourcesSubsystemAdd implements ModelAddOperationHandler, RuntimeOperationHandler {

    static final NewDataSourcesSubsystemAdd INSTANCE = new NewDataSourcesSubsystemAdd();

    /** {@inheritDoc} */
    @Override
    public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(REMOVE);
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));


        if (context instanceof NewBootOperationContext || context instanceof NewRuntimeOperationContext) {
            final NewServerOperationContext updateContext = (NewServerOperationContext) context;
            final ServiceTarget serviceTarget = updateContext.getServiceTarget();

            try {
                DataSources datasources = buildDataSourcesObject(operation);
                serviceTarget.addService(ConnectorServices.DATASOURCES_SERVICE, new DataSourcesService(datasources))
                        .setInitialMode(Mode.ACTIVE).install();
                if (context instanceof NewBootOperationContext) {
                    NewBootOperationContext bootContext = (NewBootOperationContext) context;
                    bootContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_DATA_SOURCES, new DataSourcesAttachmentProcessor(datasources));
                }
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }

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
                    for (final String attribute : NewDataSourcesSubsystemProviders.DATASOURCE_ATTRIBUTE) {
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

                    for (final String attribute : NewDataSourcesSubsystemProviders.XA_DATASOURCE_ATTRIBUTE) {
                        if (operation.get(attribute).isDefined()) {
                            subModel.get(attribute).set(operation.get(attribute));
                        }
                    }
                }
            }
        }

        resultHandler.handleResultComplete(compensatingOperation);

        return Cancellable.NULL;
    }

    private DataSources buildDataSourcesObject(ModelNode operation) {
        List<DataSource> datasourceList = new ArrayList<DataSource>();
        List<XaDataSource> xadatasourceList = new ArrayList<XaDataSource>();

        if (operation.hasDefined(DATASOURCES)) {
            for (ModelNode dataSourceNode : operation.get(DATASOURCES).asList()) {
                Map<String, String> connectionProperties;
                if (dataSourceNode.has(CONNECTION_PROPERTIES)) {
                    connectionProperties = new HashMap<String, String>(dataSourceNode.get(CONNECTION_PROPERTIES).asList().size());
                    for (ModelNode property : dataSourceNode.get(CONNECTION_PROPERTIES).asList()) {
                        connectionProperties.put(property.asProperty().getName(), property.asString());
                    }
                } else {
                    connectionProperties = Collections.EMPTY_MAP;
                }
                String connectionUrl = dataSourceNode.get(CONNECTION_URL).asString();
                String driverClass = dataSourceNode.get(DRIVER_CLASS).asString();
                String jndiName = dataSourceNode.get(JNDINAME).asString();
                String module = dataSourceNode.get(MODULE).asString();
                String newConnectionSql = dataSourceNode.get(NEW_CONNECTION_SQL).asString();
                String poolName = dataSourceNode.get(POOLNAME).asString();
                String urlDelimiter = dataSourceNode.get(URL_DELIMITER).asString();
                String urlSelectorStrategyClassName = dataSourceNode.get(URL_SELECTOR_STRATEGY_CLASS_NAME).asString();
                boolean useJavaContext = dataSourceNode.get(USE_JAVA_CONTEXT).asBoolean();
                boolean enabled = dataSourceNode.get(ENABLED).asBoolean();
                Integer maxPoolSize = dataSourceNode.get(MAX_POOL_SIZE).asInt();
                Integer minPoolSize = dataSourceNode.get(MIN_POOL_SIZE).asInt();
                boolean prefill = dataSourceNode.get(POOL_PREFILL).asBoolean();
                boolean useStrictMin = dataSourceNode.get(POOL_USE_STRICT_MIN).asBoolean();
                CommonPool pool = new CommonPoolImpl(minPoolSize, maxPoolSize, prefill, useStrictMin);
                CommonSecurity security = new CommonSecurityImpl(dataSourceNode.get(USERNAME).asString(), dataSourceNode.get(
                        PASSWORD).asString());
                boolean sharePreparedStatements = dataSourceNode.get(SHAREPREPAREDSTATEMENTS).asBoolean();
                Long preparedStatementsCacheSize = dataSourceNode.get(PREPAREDSTATEMENTSCACHESIZE).asLong();
                TrackStatementsEnum trackStatements = TrackStatementsEnum.valueOf(dataSourceNode.get(TRACKSTATEMENTS).asString());
                Statement statement = new StatementImpl(sharePreparedStatements, preparedStatementsCacheSize, trackStatements);

                Integer allocationRetry = dataSourceNode.get(ALLOCATION_RETRY).asInt();
                Long allocationRetryWaitMillis = dataSourceNode.get(ALLOCATION_RETRY_WAIT_MILLIS).asLong();
                Long blockingTimeoutMillis = dataSourceNode.get(BLOCKING_TIMEOUT_WAIT_MILLIS).asLong();
                Long idleTimeoutMinutes = dataSourceNode.get(IDLETIMEOUTMINUTES).asLong();
                Long queryTimeout = dataSourceNode.get(QUERYTIMEOUT).asLong();
                Integer xaResourceTimeout = dataSourceNode.get(XA_RESOURCE_TIMEOUT).asInt();
                Long useTryLock = dataSourceNode.get(USETRYLOCK).asLong();
                Boolean setTxQuertTimeout = dataSourceNode.get(SETTXQUERTTIMEOUT).asBoolean();
                TimeOut timeOut = new TimeOutImpl(blockingTimeoutMillis, idleTimeoutMinutes, allocationRetry,
                        allocationRetryWaitMillis, xaResourceTimeout, setTxQuertTimeout, queryTimeout, useTryLock);
                TransactionIsolation transactionIsolation = TransactionIsolation.valueOf(dataSourceNode.get(TRANSACTION_ISOLOATION)
                        .asString());
                String checkValidConnectionSql = dataSourceNode.get(CHECKVALIDCONNECTIONSQL).asString();
                String exceptionSorterClassName = dataSourceNode.get(EXCEPTIONSORTERCLASSNAME).asString();
                String staleConnectionCheckerClassName = dataSourceNode.get(STALECONNECTIONCHECKERCLASSNAME).asString();
                String validConnectionCheckerClassName = dataSourceNode.get(VALIDCONNECTIONCHECKERCLASSNAME).asString();
                Long backgroundValidationMinutes = dataSourceNode.get(BACKGROUNDVALIDATIONMINUTES).asLong();
                boolean backgroundValidation = dataSourceNode.get(BACKGROUNDVALIDATION).asBoolean();
                boolean useFastFail = dataSourceNode.get(USE_FAST_FAIL).asBoolean();
                boolean validateOnMatch = dataSourceNode.get(VALIDATEONMATCH).asBoolean();
                Validation validation = new ValidationImpl(backgroundValidation, backgroundValidationMinutes, useFastFail,
                        validConnectionCheckerClassName, checkValidConnectionSql, validateOnMatch, staleConnectionCheckerClassName,
                        exceptionSorterClassName);
                DataSource ds = new DataSourceImpl(connectionUrl, driverClass, module, transactionIsolation, connectionProperties,
                        timeOut, security, statement, validation, urlDelimiter, urlSelectorStrategyClassName, newConnectionSql,
                        useJavaContext, poolName, enabled, jndiName, pool);
                datasourceList.add(ds);
            }
        }

        if (operation.hasDefined(XA_DATASOURCES)) {
            for (ModelNode dataSourceNode : operation.get(XA_DATASOURCES).asList()) {
                Map<String, String> xaDataSourceProperty = new HashMap<String, String>(dataSourceNode.get(XADATASOURCEPROPERTIES)
                        .asList().size());
                for (ModelNode property : dataSourceNode.get(XADATASOURCEPROPERTIES).asList()) {
                    xaDataSourceProperty.put(property.asProperty().getName(), property.asString());
                }
                String xaDataSourceClass = dataSourceNode.get(XADATASOURCECLASS).asString();
                String jndiName = dataSourceNode.get(JNDINAME).asString();
                String module = dataSourceNode.get(MODULE).asString();
                String newConnectionSql = dataSourceNode.get(NEW_CONNECTION_SQL).asString();
                String poolName = dataSourceNode.get(POOLNAME).asString();
                String urlDelimiter = dataSourceNode.get(URL_DELIMITER).asString();
                String urlSelectorStrategyClassName = dataSourceNode.get(URL_SELECTOR_STRATEGY_CLASS_NAME).asString();
                boolean useJavaContext = dataSourceNode.get(USE_JAVA_CONTEXT).asBoolean();
                boolean enabled = dataSourceNode.get(ENABLED).asBoolean();
                Integer maxPoolSize = dataSourceNode.get(MAX_POOL_SIZE).asInt();
                Integer minPoolSize = dataSourceNode.get(MIN_POOL_SIZE).asInt();
                boolean prefill = dataSourceNode.get(POOL_PREFILL).asBoolean();
                boolean useStrictMin = dataSourceNode.get(POOL_USE_STRICT_MIN).asBoolean();
                boolean interleaving = dataSourceNode.get(INTERLIVING).asBoolean();
                boolean noTxSeparatePool = dataSourceNode.get(NOTXSEPARATEPOOL).asBoolean();
                boolean padXid = dataSourceNode.get(PAD_XID).asBoolean();
                boolean isSameRmOverride = dataSourceNode.get(SAME_RM_OVERRIDE).asBoolean();
                boolean wrapXaDataSource = dataSourceNode.get(WRAP_XA_DATASOURCE).asBoolean();
                CommonXaPool xaPool = new CommonXaPoolImpl(minPoolSize, maxPoolSize, prefill, useStrictMin, isSameRmOverride,
                        interleaving, padXid, wrapXaDataSource, noTxSeparatePool);
                CommonSecurity security = new CommonSecurityImpl(dataSourceNode.get(USERNAME).asString(), dataSourceNode.get(
                        PASSWORD).asString());
                boolean sharePreparedStatements = dataSourceNode.get(SHAREPREPAREDSTATEMENTS).asBoolean();
                Long preparedStatementsCacheSize = dataSourceNode.get(PREPAREDSTATEMENTSCACHESIZE).asLong();
                TrackStatementsEnum trackStatements = TrackStatementsEnum.valueOf(dataSourceNode.get(TRACKSTATEMENTS).asString());
                Statement statement = new StatementImpl(sharePreparedStatements, preparedStatementsCacheSize, trackStatements);

                Integer allocationRetry = dataSourceNode.get(ALLOCATION_RETRY).asInt();
                Long allocationRetryWaitMillis = dataSourceNode.get(ALLOCATION_RETRY_WAIT_MILLIS).asLong();
                Long blockingTimeoutMillis = dataSourceNode.get(BLOCKING_TIMEOUT_WAIT_MILLIS).asLong();
                Long idleTimeoutMinutes = dataSourceNode.get(IDLETIMEOUTMINUTES).asLong();
                Long queryTimeout = dataSourceNode.get(QUERYTIMEOUT).asLong();
                Integer xaResourceTimeout = dataSourceNode.get(XA_RESOURCE_TIMEOUT).asInt();
                Long useTryLock = dataSourceNode.get(USETRYLOCK).asLong();
                Boolean setTxQuertTimeout = dataSourceNode.get(SETTXQUERTTIMEOUT).asBoolean();
                TimeOut timeOut = new TimeOutImpl(blockingTimeoutMillis, idleTimeoutMinutes, allocationRetry,
                        allocationRetryWaitMillis, xaResourceTimeout, setTxQuertTimeout, queryTimeout, useTryLock);
                TransactionIsolation transactionIsolation = TransactionIsolation.valueOf(dataSourceNode.get(TRANSACTION_ISOLOATION)
                        .asString());
                String checkValidConnectionSql = dataSourceNode.get(CHECKVALIDCONNECTIONSQL).asString();
                String exceptionSorterClassName = dataSourceNode.get(EXCEPTIONSORTERCLASSNAME).asString();
                String staleConnectionCheckerClassName = dataSourceNode.get(STALECONNECTIONCHECKERCLASSNAME).asString();
                String validConnectionCheckerClassName = dataSourceNode.get(VALIDCONNECTIONCHECKERCLASSNAME).asString();
                Long backgroundValidationMinutes = dataSourceNode.get(BACKGROUNDVALIDATIONMINUTES).asLong();
                boolean backgroundValidation = dataSourceNode.get(BACKGROUNDVALIDATION).asBoolean();
                boolean useFastFail = dataSourceNode.get(USE_FAST_FAIL).asBoolean();
                boolean validateOnMatch = dataSourceNode.get(VALIDATEONMATCH).asBoolean();
                Validation validation = new ValidationImpl(backgroundValidation, backgroundValidationMinutes, useFastFail,
                        validConnectionCheckerClassName, checkValidConnectionSql, validateOnMatch, staleConnectionCheckerClassName,
                        exceptionSorterClassName);
                XaDataSource ds = new XADataSourceImpl(transactionIsolation, timeOut, security, statement, validation,
                        urlDelimiter, urlSelectorStrategyClassName, useJavaContext, poolName, enabled, jndiName,
                        xaDataSourceProperty, xaDataSourceClass, module, newConnectionSql, xaPool);

                xadatasourceList.add(ds);
            }
        }

        return new DatasourcesImpl(datasourceList, xadatasourceList);

    }
}
