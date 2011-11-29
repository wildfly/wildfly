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

import static org.jboss.as.connector.ConnectorLogger.SUBSYSTEM_DATASOURCES_LOGGER;
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
import static org.jboss.as.connector.subsystems.datasources.AbstractDataSourceAdd.populateAddModel;
import static org.jboss.as.connector.subsystems.datasources.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.datasources.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.datasources.Constants.CHECKVALIDCONNECTIONSQL;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_URL;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCES;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_CLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_DRIVER;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATA_SOURCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_CLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_DATASOURCE_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_MAJOR_VERSION;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_MINOR_VERSION;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_MODULE_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_XA_DATASOURCE_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.EXCEPTIONSORTERCLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.EXCEPTIONSORTER_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.INTERLEAVING;
import static org.jboss.as.connector.subsystems.datasources.Constants.JDBC_DRIVER_NAME;
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
import static org.jboss.as.connector.subsystems.datasources.Constants.XADATASOURCE_PROPERTY_VALUE;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_DATASOURCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_RESOURCE_TIMEOUT;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.ADD_CONNECTION_PROPERTIES_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.ADD_DATA_SOURCE_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.ADD_JDBC_DRIVER_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.ADD_XADATASOURCE_PROPERTIES_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.ADD_XA_DATA_SOURCE_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.CONNECTION_PROPERTIES_DESC;
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
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.REMOVE_CONNECTION_PROPERTIES_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.REMOVE_DATA_SOURCE_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.REMOVE_JDBC_DRIVER_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.REMOVE_XADATASOURCE_PROPERTIES_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.REMOVE_XA_DATA_SOURCE_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.SUBSYSTEM;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.SUBSYSTEM_ADD_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.TEST_CONNECTION_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.XADATASOURCE_PROPERTIES_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.XA_DATASOURCE_ATTRIBUTE;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.XA_DATA_SOURCE_DESC;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DISABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PERSISTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
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
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
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
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.metadata.ds.DataSources;
import org.jboss.jca.common.api.metadata.ds.Driver;
import org.jboss.jca.common.api.metadata.ds.DsSecurity;
import org.jboss.jca.common.api.metadata.ds.Statement;
import org.jboss.jca.common.api.metadata.ds.TimeOut;
import org.jboss.jca.common.api.metadata.ds.Validation;
import org.jboss.jca.common.api.metadata.ds.XaDataSource;
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

    public static final String SUBSYSTEM_NAME = Constants.DATASOURCES;

    @Override
    public void initialize(final ExtensionContext context) {
        SUBSYSTEM_DATASOURCES_LOGGER.debugf("Initializing Datasources Extension");

        // Register the remoting subsystem
        final SubsystemRegistration registration = context.registerSubsystem(SUBSYSTEM_NAME);

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

        final ManagementResourceRegistration jdbcDrivers = subsystem.registerSubModel(PathElement.pathElement(JDBC_DRIVER_NAME),
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

        final ManagementResourceRegistration configAdapter = dataSources.registerSubModel(PathElement.pathElement(CONNECTION_PROPERTIES.getName()), CONNECTION_PROPERTIES_DESC);
        configAdapter.registerOperationHandler(ADD, ConnectionPropertyAdd.INSTANCE, ADD_CONNECTION_PROPERTIES_DESC, false);
        configAdapter.registerOperationHandler(REMOVE, ConnectionPropertyRemove.INSTANCE, REMOVE_CONNECTION_PROPERTIES_DESC, false);

        for (final String attributeName : PoolMetrics.ATTRIBUTES) {
            dataSources.registerMetric(attributeName, PoolMetrics.LocalAndXaDataSourcePoolMetricsHandler.INSTANCE);

        }

        for (final String attributeName : LocalAndXaDataSourcesJdbcMetrics.ATTRIBUTES) {
            dataSources.registerMetric(attributeName, LocalAndXaDataSourcesJdbcMetrics.INSTANCE);

        }

        for (final SimpleAttributeDefinition attribute : DataSourcesSubsystemProviders.DATASOURCE_ATTRIBUTE) {
            if (PoolConfigurationRWHandler.ATTRIBUTES.contains(attribute.getName())) {
               dataSources.registerReadWriteAttribute(attribute.getName(), PoolConfigurationReadHandler.INSTANCE,
                    LocalAndXaDataSourcePoolConfigurationWriteHandler.INSTANCE, Storage.CONFIGURATION);
            } else {
               dataSources.registerReadWriteAttribute(attribute.getName(), null, new DisableRequiredWriteAttributeHandler(DATASOURCE_ATTRIBUTE) , Storage.CONFIGURATION);
            }
        }

        final ManagementResourceRegistration xaDataSources = subsystem.registerSubModel(PathElement.pathElement(XA_DATASOURCE),
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

        final ManagementResourceRegistration xadatasourcePropertyAdapter = xaDataSources.registerSubModel(PathElement.pathElement(XADATASOURCE_PROPERTIES.getName()), XADATASOURCE_PROPERTIES_DESC);
        xadatasourcePropertyAdapter.registerOperationHandler(ADD, XaDataSourcePropertyAdd.INSTANCE, ADD_XADATASOURCE_PROPERTIES_DESC, false);
        xadatasourcePropertyAdapter.registerOperationHandler(REMOVE, XaDataSourcePropertyRemove.INSTANCE, REMOVE_XADATASOURCE_PROPERTIES_DESC, false);


        for (final String attributeName : PoolMetrics.ATTRIBUTES) {
            xaDataSources.registerMetric(attributeName, PoolMetrics.LocalAndXaDataSourcePoolMetricsHandler.INSTANCE);
        }

        for (final String attributeName : LocalAndXaDataSourcesJdbcMetrics.ATTRIBUTES) {
            xaDataSources.registerMetric(attributeName, LocalAndXaDataSourcesJdbcMetrics.INSTANCE);

        }

        for (final SimpleAttributeDefinition attribute : DataSourcesSubsystemProviders.XA_DATASOURCE_ATTRIBUTE) {
            if (PoolConfigurationRWHandler.ATTRIBUTES.contains(attribute.getName())) {
               xaDataSources.registerReadWriteAttribute(attribute.getName(), PoolConfigurationReadHandler.INSTANCE,
                    LocalAndXaDataSourcePoolConfigurationWriteHandler.INSTANCE, Storage.CONFIGURATION);
            } else {
               xaDataSources.registerReadWriteAttribute(attribute.getName(), null, new DisableRequiredWriteAttributeHandler(XA_DATASOURCE_ATTRIBUTE) , Storage.CONFIGURATION);
            }
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

            if (node.hasDefined(DATA_SOURCE) || node.hasDefined(XA_DATASOURCE)) {
                boolean isXADataSource = false;

                if (node.hasDefined(DATA_SOURCE)) {
                    writeDS(writer, false, node.get(DATA_SOURCE).asPropertyList());
                }
                if (node.hasDefined(XA_DATASOURCE)) {
                    writeDS(writer, true, node.get(XA_DATASOURCE).asPropertyList());
                }

            }

            if (node.hasDefined(JDBC_DRIVER_NAME)) {
                writer.writeStartElement(DataSources.Tag.DRIVERS.getLocalName());
                for (Property driverProperty : node.get(JDBC_DRIVER_NAME).asPropertyList()) {
                    writer.writeStartElement(DataSources.Tag.DRIVER.getLocalName());
                    writer.writeAttribute(Driver.Attribute.NAME.getLocalName(), driverProperty.getValue().require(DRIVER_NAME.getName()).asString());
                    writeAttributeIfHas(writer, driverProperty.getValue(), Driver.Attribute.MODULE, DRIVER_MODULE_NAME.getName());
                    writeAttributeIfHas(writer, driverProperty.getValue(), Driver.Attribute.MAJOR_VERSION, DRIVER_MAJOR_VERSION.getName());
                    writeAttributeIfHas(writer, driverProperty.getValue(), Driver.Attribute.MINOR_VERSION, DRIVER_MINOR_VERSION.getName());
                    writeElementIfHas(writer, driverProperty.getValue(), Driver.Tag.DRIVER_CLASS.getLocalName(), DRIVER_CLASS_NAME.getName());
                    writeElementIfHas(writer, driverProperty.getValue(), Driver.Tag.XA_DATASOURCE_CLASS.getLocalName(), DRIVER_XA_DATASOURCE_CLASS_NAME.getName());

                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }

            writer.writeEndElement();
            writer.writeEndElement();
        }

        private void writeDS(XMLExtendedStreamWriter writer, boolean isXADataSource, List<Property> propertyList) throws XMLStreamException {
            for (Property property : propertyList) {
                final ModelNode dataSourceNode = property.getValue();

                writer.writeStartElement(isXADataSource ? DataSources.Tag.XA_DATASOURCE.getLocalName()
                        : DataSources.Tag.DATASOURCE.getLocalName());
                JNDINAME.marshallAsAttribute(dataSourceNode, writer);
                writer.writeAttribute("pool-name", property.getName());
                ENABLED.marshallAsAttribute(dataSourceNode, writer);
                JTA.marshallAsAttribute(dataSourceNode, writer);
                USE_JAVA_CONTEXT.marshallAsAttribute(dataSourceNode, writer);
                SPY.marshallAsAttribute(dataSourceNode, writer);
                USE_CCM.marshallAsAttribute(dataSourceNode, writer);

                if (!isXADataSource) {
                    CONNECTION_URL.marshallAsElement(dataSourceNode, writer);
                    DRIVER_CLASS.marshallAsElement(dataSourceNode, writer);
                    DATASOURCE_CLASS.marshallAsElement(dataSourceNode, writer);
                    if (dataSourceNode.hasDefined(CONNECTION_PROPERTIES.getName())) {
                        for (Property connectionProperty : dataSourceNode.get(CONNECTION_PROPERTIES.getName()).asPropertyList()) {
                            writeProperty(writer, dataSourceNode, connectionProperty.getName(), connectionProperty
                                    .getValue().get("value").asString(), DataSource.Tag.CONNECTION_PROPERTY.getLocalName());
                        }
                    }
                }
                if (isXADataSource) {
                    if (dataSourceNode.hasDefined(XADATASOURCE_PROPERTIES.getName())) {
                        for (Property prop : dataSourceNode.get(XADATASOURCE_PROPERTIES.getName()).asPropertyList()) {
                            writeProperty(writer, dataSourceNode, prop.getName(), prop
                                    .getValue().get("value").asString(), XaDataSource.Tag.XA_DATASOURCE_PROPERTY.getLocalName());
                        }

                    }
                    XADATASOURCECLASS.marshallAsElement(dataSourceNode, writer);

                }
                DATASOURCE_DRIVER.marshallAsElement(dataSourceNode, writer);

                if (isXADataSource) {
                    URL_DELIMITER.marshallAsElement(dataSourceNode, writer);
                    URL_SELECTOR_STRATEGY_CLASS_NAME.marshallAsElement(dataSourceNode, writer);
                }
                NEW_CONNECTION_SQL.marshallAsElement(dataSourceNode, writer);
                TRANSACTION_ISOLATION.marshallAsElement(dataSourceNode, writer);

                if (!isXADataSource) {
                    URL_DELIMITER.marshallAsElement(dataSourceNode, writer);
                    URL_SELECTOR_STRATEGY_CLASS_NAME.marshallAsElement(dataSourceNode, writer);
                }
                boolean poolRequired = MIN_POOL_SIZE.isMarshallable(dataSourceNode) ||
                        MAX_POOL_SIZE.isMarshallable(dataSourceNode) ||
                        POOL_PREFILL.isMarshallable(dataSourceNode) ||
                        POOL_USE_STRICT_MIN.isMarshallable(dataSourceNode) ||
                        POOL_FLUSH_STRATEGY.isMarshallable(dataSourceNode);
                if (isXADataSource) {
                    poolRequired = poolRequired
                            || SAME_RM_OVERRIDE.isMarshallable(dataSourceNode) ||
                            INTERLEAVING.isMarshallable(dataSourceNode) ||
                            NOTXSEPARATEPOOL.isMarshallable(dataSourceNode) ||
                            PAD_XID.isMarshallable(dataSourceNode) ||
                            WRAP_XA_RESOURCE.isMarshallable(dataSourceNode);
                }
                if (poolRequired) {
                    writer.writeStartElement(isXADataSource ? XaDataSource.Tag.XA_POOL.getLocalName() : DataSource.Tag.POOL
                            .getLocalName());
                    MIN_POOL_SIZE.marshallAsElement(dataSourceNode, writer);
                    MAX_POOL_SIZE.marshallAsElement(dataSourceNode, writer);
                    POOL_PREFILL.marshallAsElement(dataSourceNode, writer);
                    POOL_USE_STRICT_MIN.marshallAsElement(dataSourceNode, writer);
                    POOL_FLUSH_STRATEGY.marshallAsElement(dataSourceNode, writer);

                    if (isXADataSource) {
                        SAME_RM_OVERRIDE.marshallAsElement(dataSourceNode, writer);
                        INTERLEAVING.marshallAsElement(dataSourceNode, writer);
                        NOTXSEPARATEPOOL.marshallAsElement(dataSourceNode, writer);
                        PAD_XID.marshallAsElement(dataSourceNode, writer);
                        WRAP_XA_RESOURCE.marshallAsElement(dataSourceNode, writer);
                    }
                    writer.writeEndElement();
                }
                boolean securityRequired = USERNAME.isMarshallable(dataSourceNode) ||
                        PASSWORD.isMarshallable(dataSourceNode) ||
                        SECURITY_DOMAIN.isMarshallable(dataSourceNode) ||
                        REAUTHPLUGIN_CLASSNAME.isMarshallable(dataSourceNode) ||
                        REAUTHPLUGIN_PROPERTIES.isMarshallable(dataSourceNode);
                if (securityRequired) {
                    writer.writeStartElement(DataSource.Tag.SECURITY.getLocalName());
                    USERNAME.marshallAsElement(dataSourceNode, writer);
                    PASSWORD.marshallAsElement(dataSourceNode, writer);
                    SECURITY_DOMAIN.marshallAsElement(dataSourceNode, writer);

                    if (dataSourceNode.hasDefined(REAUTHPLUGIN_CLASSNAME.getName())) {
                        writer.writeStartElement(DsSecurity.Tag.REAUTH_PLUGIN.getLocalName());
                        writer.writeAttribute(
                                org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(),
                                dataSourceNode.get(REAUTHPLUGIN_CLASSNAME.getName()).asString());

                        if (dataSourceNode.hasDefined(REAUTHPLUGIN_PROPERTIES.getName())) {
                            for (Property connectionProperty : dataSourceNode.get(REAUTHPLUGIN_PROPERTIES.getName()).asPropertyList()) {
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

                boolean recoveryRequired = RECOVERY_USERNAME.isMarshallable(dataSourceNode) ||
                        RECOVERY_PASSWORD.isMarshallable(dataSourceNode) ||
                        RECOVERY_SECURITY_DOMAIN.isMarshallable(dataSourceNode) ||
                        RECOVERLUGIN_CLASSNAME.isMarshallable(dataSourceNode) ||
                        NO_RECOVERY.isMarshallable(dataSourceNode) ||
                        RECOVERLUGIN_PROPERTIES.isMarshallable(dataSourceNode);
                if (recoveryRequired && isXADataSource) {
                    writer.writeStartElement(XaDataSource.Tag.RECOVERY.getLocalName());
                    NO_RECOVERY.marshallAsAttribute(dataSourceNode, writer);
                    if (hasAnyOf(dataSourceNode, RECOVERY_USERNAME, RECOVERY_PASSWORD, RECOVERY_SECURITY_DOMAIN)) {
                        writer.writeStartElement(Recovery.Tag.RECOVER_CREDENTIAL.getLocalName());
                        RECOVERY_USERNAME.marshallAsElement(dataSourceNode, writer);
                        RECOVERY_PASSWORD.marshallAsElement(dataSourceNode, writer);
                        RECOVERY_SECURITY_DOMAIN.marshallAsElement(dataSourceNode, writer);
                        writer.writeEndElement();
                    }
                    if (hasAnyOf(dataSourceNode, RECOVERLUGIN_CLASSNAME)) {
                        writer.writeStartElement(Recovery.Tag.RECOVER_PLUGIN.getLocalName());
                        writer.writeAttribute(
                                org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(),
                                dataSourceNode.get(RECOVERLUGIN_CLASSNAME.getName()).asString());
                        if (dataSourceNode.hasDefined(RECOVERLUGIN_PROPERTIES.getName())) {
                            for (Property connectionProperty : dataSourceNode.get(RECOVERLUGIN_PROPERTIES.getName()).asPropertyList()) {
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

                boolean validationRequired = VALIDCONNECTIONCHECKERCLASSNAME.isMarshallable(dataSourceNode) ||

                        VALIDCONNECTIONCHECKER_PROPERTIES.isMarshallable(dataSourceNode) ||
                        CHECKVALIDCONNECTIONSQL.isMarshallable(dataSourceNode) ||
                        VALIDATEONMATCH.isMarshallable(dataSourceNode) ||
                        BACKGROUNDVALIDATION.isMarshallable(dataSourceNode) ||
                        BACKGROUNDVALIDATIONMILLIS.isMarshallable(dataSourceNode) ||
                        USE_FAST_FAIL.isMarshallable(dataSourceNode) ||
                        STALECONNECTIONCHECKERCLASSNAME.isMarshallable(dataSourceNode) ||
                        STALECONNECTIONCHECKER_PROPERTIES.isMarshallable(dataSourceNode) ||
                        EXCEPTIONSORTERCLASSNAME.isMarshallable(dataSourceNode) ||
                        EXCEPTIONSORTER_PROPERTIES.isMarshallable(dataSourceNode);
                if (validationRequired) {
                    writer.writeStartElement(DataSource.Tag.VALIDATION.getLocalName());
                    if (dataSourceNode.hasDefined(VALIDCONNECTIONCHECKERCLASSNAME.getName())) {
                        writer.writeStartElement(Validation.Tag.VALID_CONNECTION_CHECKER.getLocalName());
                        writer.writeAttribute(
                                org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(),
                                dataSourceNode.get(VALIDCONNECTIONCHECKERCLASSNAME.getName()).asString());

                        if (dataSourceNode.hasDefined(VALIDCONNECTIONCHECKER_PROPERTIES.getName())) {
                            for (Property connectionProperty : dataSourceNode.get(VALIDCONNECTIONCHECKER_PROPERTIES.getName())
                                    .asPropertyList()) {
                                writeProperty(writer, dataSourceNode, connectionProperty.getName(), connectionProperty
                                        .getValue().asString(),
                                        org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY
                                                .getLocalName());
                            }
                        }
                        writer.writeEndElement();
                    }
                    CHECKVALIDCONNECTIONSQL.marshallAsElement(dataSourceNode, writer);
                    VALIDATEONMATCH.marshallAsElement(dataSourceNode, writer);
                    BACKGROUNDVALIDATION.marshallAsElement(dataSourceNode, writer);
                    BACKGROUNDVALIDATIONMILLIS.marshallAsElement(dataSourceNode, writer);
                    USE_FAST_FAIL.marshallAsElement(dataSourceNode, writer);
                    if (dataSourceNode.hasDefined(STALECONNECTIONCHECKERCLASSNAME.getName())) {
                        writer.writeStartElement(Validation.Tag.STALE_CONNECTION_CHECKER.getLocalName());
                        writer.writeAttribute(org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(),
                                dataSourceNode.get(STALECONNECTIONCHECKERCLASSNAME.getName()).asString());

                        if (dataSourceNode.hasDefined(STALECONNECTIONCHECKER_PROPERTIES.getName())) {

                            for (Property connectionProperty : dataSourceNode.get(STALECONNECTIONCHECKER_PROPERTIES.getName())
                                    .asPropertyList()) {
                                writeProperty(writer, dataSourceNode, connectionProperty.getName(), connectionProperty
                                        .getValue().asString(),
                                        org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY
                                                .getLocalName());
                            }
                        }
                        writer.writeEndElement();
                    }
                    if (dataSourceNode.hasDefined(EXCEPTIONSORTERCLASSNAME.getName())) {
                        writer.writeStartElement(Validation.Tag.EXCEPTION_SORTER.getLocalName());
                        writer.writeAttribute(
                                org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(),
                                dataSourceNode.get(EXCEPTIONSORTERCLASSNAME.getName()).asString());
                        if (dataSourceNode.hasDefined(EXCEPTIONSORTER_PROPERTIES.getName())) {
                            for (Property connectionProperty : dataSourceNode.get(EXCEPTIONSORTER_PROPERTIES.getName())
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
                boolean timeoutRequired = BLOCKING_TIMEOUT_WAIT_MILLIS.isMarshallable(dataSourceNode) ||
                        IDLETIMEOUTMINUTES.isMarshallable(dataSourceNode) ||
                        SETTXQUERYTIMEOUT.isMarshallable(dataSourceNode) ||
                        QUERYTIMEOUT.isMarshallable(dataSourceNode) ||
                        USETRYLOCK.isMarshallable(dataSourceNode) ||
                        ALLOCATION_RETRY.isMarshallable(dataSourceNode) ||
                        ALLOCATION_RETRY_WAIT_MILLIS.isMarshallable(dataSourceNode) ||
                        XA_RESOURCE_TIMEOUT.isMarshallable(dataSourceNode);
                if (timeoutRequired) {
                    writer.writeStartElement(DataSource.Tag.TIMEOUT.getLocalName());
                    SETTXQUERYTIMEOUT.marshallAsElement(dataSourceNode, writer);
                    BLOCKING_TIMEOUT_WAIT_MILLIS.marshallAsElement(dataSourceNode, writer);
                    IDLETIMEOUTMINUTES.marshallAsElement(dataSourceNode, writer);
                    QUERYTIMEOUT.marshallAsElement(dataSourceNode, writer);
                    USETRYLOCK.marshallAsElement(dataSourceNode, writer);
                    ALLOCATION_RETRY.marshallAsElement(dataSourceNode, writer);
                    ALLOCATION_RETRY_WAIT_MILLIS.marshallAsElement(dataSourceNode, writer);
                    XA_RESOURCE_TIMEOUT.marshallAsElement(dataSourceNode, writer);
                    writer.writeEndElement();
                }
                boolean statementRequired = hasAnyOf(dataSourceNode, TRACKSTATEMENTS, PREPAREDSTATEMENTSCACHESIZE, SHAREPREPAREDSTATEMENTS);
                if (statementRequired) {
                    writer.writeStartElement(DataSource.Tag.STATEMENT.getLocalName());
                    TRACKSTATEMENTS.marshallAsElement(dataSourceNode, writer);
                    PREPAREDSTATEMENTSCACHESIZE.marshallAsElement(dataSourceNode, writer);
                    SHAREPREPAREDSTATEMENTS.marshallAsElement(dataSourceNode, writer);

                    writer.writeEndElement();
                }

                writer.writeEndElement();
            }
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

        private boolean hasAnyOf(ModelNode node, SimpleAttributeDefinition... names) {
            for (SimpleAttributeDefinition current : names) {
                if (has(node, current.getName())) {
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

            try {
                String localName = null;
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case DATASOURCES_1_0: {
                        localName = reader.getLocalName();
                        Element element = Element.forName(reader.getLocalName());
                        SUBSYSTEM_DATASOURCES_LOGGER.tracef("%s -> %s", localName, element);
                        switch (element) {
                            case SUBSYSTEM: {

                                final DsParser parser = new DsParser();
                                parser.parse(reader, list, address);
                                requireNoContent(reader);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new XMLStreamException(e);
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

            if (subModel.hasDefined(JDBC_DRIVER_NAME)) {
                for (final Property jdbcDriver : subModel.get(Constants.JDBC_DRIVER_NAME).asPropertyList()) {
                    final ModelNode address = rootAddress.toModelNode();
                    address.add(Constants.JDBC_DRIVER_NAME, jdbcDriver.getName());
                    final ModelNode addOperation = Util.getEmptyOperation(ADD, address);
                    addOperation.get(DRIVER_NAME.getName()).set(jdbcDriver.getValue().get(DRIVER_NAME.getName()));
                    addOperation.get(DRIVER_MODULE_NAME.getName()).set(jdbcDriver.getValue().get(DRIVER_MODULE_NAME.getName()));
                    addOperation.get(DRIVER_MAJOR_VERSION.getName()).set(jdbcDriver.getValue().get(DRIVER_MAJOR_VERSION.getName()));
                    addOperation.get(DRIVER_MINOR_VERSION.getName()).set(jdbcDriver.getValue().get(DRIVER_MINOR_VERSION.getName()));
                    addOperation.get(DRIVER_CLASS_NAME.getName()).set(jdbcDriver.getValue().get(DRIVER_CLASS_NAME.getName()));
                    addOperation.get(DRIVER_DATASOURCE_CLASS_NAME.getName()).set(
                            jdbcDriver.getValue().get(DRIVER_DATASOURCE_CLASS_NAME.getName()));
                    addOperation.get(DRIVER_XA_DATASOURCE_CLASS_NAME.getName()).set(
                            jdbcDriver.getValue().get(DRIVER_XA_DATASOURCE_CLASS_NAME.getName()));
                    result.add(addOperation);
                }
            }

            if (subModel.hasDefined(DATA_SOURCE)) {
                for (final Property dataSourceProp : subModel.get(Constants.DATA_SOURCE).asPropertyList()) {
                    final ModelNode address = rootAddress.toModelNode();
                    address.add(Constants.DATA_SOURCE, dataSourceProp.getName());
                    final ModelNode addOperation = Util.getEmptyOperation(ADD, address);
                    final ModelNode dataSource = dataSourceProp.getValue();

                    populateAddModel(dataSource, addOperation, CONNECTION_PROPERTIES.getName(), DATASOURCE_ATTRIBUTE);

                    addOperation.get(DATASOURCE_DRIVER.getName()).set(dataSourceProp.getValue().get(DATASOURCE_DRIVER.getName()));
                    result.add(addOperation);

                    if (dataSource.hasDefined(CONNECTION_PROPERTIES.getName())) {
                        for (final Property prop : dataSource.get(CONNECTION_PROPERTIES.getName()).asPropertyList()) {
                            final ModelNode propAdrress = address.clone();

                            propAdrress.add(CONNECTION_PROPERTIES.getName(), prop.getName());
                            final ModelNode addPropOperation = Util.getEmptyOperation(ADD, propAdrress);
                            addPropOperation.get("value").set(prop.getValue().get(VALUE).asString());
                            result.add(addPropOperation);
                        }

                    }

                    if (! dataSource.hasDefined(ENABLED.getName()) || dataSource.get(ENABLED.getName()).asBoolean()) {
                        final ModelNode enableOperation = new ModelNode();
                        enableOperation.get(OP).set(ENABLE);
                        enableOperation.get(OP_ADDR).set(address);
                        enableOperation.get(PERSISTENT).set(dataSource.hasDefined(ENABLED.getName()));
                        result.add(enableOperation);
                    }
                }
            }

            if (subModel.hasDefined(XA_DATASOURCE)) {
                for (final Property dataSourceProp : subModel.get(Constants.XA_DATASOURCE).asPropertyList()) {
                    final ModelNode address = rootAddress.toModelNode();
                    address.add(Constants.XA_DATASOURCE, dataSourceProp.getName());
                    final ModelNode addOperation = Util.getEmptyOperation(ADD, address);
                    final ModelNode dataSource = dataSourceProp.getValue();

                    populateAddModel(dataSource, addOperation, XADATASOURCE_PROPERTIES.getName(), XA_DATASOURCE_ATTRIBUTE);

                    addOperation.get(DATASOURCE_DRIVER.getName()).set(dataSourceProp.getValue().get(DATASOURCE_DRIVER.getName()));
                    result.add(addOperation);

                    if (dataSource.hasDefined(XADATASOURCE_PROPERTIES.getName())) {
                        for (final Property prop : dataSource.get(XADATASOURCE_PROPERTIES.getName()).asPropertyList()) {
                            final ModelNode propAdrress = address.clone();

                            propAdrress.add(XADATASOURCE_PROPERTIES.getName(), prop.getName());
                            final ModelNode addPropOperation = Util.getEmptyOperation(ADD, propAdrress);
                            addPropOperation.get("value").set(prop.getValue().get(VALUE).asString());
                            result.add(addPropOperation);
                        }

                    }

                    if (! dataSource.hasDefined(ENABLED.getName()) || dataSource.get(ENABLED.getName()).asBoolean()) {
                        final ModelNode enableOperation = new ModelNode();
                        enableOperation.get(OP).set(ENABLE);
                        enableOperation.get(OP_ADDR).set(address);
                        enableOperation.get(PERSISTENT).set(dataSource.hasDefined(ENABLED.getName()));
                        result.add(enableOperation);
                    }
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

