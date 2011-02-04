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
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_RESOURCE_TIMEOUT;
import static org.jboss.as.connector.subsystems.datasources.NewDataSourcesSubsystemProviders.SUBSYSTEM;
import static org.jboss.as.connector.subsystems.datasources.NewDataSourcesSubsystemProviders.SUBSYSTEM_ADD_DESC;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.NewExtension;
import org.jboss.as.controller.NewExtensionContext;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.api.metadata.common.CommonPool;
import org.jboss.jca.common.api.metadata.common.CommonSecurity;
import org.jboss.jca.common.api.metadata.common.CommonXaPool;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.metadata.ds.DataSources;
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
 */
public class NewDataSourcesExtension implements NewExtension {

    private static final Logger log = Logger.getLogger("org.jboss.as.datasources");

    @Override
    public void initialize(final NewExtensionContext context) {
        // Register the remoting subsystem
        final SubsystemRegistration registration = context.registerSubsystem(DATASOURCES);

        registration.registerXMLElementWriter(NewConnectorSubsystemParser.INSTANCE);

        // Remoting subsystem description and operation handlers
        final ModelNodeRegistration subsystem = registration.registerSubsystemModel(SUBSYSTEM);
        subsystem.registerOperationHandler("add", NewDataSourcesSubsystemAdd.INSTANCE, SUBSYSTEM_ADD_DESC, false);

    }

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), NewConnectorSubsystemParser.INSTANCE);
    }

    static final class NewConnectorSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
            XMLElementWriter<SubsystemMarshallingContext> {

        static final NewConnectorSubsystemParser INSTANCE = new NewConnectorSubsystemParser();

        /** {@inheritDoc} */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
            ModelNode node = context.getModelNode();

            writer.writeStartElement(DATASOURCES);
            if (node.has(DATASOURCES)) {
                for (ModelNode dataSourceNode : node.get(DATASOURCES).asList()) {
                    boolean isXADataSource = hasAnyOf(dataSourceNode, XA_RESOURCE_TIMEOUT, XADATASOURCECLASS,
                            XADATASOURCEPROPERTIES);
                    writer.writeStartElement(isXADataSource ? DataSources.Tag.XA_DATASOURCE.getLocalName()
                            : DataSources.Tag.DATASOURCE.getLocalName());

                    writeAttributeIfHas(writer, dataSourceNode, DataSource.Attribute.JNDINAME, JNDINAME);
                    writeAttributeIfHas(writer, dataSourceNode, DataSource.Attribute.POOL_NAME, POOLNAME);
                    writeAttributeIfHas(writer, dataSourceNode, DataSource.Attribute.ENABLED, ENABLED);
                    writeAttributeIfHas(writer, dataSourceNode, DataSource.Attribute.USEJAVACONTEXT, USE_JAVA_CONTEXT);

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
                        writeElementIfHas(writer, dataSourceNode, CommonSecurity.Tag.USERNAME, USERNAME);
                        writeElementIfHas(writer, dataSourceNode, CommonSecurity.Tag.PASSWORD, PASSWORD);
                        writer.writeEndElement();
                    }
                    boolean validationRequired = hasAnyOf(dataSourceNode, VALIDCONNECTIONCHECKERCLASSNAME,
                            CHECKVALIDCONNECTIONSQL, VALIDATEONMATCH, BACKGROUNDVALIDATION, BACKGROUNDVALIDATIONMINUTES,
                            USE_FAST_FAIL, STALECONNECTIONCHECKERCLASSNAME, EXCEPTIONSORTERCLASSNAME);
                    if (validationRequired) {
                        writer.writeStartElement(DataSource.Tag.VALIDATION.getLocalName());
                        writeElementIfHas(writer, dataSourceNode, Validation.Tag.VALIDCONNECTIONCHECKERCLASSNAME,
                                VALIDCONNECTIONCHECKERCLASSNAME);
                        writeElementIfHas(writer, dataSourceNode, Validation.Tag.CHECKVALIDCONNECTIONSQL,
                                CHECKVALIDCONNECTIONSQL);
                        writeElementIfHas(writer, dataSourceNode, Validation.Tag.VALIDATEONMATCH, VALIDATEONMATCH);
                        writeElementIfHas(writer, dataSourceNode, Validation.Tag.BACKGROUNDVALIDATION, BACKGROUNDVALIDATION);
                        writeElementIfHas(writer, dataSourceNode, Validation.Tag.BACKGROUNDVALIDATIONMINUTES,
                                BACKGROUNDVALIDATIONMINUTES);
                        writeElementIfHas(writer, dataSourceNode, Validation.Tag.USEFASTFAIL, USE_FAST_FAIL);
                        writeElementIfHas(writer, dataSourceNode, Validation.Tag.STALECONNECTIONCHECKERCLASSNAME,
                                STALECONNECTIONCHECKERCLASSNAME);
                        writeElementIfHas(writer, dataSourceNode, Validation.Tag.EXCEPTIONSORTERCLASSNAME,
                                EXCEPTIONSORTERCLASSNAME);
                        writer.writeEndElement();
                    }
                    boolean timeoutRequired = hasAnyOf(dataSourceNode, BLOCKING_TIMEOUT_WAIT_MILLIS, IDLETIMEOUTMINUTES,
                            SETTXQUERTTIMEOUT, QUERYTIMEOUT, USETRYLOCK, ALLOCATION_RETRY, ALLOCATION_RETRY_WAIT_MILLIS,
                            XA_RESOURCE_TIMEOUT);
                    if (timeoutRequired) {
                        writeElementIfHas(writer, dataSourceNode, TimeOut.Tag.BLOCKINGTIMEOUTMILLIS,
                                BLOCKING_TIMEOUT_WAIT_MILLIS);
                        writeElementIfHas(writer, dataSourceNode, TimeOut.Tag.IDLETIMEOUTMINUTES, IDLETIMEOUTMINUTES);
                        writeEmptyElementIfHasAndTrue(writer, dataSourceNode, TimeOut.Tag.SETTXQUERYTIMEOUT, SETTXQUERTTIMEOUT);
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

        private void writeElementIfHas(XMLExtendedStreamWriter writer, ModelNode node, CommonSecurity.Tag element,
                String identifier) throws XMLStreamException {
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

            // FIXME this should come from somewhere
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
                        final Element element = Element.forName(reader.getLocalName());
                        log.tracef("%s -> %s", localName, element);
                        switch (element) {
                            case SUBSYSTEM: {
                                DsParser parser = new DsParser();
                                dataSources = parser.parse(reader);

                                // Ensure the final end tag of the subsystem has been read.
                                ParseUtils.requireNoContent(reader);

                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new XMLStreamException(e);
            }

            ModelNode datasourcesNode = subsystem.get(DATASOURCES);
            for (DataSource ds : dataSources.getDataSource()) {
                ModelNode dsModel = new ModelNode();
                for (Entry<String, String> entry : ds.getConnectionProperties().entrySet()) {
                    dsModel.get(CONNECTION_PROPERTIES, entry.getKey()).set(entry.getValue());
                }
                setIfNotNull(dsModel, CONNECTION_URL, ds.getConnectionUrl());
                setIfNotNull(dsModel, DRIVER_CLASS, ds.getDriverClass());
                setIfNotNull(dsModel, JNDINAME, ds.getJndiName());
                setIfNotNull(dsModel, MODULE, ds.getModule());
                setIfNotNull(dsModel, NEW_CONNECTION_SQL, ds.getNewConnectionSql());
                setIfNotNull(dsModel, POOLNAME, ds.getPoolName());
                setIfNotNull(dsModel, URL_DELIMITER, ds.getUrlDelimiter());
                setIfNotNull(dsModel, URL_SELECTOR_STRATEGY_CLASS_NAME, ds.getUrlSelectorStrategyClassName());
                setIfNotNull(dsModel, USE_JAVA_CONTEXT, ds.isUseJavaContext());
                setIfNotNull(dsModel, ENABLED, ds.isEnabled());

                CommonPool pool = ds.getPool();
                if (pool != null) {
                    setIfNotNull(dsModel, MAX_POOL_SIZE, pool.getMaxPoolSize());
                    setIfNotNull(dsModel, MIN_POOL_SIZE, pool.getMinPoolSize());
                    setIfNotNull(dsModel, POOL_PREFILL, pool.isPrefill());
                    setIfNotNull(dsModel, POOL_USE_STRICT_MIN, pool.isUseStrictMin());
                }
                CommonSecurity security = ds.getSecurity();
                if (security != null) {
                    setIfNotNull(dsModel, USERNAME, security.getUserName());
                    setIfNotNull(dsModel, PASSWORD, security.getPassword());
                }
                Statement statement = ds.getStatement();
                if (statement != null) {
                    setIfNotNull(dsModel, PREPAREDSTATEMENTSCACHESIZE, statement.getPreparedStatementsCacheSize());
                    setIfNotNull(dsModel, SHAREPREPAREDSTATEMENTS, statement.isSharePreparedStatements());
                    if (statement.getTrackStatements() != null) {
                        setIfNotNull(dsModel, TRACKSTATEMENTS, statement.getTrackStatements().name());
                    }
                }
                TimeOut timeout = ds.getTimeOut();
                if (timeout != null) {
                    setIfNotNull(dsModel, ALLOCATION_RETRY, timeout.getAllocationRetry());
                    setIfNotNull(dsModel, ALLOCATION_RETRY_WAIT_MILLIS, timeout.getAllocationRetryWaitMillis());
                    setIfNotNull(dsModel, BLOCKING_TIMEOUT_WAIT_MILLIS, timeout.getBlockingTimeoutMillis());
                    setIfNotNull(dsModel, IDLETIMEOUTMINUTES, timeout.getIdleTimeoutMinutes());
                    setIfNotNull(dsModel, QUERYTIMEOUT, timeout.getQueryTimeout());
                    setIfNotNull(dsModel, USETRYLOCK, timeout.getUseTryLock());
                    setIfNotNull(dsModel, SETTXQUERTTIMEOUT, timeout.isSetTxQueryTimeout());
                }
                if (ds.getTransactionIsolation() != null) {
                    setIfNotNull(dsModel, TRANSACTION_ISOLOATION, ds.getTransactionIsolation().name());
                }
                Validation validation = ds.getValidation();
                if (validation != null) {
                    setIfNotNull(dsModel, CHECKVALIDCONNECTIONSQL, validation.getCheckValidConnectionSql());
                    setIfNotNull(dsModel, EXCEPTIONSORTERCLASSNAME, validation.getExceptionSorterClassName());
                    setIfNotNull(dsModel, STALECONNECTIONCHECKERCLASSNAME, validation.getStaleConnectionCheckerClassName());
                    setIfNotNull(dsModel, VALIDCONNECTIONCHECKERCLASSNAME, validation.getValidConnectionCheckerClassName());
                    setIfNotNull(dsModel, BACKGROUNDVALIDATIONMINUTES, validation.getBackgroundValidationMinutes());
                    setIfNotNull(dsModel, BACKGROUNDVALIDATION, validation.isBackgroundValidation());
                    setIfNotNull(dsModel, USE_FAST_FAIL, validation.isUseFastFail());
                    setIfNotNull(dsModel, VALIDATEONMATCH, validation.isValidateOnMatch());
                }

                datasourcesNode.add(dsModel);
            }

            ModelNode XAdatasourcesNode = subsystem.get(DATASOURCES);
            for (XaDataSource xads : dataSources.getXaDataSource()) {
                ModelNode xadsModel = new ModelNode();
                for (Entry<String, String> entry : xads.getXaDataSourceProperty().entrySet()) {
                    xadsModel.get(XADATASOURCEPROPERTIES, entry.getKey()).set(entry.getValue());
                }
                setIfNotNull(xadsModel, XADATASOURCECLASS, xads.getXaDataSourceClass());
                setIfNotNull(xadsModel, JNDINAME, xads.getJndiName());
                setIfNotNull(xadsModel, MODULE, xads.getModule());
                setIfNotNull(xadsModel, NEW_CONNECTION_SQL, xads.getNewConnectionSql());
                setIfNotNull(xadsModel, POOLNAME, xads.getPoolName());
                setIfNotNull(xadsModel, URL_DELIMITER, xads.getUrlDelimiter());
                setIfNotNull(xadsModel, URL_SELECTOR_STRATEGY_CLASS_NAME, xads.getUrlSelectorStrategyClassName());
                setIfNotNull(xadsModel, USE_JAVA_CONTEXT, xads.isUseJavaContext());
                setIfNotNull(xadsModel, ENABLED, xads.isEnabled());
                CommonXaPool pool = xads.getXaPool();
                if (pool != null) {
                    setIfNotNull(xadsModel, MAX_POOL_SIZE, pool.getMaxPoolSize());
                    setIfNotNull(xadsModel, MIN_POOL_SIZE, pool.getMinPoolSize());
                    setIfNotNull(xadsModel, POOL_PREFILL, pool.isPrefill());
                    setIfNotNull(xadsModel, POOL_USE_STRICT_MIN, pool.isUseStrictMin());
                    setIfNotNull(xadsModel, INTERLIVING, pool.isInterleaving());
                    setIfNotNull(xadsModel, NOTXSEPARATEPOOL, pool.isNoTxSeparatePool());
                    setIfNotNull(xadsModel, PAD_XID, pool.isPadXid());
                    setIfNotNull(xadsModel, SAME_RM_OVERRIDE, pool.isSameRmOverride());
                    setIfNotNull(xadsModel, WRAP_XA_DATASOURCE, pool.isWrapXaDataSource());
                }
                CommonSecurity security = xads.getSecurity();
                if (security != null) {
                    setIfNotNull(xadsModel, USERNAME, security.getUserName());
                    setIfNotNull(xadsModel, PASSWORD, security.getPassword());
                }
                Statement statement = xads.getStatement();
                if (statement != null) {
                    setIfNotNull(xadsModel, PREPAREDSTATEMENTSCACHESIZE, statement.getPreparedStatementsCacheSize());
                    setIfNotNull(xadsModel, SHAREPREPAREDSTATEMENTS, statement.isSharePreparedStatements());
                    if (statement.getTrackStatements() != null) {
                        setIfNotNull(xadsModel, TRACKSTATEMENTS, statement.getTrackStatements().name());
                    }
                }
                TimeOut timeout = xads.getTimeOut();
                if (timeout != null) {
                    setIfNotNull(xadsModel, ALLOCATION_RETRY, timeout.getAllocationRetry());
                    setIfNotNull(xadsModel, ALLOCATION_RETRY_WAIT_MILLIS, timeout.getAllocationRetryWaitMillis());
                    setIfNotNull(xadsModel, BLOCKING_TIMEOUT_WAIT_MILLIS, timeout.getBlockingTimeoutMillis());
                    setIfNotNull(xadsModel, IDLETIMEOUTMINUTES, timeout.getIdleTimeoutMinutes());
                    setIfNotNull(xadsModel, QUERYTIMEOUT, timeout.getQueryTimeout());
                    setIfNotNull(xadsModel, USETRYLOCK, timeout.getUseTryLock());
                    setIfNotNull(xadsModel, SETTXQUERTTIMEOUT, timeout.isSetTxQueryTimeout());
                    setIfNotNull(xadsModel, XA_RESOURCE_TIMEOUT, timeout.getXaResourceTimeout());
                }
                if (xads.getTransactionIsolation() != null) {
                    setIfNotNull(xadsModel, TRANSACTION_ISOLOATION, xads.getTransactionIsolation().name());
                }
                Validation validation = xads.getValidation();
                if (xads.getValidation() != null) {
                    setIfNotNull(xadsModel, CHECKVALIDCONNECTIONSQL, validation.getCheckValidConnectionSql());
                    setIfNotNull(xadsModel, EXCEPTIONSORTERCLASSNAME, validation.getExceptionSorterClassName());
                    setIfNotNull(xadsModel, STALECONNECTIONCHECKERCLASSNAME, validation.getStaleConnectionCheckerClassName());
                    setIfNotNull(xadsModel, VALIDCONNECTIONCHECKERCLASSNAME, validation.getValidConnectionCheckerClassName());
                    setIfNotNull(xadsModel, BACKGROUNDVALIDATIONMINUTES, validation.getBackgroundValidationMinutes());
                    setIfNotNull(xadsModel, BACKGROUNDVALIDATION, validation.isBackgroundValidation());
                    setIfNotNull(xadsModel, USE_FAST_FAIL, validation.isUseFastFail());
                    setIfNotNull(xadsModel, VALIDATEONMATCH, validation.isValidateOnMatch());
                }

                XAdatasourcesNode.add(xadsModel);
            }
        }

        private void setIfNotNull(ModelNode node, String identifier, Boolean value) {
            if (value != null) {
                node.get(identifier).set(value);
            }
        }

        private void setIfNotNull(ModelNode node, String identifier, Integer value) {
            if (value != null) {
                node.get(identifier).set(value);
            }
        }

        private void setIfNotNull(ModelNode node, String identifier, Long value) {
            if (value != null) {
                node.get(identifier).set(value);
            }
        }

        private void setIfNotNull(ModelNode node, String identifier, String value) {
            if (value != null) {
                node.get(identifier).set(value);
            }
        }

    }
}
