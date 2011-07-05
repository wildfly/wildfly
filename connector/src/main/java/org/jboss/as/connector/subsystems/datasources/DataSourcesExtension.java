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

import static org.jboss.as.connector.pool.Constants.BACKGROUNDVALIDATION;
import static org.jboss.as.connector.pool.Constants.BACKGROUNDVALIDATIONMINUTES;
import static org.jboss.as.connector.pool.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS;
import static org.jboss.as.connector.pool.Constants.IDLETIMEOUTMINUTES;
import static org.jboss.as.connector.pool.Constants.MAX_POOL_SIZE;
import static org.jboss.as.connector.pool.Constants.MIN_POOL_SIZE;
import static org.jboss.as.connector.pool.Constants.POOL_PREFILL;
import static org.jboss.as.connector.pool.Constants.POOL_USE_STRICT_MIN;
import static org.jboss.as.connector.pool.Constants.USE_FAST_FAIL;
import static org.jboss.as.connector.subsystems.datasources.AbstractDataSourceAdd.populateAddModel;
import static org.jboss.as.connector.subsystems.datasources.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.datasources.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.datasources.Constants.CHECKVALIDCONNECTIONSQL;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_URL;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCES;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_DRIVER;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_DRIVER_CLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATA_SOURCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_MAJOR_VERSION;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_MINOR_VERSION;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_MODULE_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_XA_DATASOURCE_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.EXCEPTIONSORTERCLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.EXCEPTIONSORTER_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.FLUSH_STRATEGY;
import static org.jboss.as.connector.subsystems.datasources.Constants.INTERLIVING;
import static org.jboss.as.connector.subsystems.datasources.Constants.JDBC_DRIVER;
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
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_DATA_SOURCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_RESOURCE_TIMEOUT;
import static org.jboss.as.connector.subsystems.datasources.DataSourceModelNodeUtil.fillFrom;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.ADD_DATA_SOURCE_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.ADD_JDBC_DRIVER_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.ADD_XA_DATA_SOURCE_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.DATASOURCE_ATTRIBUTE;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.DATA_SOURCE_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.DISABLE_DATA_SOURCE_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.DISABLE_XA_DATA_SOURCE_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.ENABLE_DATA_SOURCE_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.ENABLE_XA_DATA_SOURCE_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.FLUSH_ALL_CONNECTION_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.FLUSH_IDLE_CONNECTION_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.GET_INSTALLED_DRIVER_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.INSTALLED_DRIVERS_LIST_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.JDBC_DRIVER_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.REMOVE_DATA_SOURCE_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.REMOVE_JDBC_DRIVER_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.REMOVE_XA_DATA_SOURCE_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.SUBSYSTEM;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.SUBSYSTEM_ADD_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.TEST_CONNECTION_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.XA_DATASOURCE_ATTRIBUTE;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.XA_DATA_SOURCE_DESC;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DISABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.connector.pool.PoolConfigurationRWHandler;
import org.jboss.as.connector.pool.PoolConfigurationRWHandler.LocalAndXaDataSourcePoolConfigurationWriteHandler;
import org.jboss.as.connector.pool.PoolConfigurationRWHandler.PoolConfigurationReadHandler;
import org.jboss.as.connector.pool.PoolMetrics;
import org.jboss.as.connector.pool.PoolOperations;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.jca.common.api.metadata.common.CommonPool;
import org.jboss.jca.common.api.metadata.common.CommonXaPool;
import org.jboss.jca.common.api.metadata.common.Credential;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.metadata.ds.DataSources;
import org.jboss.jca.common.api.metadata.ds.Driver;
import org.jboss.jca.common.api.metadata.ds.DsSecurity;
import org.jboss.jca.common.api.metadata.ds.Statement;
import org.jboss.jca.common.api.metadata.ds.TimeOut;
import org.jboss.jca.common.api.metadata.ds.Validation;
import org.jboss.jca.common.api.metadata.ds.XaDataSource;
import org.jboss.jca.common.metadata.ds.DsParser;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author John Bailey
 */
public class DataSourcesExtension implements Extension {

    private static final Logger log = Logger.getLogger("org.jboss.as.datasources");

