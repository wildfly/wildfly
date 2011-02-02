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
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCES_SUBSYTEM;
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
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.metadata.ds.DataSources;
import org.jboss.jca.common.api.metadata.ds.XaDataSource;
import org.jboss.jca.common.metadata.ds.DsParser;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
public class NewDataSourcesExtension implements NewExtension {

    private static final Logger log = Logger.getLogger("org.jboss.as.datasources");

    @Override
    public void initialize(final NewExtensionContext context) {
        // Register the remoting subsystem
        final SubsystemRegistration registration = context.registerSubsystem(DATASOURCES_SUBSYTEM);

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
            // FIXME write out the details
            writer.writeEndElement();
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
                dsModel.get(CONNECTION_URL).set(ds.getConnectionUrl());
                dsModel.get(DRIVER_CLASS).set(ds.getDriverClass());
                dsModel.get(JNDINAME).set(ds.getJndiName());
                dsModel.get(MODULE).set(ds.getModule());
                dsModel.get(NEW_CONNECTION_SQL).set(ds.getNewConnectionSql());
                dsModel.get(POOLNAME).set(ds.getPoolName());
                dsModel.get(URL_DELIMITER).set(ds.getUrlDelimiter());
                dsModel.get(URL_SELECTOR_STRATEGY_CLASS_NAME).set(ds.getUrlSelectorStrategyClassName());
                dsModel.get(USE_JAVA_CONTEXT).set(ds.isUseJavaContext());
                dsModel.get(ENABLED).set(ds.isEnabled());
                if (ds.getPool() != null) {
                    dsModel.get(MAX_POOL_SIZE).set(ds.getPool().getMaxPoolSize());
                    dsModel.get(MIN_POOL_SIZE).set(ds.getPool().getMinPoolSize());
                    dsModel.get(POOL_PREFILL).set(ds.getPool().isPrefill());
                    dsModel.get(POOL_USE_STRICT_MIN).set(ds.getPool().isUseStrictMin());
                }
                if (ds.getSecurity() != null) {
                    dsModel.get(USERNAME).set(ds.getSecurity().getUserName());
                    dsModel.get(PASSWORD).set(ds.getSecurity().getPassword());
                }
                if (ds.getStatement() != null) {
                    dsModel.get(PREPAREDSTATEMENTSCACHESIZE).set(ds.getStatement().getPreparedStatementsCacheSize());
                    dsModel.get(SHAREPREPAREDSTATEMENTS).set(ds.getStatement().isSharePreparedStatements());
                    dsModel.get(TRACKSTATEMENTS).set(ds.getStatement().getTrackStatements().name());
                }
                if (ds.getTimeOut() != null) {
                    dsModel.get(ALLOCATION_RETRY).set(ds.getTimeOut().getAllocationRetry());
                    dsModel.get(ALLOCATION_RETRY_WAIT_MILLIS).set(ds.getTimeOut().getAllocationRetryWaitMillis());
                    dsModel.get(BLOCKING_TIMEOUT_WAIT_MILLIS).set(ds.getTimeOut().getBlockingTimeoutMillis());
                    dsModel.get(IDLETIMEOUTMINUTES).set(ds.getTimeOut().getIdleTimeoutMinutes());
                    dsModel.get(QUERYTIMEOUT).set(ds.getTimeOut().getQueryTimeout());
                    dsModel.get(USETRYLOCK).set(ds.getTimeOut().getUseTryLock());
                    dsModel.get(SETTXQUERTTIMEOUT).set(ds.getTimeOut().isSetTxQueryTimeout());
                }
                dsModel.get(TRANSACTION_ISOLOATION).set(ds.getTransactionIsolation().name());
                if (ds.getValidation() != null) {
                    dsModel.get(CHECKVALIDCONNECTIONSQL).set(ds.getValidation().getCheckValidConnectionSql());
                    dsModel.get(EXCEPTIONSORTERCLASSNAME).set(ds.getValidation().getExceptionSorterClassName());
                    dsModel.get(STALECONNECTIONCHECKERCLASSNAME).set(ds.getValidation().getStaleConnectionCheckerClassName());
                    dsModel.get(VALIDCONNECTIONCHECKERCLASSNAME).set(ds.getValidation().getValidConnectionCheckerClassName());
                    dsModel.get(BACKGROUNDVALIDATIONMINUTES).set(ds.getValidation().getBackgroundValidationMinutes());
                    dsModel.get(BACKGROUNDVALIDATION).set(ds.getValidation().isBackgroundValidation());
                    dsModel.get(USE_FAST_FAIL).set(ds.getValidation().isUseFastFail());
                    dsModel.get(VALIDATEONMATCH).set(ds.getValidation().isValidateOnMatch());
                }

                datasourcesNode.add(dsModel);
            }

