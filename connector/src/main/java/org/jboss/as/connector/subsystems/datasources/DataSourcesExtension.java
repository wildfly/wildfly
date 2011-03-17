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

import java.util.Collections;
import static org.jboss.as.connector.subsystems.datasources.AbstractDataSourceAdd.populateAddModel;
import static org.jboss.as.connector.subsystems.datasources.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.datasources.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.datasources.Constants.BACKGROUNDVALIDATION;
import static org.jboss.as.connector.subsystems.datasources.Constants.BACKGROUNDVALIDATIONMINUTES;
import static org.jboss.as.connector.subsystems.datasources.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.datasources.Constants.CHECKVALIDCONNECTIONSQL;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_URL;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCES;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATA_SOURCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_CLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.EXCEPTIONSORTERCLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.IDLETIMEOUTMINUTES;
import static org.jboss.as.connector.subsystems.datasources.Constants.INTERLIVING;
import static org.jboss.as.connector.subsystems.datasources.Constants.JDBC_DRIVER;
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
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_DATA_SOURCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.XADATASOURCECLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.XADATASOURCEPROPERTIES;
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
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.REMOVE_DATA_SOURCE_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.REMOVE_JDBC_DRIVER_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.JDBC_DRIVER_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.REMOVE_XA_DATA_SOURCE_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.SUBSYSTEM;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.SUBSYSTEM_ADD_DESC;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.XA_DATASOURCE_ATTRIBUTE;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.XA_DATA_SOURCE_DESC;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DISABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.jca.common.api.metadata.common.CommonPool;
import org.jboss.jca.common.api.metadata.common.CommonXaPool;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.metadata.ds.DataSources;
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
        final ModelNodeRegistration subsystem = registration.registerSubsystemModel(SUBSYSTEM);
        subsystem.registerOperationHandler(ADD, DataSourcesSubsystemAdd.INSTANCE, SUBSYSTEM_ADD_DESC, false);
        subsystem.registerOperationHandler(DESCRIBE, DataSourcesSubsystemDescribeHandler.INSTANCE,
                DataSourcesSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        final ModelNodeRegistration jdbcDrivers = subsystem.registerSubModel(PathElement.pathElement(JDBC_DRIVER), JDBC_DRIVER_DESC);
        jdbcDrivers.registerOperationHandler(ADD, JdbcDriverAdd.INSTANCE, ADD_JDBC_DRIVER_DESC, false);
        jdbcDrivers.registerOperationHandler(REMOVE, JdbcDriverRemove.INSTANCE, REMOVE_JDBC_DRIVER_DESC, false);

        final ModelNodeRegistration dataSources = subsystem.registerSubModel(PathElement.pathElement(DATA_SOURCE), DATA_SOURCE_DESC);
        dataSources.registerOperationHandler(ADD, DataSourceAdd.INSTANCE, ADD_DATA_SOURCE_DESC, false);
        dataSources.registerOperationHandler(REMOVE, DataSourceRemove.INSTANCE, REMOVE_DATA_SOURCE_DESC, false);
        dataSources.registerOperationHandler(ENABLE, DataSourceEnable.INSTANCE, ENABLE_DATA_SOURCE_DESC, false);
        dataSources.registerOperationHandler(DISABLE, DataSourceDisable.INSTANCE, DISABLE_DATA_SOURCE_DESC, false);

        final ModelNodeRegistration xaDataSources = subsystem.registerSubModel(PathElement.pathElement(XA_DATA_SOURCE), XA_DATA_SOURCE_DESC);
        xaDataSources.registerOperationHandler(ADD, XaDataSourceAdd.INSTANCE, ADD_XA_DATA_SOURCE_DESC, false);
        xaDataSources.registerOperationHandler(REMOVE, XaDataSourceRemove.INSTANCE, REMOVE_XA_DATA_SOURCE_DESC, false);
        xaDataSources.registerOperationHandler(ENABLE, DataSourceEnable.INSTANCE, ENABLE_XA_DATA_SOURCE_DESC, false);
        xaDataSources.registerOperationHandler(DISABLE, DataSourceDisable.INSTANCE, DISABLE_XA_DATA_SOURCE_DESC, false);
    }

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), NewDataSourceSubsystemParser.INSTANCE);
    }

    static final class NewDataSourceSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
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
            if (node.hasDefined(DATA_SOURCE)) {
                for (Property property : node.get(DATA_SOURCE).asPropertyList()) {
                    final ModelNode dataSourceNode = property.getValue();
                    boolean isXADataSource = hasAnyOf(dataSourceNode, XA_RESOURCE_TIMEOUT, XADATASOURCECLASS,
                            XADATASOURCEPROPERTIES);
                    writer.writeStartElement(isXADataSource ? DataSources.Tag.XA_DATASOURCE.getLocalName()
                            : DataSources.Tag.DATASOURCE.getLocalName());

                    writeAttributeIfHas(writer, dataSourceNode, DataSource.Attribute.JNDINAME, JNDINAME);
                    writeAttributeIfHas(writer, dataSourceNode, DataSource.Attribute.POOL_NAME, POOLNAME);
                    writeAttributeIfHas(writer, dataSourceNode, DataSource.Attribute.ENABLED, ENABLED);
                    writeAttributeIfHas(writer, dataSourceNode, DataSource.Attribute.USEJAVACONTEXT, USE_JAVA_CONTEXT);
                    writeAttributeIfHas(writer, dataSourceNode, DataSource.Attribute.SPY, SPY);

                    if (!isXADataSource) {
                        writeElementIfHas(writer, dataSourceNode, DataSource.Tag.CONNECTIONURL, CONNECTION_URL);
                        writeElementIfHas(writer, dataSourceNode, DataSource.Tag.DRIVERCLASS, DRIVER_CLASS);
                    }
                    if (isXADataSource) {
                        // TODO - Write XA properties.
                        writeElementIfHas(writer, dataSourceNode, XaDataSource.Tag.XADATASOURCECLASS, XADATASOURCECLASS);
                    }
                    writeElementIfHas(writer, dataSourceNode, DataSource.Tag.MODULE, MODULE);
                    if (!isXADataSource) {
                        // TODO - Write Properties

                    }
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
                            POOL_USE_STRICT_MIN);
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
                        if (isXADataSource) {
                            writeElementIfHas(writer, dataSourceNode, CommonXaPool.Tag.ISSAMERMOVERRIDEVALUE, SAME_RM_OVERRIDE);
                            writeEmptyElementIfHasAndTrue(writer, dataSourceNode, CommonXaPool.Tag.ISSAMERMOVERRIDEVALUE,
                                    SAME_RM_OVERRIDE);
                            writeEmptyElementIfHasAndTrue(writer, dataSourceNode, CommonXaPool.Tag.NO_TX_SEPARATE_POOLS,
                                    NOTXSEPARATEPOOL);
                            writeElementIfHas(writer, dataSourceNode, CommonXaPool.Tag.PAD_XID, PAD_XID);
                            writeElementIfHas(writer, dataSourceNode, CommonXaPool.Tag.WRAP_XA_RESOURCE, WRAP_XA_DATASOURCE);
                        }
                        writer.writeEndElement();
                    }
                    boolean securityRequired = hasAnyOf(dataSourceNode, USERNAME, PASSWORD);
                    if (securityRequired) {
                        writer.writeStartElement(DataSource.Tag.SECURITY.getLocalName());
                        writeElementIfHas(writer, dataSourceNode, DsSecurity.Tag.USERNAME, USERNAME);
                        writeElementIfHas(writer, dataSourceNode, DsSecurity.Tag.PASSWORD, PASSWORD);
                        writeElementIfHas(writer, dataSourceNode, DsSecurity.Tag.SECURITY_DOMAIN, SECURITY_DOMAIN);

                        writer.writeEndElement();
                    }
                    boolean validationRequired = hasAnyOf(dataSourceNode, VALIDCONNECTIONCHECKERCLASSNAME,
                            CHECKVALIDCONNECTIONSQL, VALIDATEONMATCH, BACKGROUNDVALIDATION, BACKGROUNDVALIDATIONMINUTES,
                            USE_FAST_FAIL, STALECONNECTIONCHECKERCLASSNAME, EXCEPTIONSORTERCLASSNAME);
                    if (validationRequired) {
                        writer.writeStartElement(DataSource.Tag.VALIDATION.getLocalName());
                        writeElementIfHas(writer, dataSourceNode, Validation.Tag.VALIDCONNECTIONCHECKER,
                                VALIDCONNECTIONCHECKERCLASSNAME);
                        writeElementIfHas(writer, dataSourceNode, Validation.Tag.CHECKVALIDCONNECTIONSQL,
                                CHECKVALIDCONNECTIONSQL);
                        writeElementIfHas(writer, dataSourceNode, Validation.Tag.VALIDATEONMATCH, VALIDATEONMATCH);
                        writeElementIfHas(writer, dataSourceNode, Validation.Tag.BACKGROUNDVALIDATION, BACKGROUNDVALIDATION);
                        writeElementIfHas(writer, dataSourceNode, Validation.Tag.BACKGROUNDVALIDATIONMINUTES,
                                BACKGROUNDVALIDATIONMINUTES);
                        writeElementIfHas(writer, dataSourceNode, Validation.Tag.USEFASTFAIL, USE_FAST_FAIL);
                        writeElementIfHas(writer, dataSourceNode, Validation.Tag.STALECONNECTIONCHECKER,
                                STALECONNECTIONCHECKERCLASSNAME);
                        writeElementIfHas(writer, dataSourceNode, Validation.Tag.EXCEPTIONSORTER, EXCEPTIONSORTERCLASSNAME);
                        writer.writeEndElement();
                    }
                    boolean timeoutRequired = hasAnyOf(dataSourceNode, BLOCKING_TIMEOUT_WAIT_MILLIS, IDLETIMEOUTMINUTES,
                            SETTXQUERYTIMEOUT, QUERYTIMEOUT, USETRYLOCK, ALLOCATION_RETRY, ALLOCATION_RETRY_WAIT_MILLIS,
                            XA_RESOURCE_TIMEOUT);
                    if (timeoutRequired) {
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
            writer.writeEndElement();

            if (node.hasDefined(JDBC_DRIVER)) {
                writer.writeStartElement(Element.DRIVERS.getLocalName());
                for (Property driverProperty : node.get(JDBC_DRIVER).asPropertyList()) {
                    writer.writeStartElement(Element.DRIVER.getLocalName());
                    writer.writeAttribute(Attribute.MODULE.getLocalName(), driverProperty.getValue().require(MODULE).asString());
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

        private void writeAttributeIfHas(final XMLExtendedStreamWriter writer, final ModelNode node,
                                         final DataSource.Attribute attr, final String identifier) throws XMLStreamException {
            if (has(node, identifier)) {
                writer.writeAttribute(attr.getLocalName(), node.get(identifier).asString());
            }
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

                                // Parse what is left after the datasources element
                                parseForDrivers(reader, address, list);
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
            }
        }

        private void parseForDrivers(XMLExtendedStreamReader reader, final ModelNode parentAddress, final List<ModelNode> list) throws XMLStreamException {
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case DATASOURCES_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        switch (element) {
                            case DRIVERS: {
                                parseDrivers(reader, parentAddress, list);
                                break;
                            }
                            default: {
                                throw unexpectedElement(reader);
                            }
                        }
                        break;
                    }
                    default: {
                        throw unexpectedElement(reader);
                    }
                }
            }
        }

        private void parseDrivers(final XMLExtendedStreamReader reader, final ModelNode parentAddress, final List<ModelNode> list) throws XMLStreamException {
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case DATASOURCES_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        switch (element) {
                            case DRIVER: {

                                String moduleName = null;

                                for (int i = 0; i < reader.getAttributeCount(); i++) {
                                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                                    switch (attribute) {
                                        case MODULE: {
                                            moduleName = reader.getAttributeValue(i);
                                            break;
                                        }
                                        default: {
                                            throw unexpectedAttribute(reader, i);
                                        }
                                    }
                                }

                                if (moduleName == null) {
                                    throw missingRequired(reader, Collections.singleton("module"));
                                }

                                final ModelNode address = parentAddress.clone();
                                address.add(JDBC_DRIVER, moduleName);
                                address.protect();
                                final ModelNode op = Util.getEmptyOperation(ADD, address);
                                op.get(MODULE).set(moduleName);
                                list.add(op);

                                requireNoContent(reader);
                                break;
                            }
                            default: {
                                throw unexpectedElement(reader);
                            }
                        }
                        break;
                    }
                    default: {
                        throw unexpectedElement(reader);
                    }
                }
            }
        }
    }

    private static class DataSourcesSubsystemDescribeHandler implements ModelQueryOperationHandler, DescriptionProvider {
        static final DataSourcesSubsystemDescribeHandler INSTANCE = new DataSourcesSubsystemDescribeHandler();

        @Override
        public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) {
            final ModelNode result = new ModelNode();

            final PathAddress rootAddress = PathAddress.pathAddress(PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement());
            final ModelNode subModel = context.getSubModel();

            final ModelNode subsystemAdd = new ModelNode();
            subsystemAdd.get(OP).set(ADD);
            subsystemAdd.get(OP_ADDR).set(rootAddress.toModelNode());

            result.add(subsystemAdd);

            if (subModel.hasDefined(JDBC_DRIVER)) {
                for (final Property jdbcDriver : subModel.get(Constants.JDBC_DRIVER).asPropertyList()) {
                    final ModelNode address = rootAddress.toModelNode();
                    address.add(Constants.JDBC_DRIVER, jdbcDriver.getName());
                    final ModelNode addOperation = Util.getEmptyOperation(ADD, address);
                    addOperation.get(MODULE).set(jdbcDriver.getValue().get(MODULE));
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

                    addOperation.get(MODULE).set(dataSourceProp.getValue().get(MODULE));
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

                    addOperation.get(MODULE).set(dataSourceProp.getValue().get(MODULE));
                    result.add(addOperation);
                }
            }

            resultHandler.handleResultFragment(Util.NO_LOCATION, result);
            resultHandler.handleResultComplete();
            return new BasicOperationResult();
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }
    }

}