    @Override
    public void initialize(final ExtensionContext context) {
        log.debugf("Initializing Datasources Extension");

        // Register the remoting subsystem
        final SubsystemRegistration registration = context.registerSubsystem(DATASOURCES);

        registration.registerXMLElementWriter(NewDataSourceSubsystemParser.INSTANCE);

        // Remoting subsystem description and operation handlers
        final ManagementResourceRegistration subsystem = registration.registerSubsystemModel(SUBSYSTEM);
        subsystem.registerOperationHandler(ADD, DataSourcesSubsystemAdd.INSTANCE, SUBSYSTEM_ADD_DESC, false);
        subsystem.registerOperationHandler(DESCRIBE, DataSourcesSubsystemDescribeHandler.INSTANCE,
                DataSourcesSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        subsystem.registerOperationHandler("installed-drivers-list", InstalledDriversListOperationHandler.INSTANCE,
                INSTALLED_DRIVERS_LIST_DESC);
        subsystem.registerOperationHandler("get-installed-driver", GetInstalledDriverOperationHandler.INSTANCE,
                GET_INSTALLED_DRIVER_DESC);

        final ManagementResourceRegistration jdbcDrivers = subsystem.registerSubModel(PathElement.pathElement(JDBC_DRIVER),
                JDBC_DRIVER_DESC);
        jdbcDrivers.registerOperationHandler(ADD, JdbcDriverAdd.INSTANCE, ADD_JDBC_DRIVER_DESC, false);
        jdbcDrivers.registerOperationHandler(REMOVE, JdbcDriverRemove.INSTANCE, REMOVE_JDBC_DRIVER_DESC, false);

        final ManagementResourceRegistration dataSources = subsystem.registerSubModel(PathElement.pathElement(DATA_SOURCE),
                DATA_SOURCE_DESC);
        dataSources.registerOperationHandler(ADD, DataSourceAdd.INSTANCE, ADD_DATA_SOURCE_DESC, false);
        dataSources.registerOperationHandler(REMOVE, DataSourceRemove.INSTANCE, REMOVE_DATA_SOURCE_DESC, false);
        dataSources.registerOperationHandler(ENABLE, DataSourceEnable.LOCAL_INSTANCE, ENABLE_DATA_SOURCE_DESC, false);
        dataSources.registerOperationHandler(DISABLE, DataSourceDisable.INSTANCE, DISABLE_DATA_SOURCE_DESC, false);
        dataSources.registerOperationHandler("flush-idle-connection-in-pool",
                PoolOperations.FlushIdleConnectionInPool.DS_INSTANCE, FLUSH_IDLE_CONNECTION_DESC, false);
        dataSources.registerOperationHandler("flush-all-connection-in-pool",
                PoolOperations.FlushAllConnectionInPool.DS_INSTANCE, FLUSH_ALL_CONNECTION_DESC, false);
        dataSources.registerOperationHandler("test-connection-in-pool", PoolOperations.TestConnectionInPool.DS_INSTANCE,
                TEST_CONNECTION_DESC, false);

        for (final String attributeName : PoolMetrics.ATTRIBUTES) {
            dataSources.registerMetric(attributeName, PoolMetrics.LocalAndXaDataSourcePoolMetricsHandler.INSTANCE);

        }

        for (final String attributeName : LocalAndXaDataSourcesJdbcMetrics.ATTRIBUTES) {
            dataSources.registerMetric(attributeName, LocalAndXaDataSourcesJdbcMetrics.INSTANCE);

        }

        for (final String attributeName : PoolConfigurationRWHandler.ATTRIBUTES) {
            dataSources.registerReadWriteAttribute(attributeName, PoolConfigurationReadHandler.INSTANCE,
                    LocalAndXaDataSourcePoolConfigurationWriteHandler.INSTANCE, Storage.CONFIGURATION);
        }

        final ManagementResourceRegistration xaDataSources = subsystem.registerSubModel(PathElement.pathElement(XA_DATA_SOURCE),
                XA_DATA_SOURCE_DESC);
        xaDataSources.registerOperationHandler(ADD, XaDataSourceAdd.INSTANCE, ADD_XA_DATA_SOURCE_DESC, false);
        xaDataSources.registerOperationHandler(REMOVE, XaDataSourceRemove.INSTANCE, REMOVE_XA_DATA_SOURCE_DESC, false);
        xaDataSources.registerOperationHandler(ENABLE, DataSourceEnable.XA_INSTANCE, ENABLE_XA_DATA_SOURCE_DESC, false);
        xaDataSources.registerOperationHandler(DISABLE, DataSourceDisable.INSTANCE, DISABLE_XA_DATA_SOURCE_DESC, false);
        xaDataSources.registerOperationHandler("flush-idle-connection-in-pool",
                PoolOperations.FlushIdleConnectionInPool.DS_INSTANCE, FLUSH_IDLE_CONNECTION_DESC, false);
        xaDataSources.registerOperationHandler("flush-all-connection-in-pool",
                PoolOperations.FlushAllConnectionInPool.DS_INSTANCE, FLUSH_ALL_CONNECTION_DESC, false);
        xaDataSources.registerOperationHandler("test-connection-in-pool", PoolOperations.TestConnectionInPool.DS_INSTANCE,
                TEST_CONNECTION_DESC, false);

        for (final String attributeName : PoolMetrics.ATTRIBUTES) {
            xaDataSources.registerMetric(attributeName, PoolMetrics.LocalAndXaDataSourcePoolMetricsHandler.INSTANCE);
        }

        for (final String attributeName : LocalAndXaDataSourcesJdbcMetrics.ATTRIBUTES) {
            xaDataSources.registerMetric(attributeName, LocalAndXaDataSourcesJdbcMetrics.INSTANCE);

        }

        for (final String attributeName : PoolConfigurationRWHandler.ATTRIBUTES) {
            xaDataSources.registerReadWriteAttribute(attributeName, PoolConfigurationReadHandler.INSTANCE,
                    LocalAndXaDataSourcePoolConfigurationWriteHandler.INSTANCE, Storage.CONFIGURATION);
        }

    }

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), NewDataSourceSubsystemParser.INSTANCE);
    }

    public static final class NewDataSourceSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
            XMLElementWriter<SubsystemMarshallingContext> {

        static final NewDataSourceSubsystemParser INSTANCE = new NewDataSourceSubsystemParser();

        /**
         * {@inheritDoc}
         */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
            ModelNode node = context.getModelNode();

            writer.writeStartElement(DATASOURCES);

            if (node.hasDefined(DATA_SOURCE) || node.hasDefined(XA_DATA_SOURCE)) {
                List<Property> propertyList = node.hasDefined(DATA_SOURCE) ? node.get(DATA_SOURCE).asPropertyList()
                        : new LinkedList<Property>();
                if (node.hasDefined(XA_DATA_SOURCE)) {
                    propertyList.addAll(node.get(XA_DATA_SOURCE).asPropertyList());
                }
                for (Property property : propertyList) {
                    final ModelNode dataSourceNode = property.getValue();
                    boolean isXADataSource = hasAnyOf(dataSourceNode, XA_RESOURCE_TIMEOUT, XADATASOURCECLASS,
                            XADATASOURCEPROPERTIES);
                    writer.writeStartElement(isXADataSource ? DataSources.Tag.XA_DATASOURCE.getLocalName()
                            : DataSources.Tag.DATASOURCE.getLocalName());

                    writeAttributeIfHas(writer, dataSourceNode, DataSource.Attribute.JNDINAME, JNDINAME);
                    writeAttributeIfHas(writer, dataSourceNode, DataSource.Attribute.POOL_NAME, POOLNAME);
                    writeAttributeIfHas(writer, dataSourceNode, DataSource.Attribute.ENABLED, ENABLED);
                    writeAttributeIfHas(writer, dataSourceNode, DataSource.Attribute.JTA, JTA);
                    writeAttributeIfHas(writer, dataSourceNode, DataSource.Attribute.USEJAVACONTEXT, USE_JAVA_CONTEXT);
                    writeAttributeIfHas(writer, dataSourceNode, DataSource.Attribute.SPY, SPY);
                    writeAttributeIfHas(writer, dataSourceNode, DataSource.Attribute.USE_CCM, USE_CCM);

                    if (!isXADataSource) {
                        writeElementIfHas(writer, dataSourceNode, DataSource.Tag.CONNECTIONURL, CONNECTION_URL);
                        writeElementIfHas(writer, dataSourceNode, DataSource.Tag.DRIVERCLASS, DATASOURCE_DRIVER_CLASS);
                        if (dataSourceNode.hasDefined(CONNECTION_PROPERTIES)) {
                            for (Property connectionProperty : dataSourceNode.get(CONNECTION_PROPERTIES).asPropertyList()) {
                                writeProperty(writer, dataSourceNode, connectionProperty.getName(), connectionProperty
                                        .getValue().asString(), DataSource.Tag.CONNECTIONPROPERTY.getLocalName());
                            }
                        }
                    }
                    if (isXADataSource) {
                        if (dataSourceNode.hasDefined(XADATASOURCEPROPERTIES)) {
                            for (Property prop : dataSourceNode.get(XADATASOURCEPROPERTIES).asPropertyList()) {
                                writer.writeStartElement(XaDataSource.Tag.XADATASOURCEPROPERTY.getLocalName());
                                writer.writeAttribute("name", prop.getName());
                                writer.writeCharacters(prop.getValue().asString());
                                writer.writeEndElement();
                            }

                        }
                        writeElementIfHas(writer, dataSourceNode, XaDataSource.Tag.XADATASOURCECLASS, XADATASOURCECLASS);

                    }
                    writeElementIfHas(writer, dataSourceNode, DataSource.Tag.DRIVER, DATASOURCE_DRIVER);

                    if (isXADataSource) {
                        writeElementIfHas(writer, dataSourceNode, XaDataSource.Tag.URLDELIMITER, URL_DELIMITER);
                        writeElementIfHas(writer, dataSourceNode, XaDataSource.Tag.URLSELECTORSTRATEGYCLASSNAME,
                                URL_SELECTOR_STRATEGY_CLASS_NAME);
                    }
                    writeElementIfHas(writer, dataSourceNode, DataSource.Tag.NEWCONNECTIONSQL, NEW_CONNECTION_SQL);
                    writeElementIfHas(writer, dataSourceNode, DataSource.Tag.TRANSACTIONISOLATION, TRANSACTION_ISOLOATION);

                    if (!isXADataSource) {
                        writeElementIfHas(writer, dataSourceNode, DataSource.Tag.URLDELIMITER, URL_DELIMITER);
                        writeElementIfHas(writer, dataSourceNode, DataSource.Tag.URLSELECTORSTRATEGYCLASSNAME,
                                URL_SELECTOR_STRATEGY_CLASS_NAME);
                    }
                    boolean poolRequired = hasAnyOf(dataSourceNode, MIN_POOL_SIZE, MAX_POOL_SIZE, POOL_PREFILL,
                            POOL_USE_STRICT_MIN, FLUSH_STRATEGY);
                    if (isXADataSource) {
                        poolRequired = poolRequired
                                || hasAnyOf(dataSourceNode, SAME_RM_OVERRIDE, INTERLIVING, NOTXSEPARATEPOOL, PAD_XID,
                                        WRAP_XA_DATASOURCE);
                    }
                    if (poolRequired) {
                        writer.writeStartElement(isXADataSource ? XaDataSource.Tag.XA_POOL.getLocalName() : DataSource.Tag.POOL
                                .getLocalName());
                        writeElementIfHas(writer, dataSourceNode, CommonPool.Tag.MIN_POOL_SIZE, MIN_POOL_SIZE);
                        writeElementIfHas(writer, dataSourceNode, CommonPool.Tag.MAXPOOLSIZE, MAX_POOL_SIZE);
                        writeElementIfHas(writer, dataSourceNode, CommonPool.Tag.PREFILL, POOL_PREFILL);
                        writeElementIfHas(writer, dataSourceNode, CommonPool.Tag.USE_STRICT_MIN, POOL_USE_STRICT_MIN);
                        writeElementIfHas(writer, dataSourceNode, CommonPool.Tag.FLUSH_STRATEGY, FLUSH_STRATEGY);

                        if (isXADataSource) {
                            writeEmptyElementIfHasAndTrue(writer, dataSourceNode, CommonXaPool.Tag.ISSAMERMOVERRIDEVALUE,
                                    SAME_RM_OVERRIDE);
                            writeEmptyElementIfHasAndTrue(writer, dataSourceNode, CommonXaPool.Tag.INTERLEAVING, INTERLIVING);
                            writeEmptyElementIfHasAndTrue(writer, dataSourceNode, CommonXaPool.Tag.NO_TX_SEPARATE_POOLS,
                                    NOTXSEPARATEPOOL);
                            writeElementIfHas(writer, dataSourceNode, CommonXaPool.Tag.PAD_XID, PAD_XID);
                            writeElementIfHas(writer, dataSourceNode, CommonXaPool.Tag.WRAP_XA_RESOURCE, WRAP_XA_DATASOURCE);
                        }
                        writer.writeEndElement();
                    }
                    boolean securityRequired = hasAnyOf(dataSourceNode, USERNAME, PASSWORD, SECURITY_DOMAIN,
                            REAUTHPLUGIN_CLASSNAME, REAUTHPLUGIN_PROPERTIES);
                    if (securityRequired) {
                        writer.writeStartElement(DataSource.Tag.SECURITY.getLocalName());
                        writeElementIfHas(writer, dataSourceNode, DsSecurity.Tag.USERNAME, USERNAME);
                        writeElementIfHas(writer, dataSourceNode, DsSecurity.Tag.PASSWORD, PASSWORD);
                        writeElementIfHas(writer, dataSourceNode, DsSecurity.Tag.SECURITY_DOMAIN, SECURITY_DOMAIN);

                        if (dataSourceNode.hasDefined(REAUTHPLUGIN_CLASSNAME)) {
                            writer.writeStartElement(DsSecurity.Tag.REAUTH_PLUGIN.getLocalName());
                            writer.writeAttribute(
                                    org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(),
                                    dataSourceNode.get(REAUTHPLUGIN_CLASSNAME).asString());

                            if (dataSourceNode.hasDefined(REAUTHPLUGIN_PROPERTIES)) {
                                for (Property connectionProperty : dataSourceNode.get(REAUTHPLUGIN_PROPERTIES).asPropertyList()) {
                                    writeProperty(writer, dataSourceNode, connectionProperty.getName(), connectionProperty
                                            .getValue().asString(),
                                            org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY
                                                    .getLocalName());
                                }
                            }
                            writer.writeEndElement();
                        }
                        writer.writeEndElement();
                    }

                    boolean recoveryRequired = hasAnyOf(dataSourceNode, RECOVERY_USERNAME, RECOVERY_PASSWORD,
                            RECOVERY_SECURITY_DOMAIN, RECOVERLUGIN_CLASSNAME, NO_RECOVERY, RECOVERLUGIN_PROPERTIES);
                    if (recoveryRequired) {
                        writer.writeStartElement(XaDataSource.Tag.RECOVERY.getLocalName());
                        writeAttributeIfHas(writer, dataSourceNode, Recovery.Attribute.NO_RECOVERY, NO_RECOVERY);
                        if (hasAnyOf(dataSourceNode, RECOVERY_USERNAME, RECOVERY_PASSWORD, RECOVERY_SECURITY_DOMAIN)) {
                            writer.writeStartElement(Recovery.Tag.RECOVER_CREDENTIAL.getLocalName());
                            writeElementIfHas(writer, dataSourceNode, Credential.Tag.USERNAME.getLocalName(), RECOVERY_USERNAME);
                            writeElementIfHas(writer, dataSourceNode, Credential.Tag.PASSWORD.getLocalName(), RECOVERY_PASSWORD);
                            writeElementIfHas(writer, dataSourceNode, Credential.Tag.SECURITY_DOMAIN.getLocalName(),
                                    RECOVERY_SECURITY_DOMAIN);
                            writer.writeEndElement();
                        }
                        if (hasAnyOf(dataSourceNode, RECOVERLUGIN_CLASSNAME)) {
                            writer.writeStartElement(Recovery.Tag.RECOVER_PLUGIN.getLocalName());
                            writer.writeAttribute(
                                    org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(),
                                    dataSourceNode.get(RECOVERLUGIN_CLASSNAME).asString());
                            if (dataSourceNode.hasDefined(RECOVERLUGIN_PROPERTIES)) {
                                for (Property connectionProperty : dataSourceNode.get(RECOVERLUGIN_PROPERTIES).asPropertyList()) {
                                    writeProperty(writer, dataSourceNode, connectionProperty.getName(), connectionProperty
                                            .getValue().asString(),
                                            org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY
                                                    .getLocalName());
                                }
                            }
                            writer.writeEndElement();

                        }
                        writer.writeEndElement();
                    }

                    boolean validationRequired = hasAnyOf(dataSourceNode, VALIDCONNECTIONCHECKERCLASSNAME,
                            VALIDCONNECTIONCHECKER_PROPERTIES, CHECKVALIDCONNECTIONSQL, VALIDATEONMATCH, BACKGROUNDVALIDATION,
                            BACKGROUNDVALIDATIONMINUTES, USE_FAST_FAIL, STALECONNECTIONCHECKERCLASSNAME,
                            STALECONNECTIONCHECKER_PROPERTIES, EXCEPTIONSORTERCLASSNAME, EXCEPTIONSORTER_PROPERTIES);
                    if (validationRequired) {
                        writer.writeStartElement(DataSource.Tag.VALIDATION.getLocalName());
                        if (dataSourceNode.hasDefined(VALIDCONNECTIONCHECKERCLASSNAME)) {
                            writer.writeStartElement(Validation.Tag.VALIDCONNECTIONCHECKER.getLocalName());
                            writer.writeAttribute(
                                    org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(),
                                    dataSourceNode.get(VALIDCONNECTIONCHECKERCLASSNAME).asString());

                            if (dataSourceNode.hasDefined(VALIDCONNECTIONCHECKER_PROPERTIES)) {
                                for (Property connectionProperty : dataSourceNode.get(VALIDCONNECTIONCHECKER_PROPERTIES)
                                        .asPropertyList()) {
                                    writeProperty(writer, dataSourceNode, connectionProperty.getName(), connectionProperty
                                            .getValue().asString(),
                                            org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY
                                                    .getLocalName());
                                }
                            }
                            writer.writeEndElement();
                        }
                        writeElementIfHas(writer, dataSourceNode, Validation.Tag.CHECKVALIDCONNECTIONSQL,
                                CHECKVALIDCONNECTIONSQL);
                        writeElementIfHas(writer, dataSourceNode, Validation.Tag.VALIDATEONMATCH, VALIDATEONMATCH);
                        writeElementIfHas(writer, dataSourceNode, Validation.Tag.BACKGROUNDVALIDATION, BACKGROUNDVALIDATION);
                        writeElementIfHas(writer, dataSourceNode, Validation.Tag.BACKGROUNDVALIDATIONMINUTES,
                                BACKGROUNDVALIDATIONMINUTES);
                        writeElementIfHas(writer, dataSourceNode, Validation.Tag.USEFASTFAIL, USE_FAST_FAIL);
                        if (dataSourceNode.hasDefined(STALECONNECTIONCHECKERCLASSNAME)) {
                            writer.writeStartElement(Validation.Tag.STALECONNECTIONCHECKER.getLocalName());
                            writer.writeAttribute(
                                    org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(),
                                    dataSourceNode.get(STALECONNECTIONCHECKERCLASSNAME).asString());

                            if (dataSourceNode.hasDefined(STALECONNECTIONCHECKER_PROPERTIES)) {

                                for (Property connectionProperty : dataSourceNode.get(STALECONNECTIONCHECKER_PROPERTIES)
                                        .asPropertyList()) {
                                    writeProperty(writer, dataSourceNode, connectionProperty.getName(), connectionProperty
                                            .getValue().asString(),
                                            org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY
                                                    .getLocalName());
                                }
                            }
                            writer.writeEndElement();
                        }
                        if (dataSourceNode.hasDefined(EXCEPTIONSORTERCLASSNAME)) {
                            writer.writeStartElement(Validation.Tag.EXCEPTIONSORTER.getLocalName());
                            writer.writeAttribute(
                                    org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(),
                                    dataSourceNode.get(EXCEPTIONSORTERCLASSNAME).asString());
                            if (dataSourceNode.hasDefined(EXCEPTIONSORTER_PROPERTIES)) {
                                for (Property connectionProperty : dataSourceNode.get(EXCEPTIONSORTER_PROPERTIES)
                                        .asPropertyList()) {
                                    writeProperty(writer, dataSourceNode, connectionProperty.getName(), connectionProperty
                                            .getValue().asString(),
                                            org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY
                                                    .getLocalName());
                                }
                            }
                            writer.writeEndElement();
                        }
                        writer.writeEndElement();
                    }
                    boolean timeoutRequired = hasAnyOf(dataSourceNode, BLOCKING_TIMEOUT_WAIT_MILLIS, IDLETIMEOUTMINUTES,
                            SETTXQUERYTIMEOUT, QUERYTIMEOUT, USETRYLOCK, ALLOCATION_RETRY, ALLOCATION_RETRY_WAIT_MILLIS,
                            XA_RESOURCE_TIMEOUT);
                    if (timeoutRequired) {
                        writer.writeStartElement(DataSource.Tag.TIMEOUT.getLocalName());
                        writeElementIfHas(writer, dataSourceNode, TimeOut.Tag.BLOCKINGTIMEOUTMILLIS,
                                BLOCKING_TIMEOUT_WAIT_MILLIS);
                        writeElementIfHas(writer, dataSourceNode, TimeOut.Tag.IDLETIMEOUTMINUTES, IDLETIMEOUTMINUTES);
                        writeEmptyElementIfHasAndTrue(writer, dataSourceNode, TimeOut.Tag.SETTXQUERYTIMEOUT, SETTXQUERYTIMEOUT);
                        writeElementIfHas(writer, dataSourceNode, TimeOut.Tag.QUERYTIMEOUT, QUERYTIMEOUT);
                        writeElementIfHas(writer, dataSourceNode, TimeOut.Tag.USETRYLOCK, USETRYLOCK);
                        writeElementIfHas(writer, dataSourceNode, TimeOut.Tag.ALLOCATIONRETRY, ALLOCATION_RETRY);
                        writeElementIfHas(writer, dataSourceNode, TimeOut.Tag.ALLOCATIONRETRYWAITMILLIS,
                                ALLOCATION_RETRY_WAIT_MILLIS);
                        writeElementIfHas(writer, dataSourceNode, TimeOut.Tag.XARESOURCETIMEOUT, XA_RESOURCE_TIMEOUT);
                        writer.writeEndElement();
                    }
                    boolean statementRequired = hasAnyOf(dataSourceNode, TRACKSTATEMENTS, PREPAREDSTATEMENTSCACHESIZE,
                            SHAREPREPAREDSTATEMENTS);
                    if (statementRequired) {
                        writer.writeStartElement(DataSource.Tag.STATEMENT.getLocalName());
                        writeElementIfHas(writer, dataSourceNode, Statement.Tag.TRACKSTATEMENTS, TRACKSTATEMENTS);
                        writeElementIfHas(writer, dataSourceNode, Statement.Tag.PREPAREDSTATEMENTCACHESIZE,
                                PREPAREDSTATEMENTSCACHESIZE);
                        writeEmptyElementIfHasAndTrue(writer, dataSourceNode, Statement.Tag.SHAREPREPAREDSTATEMENTS,
                                SHAREPREPAREDSTATEMENTS);

                        writer.writeEndElement();
                    }

                    writer.writeEndElement();
                }
            }
            if (node.hasDefined(JDBC_DRIVER)) {
                writer.writeStartElement(DataSources.Tag.DRIVERS.getLocalName());
                for (Property driverProperty : node.get(JDBC_DRIVER).asPropertyList()) {
                    writer.writeStartElement(DataSources.Tag.DRIVER.getLocalName());
                    writer.writeAttribute(Driver.Attribute.NAME.getLocalName(), driverProperty.getValue().require(DRIVER_NAME)
                            .asString());
                    writeAttributeIfHas(writer, driverProperty.getValue(), Driver.Attribute.MODULE, DRIVER_MODULE_NAME);
                    writeAttributeIfHas(writer, driverProperty.getValue(), Driver.Attribute.MAJOR_VERSION, DRIVER_MAJOR_VERSION);
                    writeAttributeIfHas(writer, driverProperty.getValue(), Driver.Attribute.MINOR_VERSION, DRIVER_MINOR_VERSION);
                    writeElementIfHas(writer, driverProperty.getValue(), Driver.Tag.DRIVERCLASS.getLocalName(),
                            DRIVER_CLASS_NAME);
                    writeElementIfHas(writer, driverProperty.getValue(), Driver.Tag.XADATASOURCECLASS.getLocalName(),
                            DRIVER_XA_DATASOURCE_CLASS_NAME);

                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
            writer.writeEndElement();
        }

        private void writeAttributeIfHas(final XMLExtendedStreamWriter writer, final ModelNode node,
                final Recovery.Attribute attr, final String identifier) throws XMLStreamException {
            if (has(node, identifier)) {
                writer.writeAttribute(attr.getLocalName(), node.get(identifier).asString());
            }
        }

        private void writeAttributeIfHas(final XMLExtendedStreamWriter writer, final ModelNode node,
                final Driver.Attribute attr, final String identifier) throws XMLStreamException {
            if (has(node, identifier)) {
                writer.writeAttribute(attr.getLocalName(), node.get(identifier).asString());
            }
        }

        private void writeAttributeIfHas(final XMLExtendedStreamWriter writer, final ModelNode node,
                final DataSource.Attribute attr, final String identifier) throws XMLStreamException {
            if (has(node, identifier)) {
                writer.writeAttribute(attr.getLocalName(), node.get(identifier).asString());
            }
        }

        private void writeProperty(XMLExtendedStreamWriter writer, ModelNode node, String name, String value, String localName)
                throws XMLStreamException {

            writer.writeStartElement(localName);
            writer.writeAttribute("name", name);
            writer.writeCharacters(value);
            writer.writeEndElement();

        }

        private void writeElementIfHas(XMLExtendedStreamWriter writer, ModelNode node, String localName, String identifier)
                throws XMLStreamException {
            if (has(node, identifier)) {
                writer.writeStartElement(localName);
                writer.writeCharacters(node.get(identifier).asString());
                writer.writeEndElement();
            }
        }

        private void writeElementIfHas(XMLExtendedStreamWriter writer, ModelNode node, XaDataSource.Tag element,
                String identifier) throws XMLStreamException {
            writeElementIfHas(writer, node, element.getLocalName(), identifier);
        }

        private void writeElementIfHas(XMLExtendedStreamWriter writer, ModelNode node, DataSource.Tag element, String identifier)
                throws XMLStreamException {
            writeElementIfHas(writer, node, element.getLocalName(), identifier);
        }

        private void writeElementIfHas(XMLExtendedStreamWriter writer, ModelNode node, DsSecurity.Tag element, String identifier)
                throws XMLStreamException {
            writeElementIfHas(writer, node, element.getLocalName(), identifier);
        }

        private void writeElementIfHas(XMLExtendedStreamWriter writer, ModelNode node, CommonPool.Tag element, String identifier)
                throws XMLStreamException {
            writeElementIfHas(writer, node, element.getLocalName(), identifier);
        }

        private void writeElementIfHas(XMLExtendedStreamWriter writer, ModelNode node, CommonXaPool.Tag element,
                String identifier) throws XMLStreamException {
            writeElementIfHas(writer, node, element.getLocalName(), identifier);
        }

        private void writeElementIfHas(XMLExtendedStreamWriter writer, ModelNode node, TimeOut.Tag element, String identifier)
                throws XMLStreamException {
            writeElementIfHas(writer, node, element.getLocalName(), identifier);
        }

        private void writeElementIfHas(XMLExtendedStreamWriter writer, ModelNode node, Validation.Tag element, String identifier)
                throws XMLStreamException {
            writeElementIfHas(writer, node, element.getLocalName(), identifier);
        }

        private void writeElementIfHas(XMLExtendedStreamWriter writer, ModelNode node, Statement.Tag element, String identifier)
                throws XMLStreamException {
            writeElementIfHas(writer, node, element.getLocalName(), identifier);
        }

        private void writeEmptyElementIfHasAndTrue(XMLExtendedStreamWriter writer, ModelNode node, String localName,
                String identifier) throws XMLStreamException {
            if (node.has(identifier) && node.get(identifier).asBoolean()) {
                writer.writeEmptyElement(localName);
            }
        }

        private void writeEmptyElementIfHasAndTrue(XMLExtendedStreamWriter writer, ModelNode node, Statement.Tag element,
                String identifier) throws XMLStreamException {
            writeEmptyElementIfHasAndTrue(writer, node, element.getLocalName(), identifier);
        }

        private void writeEmptyElementIfHasAndTrue(XMLExtendedStreamWriter writer, ModelNode node, CommonXaPool.Tag element,
                String identifier) throws XMLStreamException {
            writeEmptyElementIfHasAndTrue(writer, node, element.getLocalName(), identifier);
        }

        private void writeEmptyElementIfHasAndTrue(XMLExtendedStreamWriter writer, ModelNode node, TimeOut.Tag element,
                String identifier) throws XMLStreamException {
            writeEmptyElementIfHasAndTrue(writer, node, element.getLocalName(), identifier);
        }

        private boolean hasAnyOf(ModelNode node, String... names) {
            for (String current : names) {
                if (has(node, current)) {
                    return true;
                }
            }
            return false;
        }

        private boolean has(ModelNode node, String name) {
            return node.has(name) && node.get(name).isDefined();
        }

        @Override
        public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {

            final ModelNode address = new ModelNode();
            address.add(ModelDescriptionConstants.SUBSYSTEM, DATASOURCES);
            address.protect();

            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ADD);
            subsystem.get(OP_ADDR).set(address);

            list.add(subsystem);

            DataSources dataSources = null;
            try {
                String localName = null;
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case DATASOURCES_1_0: {
                        localName = reader.getLocalName();
                        Element element = Element.forName(reader.getLocalName());
                        log.tracef("%s -> %s", localName, element);
                        switch (element) {
                            case SUBSYSTEM: {

                                final DsParser parser = new DsParser();
                                dataSources = parser.parse(reader);
                                requireNoContent(reader);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new XMLStreamException(e);
            }

            if (dataSources != null) {
                for (DataSource dataSource : dataSources.getDataSource()) {
                    final ModelNode dsAddress = address.clone();
                    dsAddress.add(DATA_SOURCE, dataSource.getJndiName());
                    dsAddress.protect();

                    final ModelNode operation = new ModelNode();
                    operation.get(OP_ADDR).set(dsAddress);
                    operation.get(OP).set(ADD);

                    fillFrom(operation, dataSource);
                    list.add(operation);
                }

                for (XaDataSource xaDataSource : dataSources.getXaDataSource()) {
                    final ModelNode dsAddress = address.clone();
                    dsAddress.add(XA_DATA_SOURCE, xaDataSource.getJndiName());
                    dsAddress.protect();

                    final ModelNode operation = new ModelNode();
                    operation.get(OP_ADDR).set(dsAddress);
                    operation.get(OP).set(ADD);

                    fillFrom(operation, xaDataSource);
                    list.add(operation);
                }

                for (Driver driver : dataSources.getDrivers()) {
                    final ModelNode driverAddress = address.clone();
                    driverAddress.add(JDBC_DRIVER, driver.getName());
                    driverAddress.protect();
                    final ModelNode op = Util.getEmptyOperation(ADD, driverAddress);

                    op.get(DRIVER_NAME).set(driver.getName());
                    op.get(DRIVER_MODULE_NAME).set(driver.getModule());
                    if (driver.getMajorVersion() != null)
                        op.get(DRIVER_MAJOR_VERSION).set(driver.getMajorVersion());
                    if (driver.getMinorVersion() != null)
                        op.get(DRIVER_MINOR_VERSION).set(driver.getMinorVersion());
                    if (driver.getDriverClass() != null)
                        op.get(DRIVER_CLASS_NAME).set(driver.getDriverClass());
                    if (driver.getXaDataSourceClass() != null)
                        op.get(DRIVER_XA_DATASOURCE_CLASS_NAME).set(driver.getXaDataSourceClass());

                    list.add(op);
                }
            }
        }

    }

    private static class DataSourcesSubsystemDescribeHandler implements OperationStepHandler, DescriptionProvider {
        static final DataSourcesSubsystemDescribeHandler INSTANCE = new DataSourcesSubsystemDescribeHandler();

        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final ModelNode result = context.getResult();
            final PathAddress rootAddress = PathAddress.pathAddress(PathAddress.pathAddress(operation.require(OP_ADDR))
                    .getLastElement());
            final ModelNode subModel = context.readModel(PathAddress.EMPTY_ADDRESS);

            final ModelNode subsystemAdd = new ModelNode();
            subsystemAdd.get(OP).set(ADD);
            subsystemAdd.get(OP_ADDR).set(rootAddress.toModelNode());

            result.add(subsystemAdd);

            if (subModel.hasDefined(JDBC_DRIVER)) {
                for (final Property jdbcDriver : subModel.get(Constants.JDBC_DRIVER).asPropertyList()) {
                    final ModelNode address = rootAddress.toModelNode();
                    address.add(Constants.JDBC_DRIVER, jdbcDriver.getName());
                    final ModelNode addOperation = Util.getEmptyOperation(ADD, address);
                    addOperation.get(DRIVER_NAME).set(jdbcDriver.getValue().get(DRIVER_NAME));
                    addOperation.get(DRIVER_MODULE_NAME).set(jdbcDriver.getValue().get(DRIVER_MODULE_NAME));
                    addOperation.get(DRIVER_MAJOR_VERSION).set(jdbcDriver.getValue().get(DRIVER_MAJOR_VERSION));
                    addOperation.get(DRIVER_MINOR_VERSION).set(jdbcDriver.getValue().get(DRIVER_MINOR_VERSION));
                    addOperation.get(DRIVER_CLASS_NAME).set(jdbcDriver.getValue().get(DRIVER_CLASS_NAME));
                    addOperation.get(DRIVER_XA_DATASOURCE_CLASS_NAME).set(
                            jdbcDriver.getValue().get(DRIVER_XA_DATASOURCE_CLASS_NAME));
                    result.add(addOperation);
                }
            }

            if (subModel.hasDefined(DATA_SOURCE)) {
                for (final Property dataSourceProp : subModel.get(Constants.DATA_SOURCE).asPropertyList()) {
                    final ModelNode address = rootAddress.toModelNode();
                    address.add(Constants.DATA_SOURCE, dataSourceProp.getName());
                    final ModelNode addOperation = Util.getEmptyOperation(ADD, address);
                    final ModelNode dataSource = dataSourceProp.getValue();

                    populateAddModel(dataSource, addOperation, CONNECTION_PROPERTIES, DATASOURCE_ATTRIBUTE);

                    addOperation.get(DATASOURCE_DRIVER).set(dataSourceProp.getValue().get(DATASOURCE_DRIVER));
                    result.add(addOperation);
                }
            }

            if (subModel.hasDefined(XA_DATA_SOURCE)) {
                for (final Property dataSourceProp : subModel.get(Constants.XA_DATA_SOURCE).asPropertyList()) {
                    final ModelNode address = rootAddress.toModelNode();
                    address.add(Constants.XA_DATA_SOURCE, dataSourceProp.getName());
                    final ModelNode addOperation = Util.getEmptyOperation(ADD, address);
                    final ModelNode dataSource = dataSourceProp.getValue();

                    populateAddModel(dataSource, addOperation, XADATASOURCEPROPERTIES, XA_DATASOURCE_ATTRIBUTE);

                    addOperation.get(DATASOURCE_DRIVER).set(dataSourceProp.getValue().get(DATASOURCE_DRIVER));
                    result.add(addOperation);
                }
            }

            context.completeStep();
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }
    }

}