            ModelNode XAdatasourcesNode = subsystem.get(DATASOURCES);
            for (XaDataSource xads : dataSources.getXaDataSource()) {
                ModelNode xadsModel = new ModelNode();
                for (Entry<String, String> entry : xads.getXaDataSourceProperty().entrySet()) {
                    xadsModel.get(XADATASOURCEPROPERTIES, entry.getKey()).set(entry.getValue());
                }
                xadsModel.get(XADATASOURCECLASS).set(xads.getXaDataSourceClass());
                xadsModel.get(JNDINAME).set(xads.getJndiName());
                xadsModel.get(MODULE).set(xads.getModule());
                xadsModel.get(NEW_CONNECTION_SQL).set(xads.getNewConnectionSql());
                xadsModel.get(POOLNAME).set(xads.getPoolName());
                xadsModel.get(URL_DELIMITER).set(xads.getUrlDelimiter());
                xadsModel.get(URL_SELECTOR_STRATEGY_CLASS_NAME).set(xads.getUrlSelectorStrategyClassName());
                xadsModel.get(USE_JAVA_CONTEXT).set(xads.isUseJavaContext());
                xadsModel.get(ENABLED).set(xads.isEnabled());
                if (xads.getXaPool() != null) {
                    xadsModel.get(MAX_POOL_SIZE).set(xads.getXaPool().getMaxPoolSize());
                    xadsModel.get(MIN_POOL_SIZE).set(xads.getXaPool().getMinPoolSize());
                    xadsModel.get(POOL_PREFILL).set(xads.getXaPool().isPrefill());
                    xadsModel.get(POOL_USE_STRICT_MIN).set(xads.getXaPool().isUseStrictMin());
                    xadsModel.get(INTERLIVING).set(xads.getXaPool().isInterleaving());
                    xadsModel.get(NOTXSEPARATEPOOL).set(xads.getXaPool().isNoTxSeparatePool());
                    xadsModel.get(PAD_XID).set(xads.getXaPool().isPadXid());
                    xadsModel.get(SAME_RM_OVERRIDE).set(xads.getXaPool().isSameRmOverride());
                    xadsModel.get(WRAP_XA_DATASOURCE).set(xads.getXaPool().isWrapXaDataSource());
                }
                if (xads.getSecurity() != null) {
                    xadsModel.get(USERNAME).set(xads.getSecurity().getUserName());
                    xadsModel.get(PASSWORD).set(xads.getSecurity().getPassword());
                }
                if (xads.getStatement() != null) {
                    xadsModel.get(PREPAREDSTATEMENTSCACHESIZE).set(xads.getStatement().getPreparedStatementsCacheSize());
                    xadsModel.get(SHAREPREPAREDSTATEMENTS).set(xads.getStatement().isSharePreparedStatements());
                    xadsModel.get(TRACKSTATEMENTS).set(xads.getStatement().getTrackStatements().name());
                }
                if (xads.getTimeOut() != null) {
                    xadsModel.get(ALLOCATION_RETRY).set(xads.getTimeOut().getAllocationRetry());
                    xadsModel.get(ALLOCATION_RETRY_WAIT_MILLIS).set(xads.getTimeOut().getAllocationRetryWaitMillis());
                    xadsModel.get(BLOCKING_TIMEOUT_WAIT_MILLIS).set(xads.getTimeOut().getBlockingTimeoutMillis());
                    xadsModel.get(IDLETIMEOUTMINUTES).set(xads.getTimeOut().getIdleTimeoutMinutes());
                    xadsModel.get(QUERYTIMEOUT).set(xads.getTimeOut().getQueryTimeout());
                    xadsModel.get(USETRYLOCK).set(xads.getTimeOut().getUseTryLock());
                    xadsModel.get(SETTXQUERTTIMEOUT).set(xads.getTimeOut().isSetTxQueryTimeout());
                    xadsModel.get(XA_RESOURCE_TIMEOUT).set(xads.getTimeOut().getXaResourceTimeout());
                }
                xadsModel.get(TRANSACTION_ISOLOATION).set(xads.getTransactionIsolation().name());
                if (xads.getValidation() != null) {
                    xadsModel.get(CHECKVALIDCONNECTIONSQL).set(xads.getValidation().getCheckValidConnectionSql());
                    xadsModel.get(EXCEPTIONSORTERCLASSNAME).set(xads.getValidation().getExceptionSorterClassName());
                    xadsModel.get(STALECONNECTIONCHECKERCLASSNAME).set(
                            xads.getValidation().getStaleConnectionCheckerClassName());
                    xadsModel.get(VALIDCONNECTIONCHECKERCLASSNAME).set(
                            xads.getValidation().getValidConnectionCheckerClassName());
                    xadsModel.get(BACKGROUNDVALIDATIONMINUTES).set(xads.getValidation().getBackgroundValidationMinutes());
                    xadsModel.get(BACKGROUNDVALIDATION).set(xads.getValidation().isBackgroundValidation());
                    xadsModel.get(USE_FAST_FAIL).set(xads.getValidation().isUseFastFail());
                    xadsModel.get(VALIDATEONMATCH).set(xads.getValidation().isValidateOnMatch());
                }

                XAdatasourcesNode.add(xadsModel);
            }
        }
    }
}
