/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
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
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_PROPERTY_VALUE;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_URL;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_CLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_DRIVER;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATA_SOURCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_CLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_CLASS_NAME;
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
import static org.jboss.as.connector.subsystems.datasources.Constants.POOLNAME_NAME;
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PERSISTENT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.connector.util.AbstractParser;
import org.jboss.as.connector.util.ParserException;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.CommonBundle;
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
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.logging.Messages;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * A DsParser.
 *
 * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
 */
public class DsParser extends AbstractParser {
    /**
     * The bundle
     */
    private static CommonBundle bundle = Messages.getBundle(CommonBundle.class);


    public void parse(final XMLExtendedStreamReader reader, final List<ModelNode> list, ModelNode parentAddress) throws Exception {

        DataSources dataSources = null;

        //iterate over tags
        int iterate;
        try {
            iterate = reader.nextTag();
        } catch (XMLStreamException e) {
            //founding a non tag..go on. Normally non-tag found at beginning are comments or DTD declaration
            iterate = reader.nextTag();
        }
        switch (iterate) {
            case END_ELEMENT: {
                // should mean we're done, so ignore it.
                break;
            }
            case START_ELEMENT: {

                switch (Tag.forName(reader.getLocalName())) {
                    case DATASOURCES: {
                        parseDataSources(reader, list, parentAddress);
                        break;
                    }
                    default:
                        throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
                }

                break;
            }
            default:
                throw new IllegalStateException();
        }


    }

    private void parseDataSources(final XMLExtendedStreamReader reader, final List<ModelNode> list, final ModelNode parentAddress) throws XMLStreamException, ParserException,
            ValidateException {
        boolean driversMatched = false;
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (Tag.forName(reader.getLocalName()) == Tag.DATASOURCES)
                        // should mean we're done, so ignore it.
                        return;
                }
                case START_ELEMENT: {
                    switch (DataSources.Tag.forName(reader.getLocalName())) {
                        case DATASOURCE: {
                            parseDataSource(reader, list, parentAddress);
                            break;
                        }
                        case XA_DATASOURCE: {
                            parseXADataSource(reader, list, parentAddress);
                            break;
                        }
                        case DRIVERS: {
                            driversMatched = true;
                            break;
                        }
                        case DRIVER: {
                            parseDriver(reader, list, parentAddress);
                            break;
                        }
                        default:
                            throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
                    }
                    break;
                }
            }
        }
        throw new ParserException(bundle.unexpectedEndOfDocument());
    }

    private void parseDriver(final XMLExtendedStreamReader reader, final List<ModelNode> list, final ModelNode parentAddress) throws XMLStreamException, ParserException,
            ValidateException {
        final ModelNode driverAddress = parentAddress.clone();
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);

        String driverName = null;
        for (org.jboss.jca.common.api.metadata.ds.Driver.Attribute attribute : Driver.Attribute.values()) {
            switch (attribute) {

                case NAME: {
                    driverName = rawAttributeText(reader, DRIVER_NAME.getXmlName());
                    DRIVER_NAME.parseAndSetParameter(driverName, operation, reader);
                    break;
                }
                case MAJOR_VERSION: {
                    String value = rawAttributeText(reader, DRIVER_MAJOR_VERSION.getXmlName());
                    DRIVER_MAJOR_VERSION.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case MINOR_VERSION: {
                    String value = rawAttributeText(reader, DRIVER_MINOR_VERSION.getXmlName());
                    DRIVER_MINOR_VERSION.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case MODULE: {
                    String value = rawAttributeText(reader, DRIVER_MODULE_NAME.getXmlName());
                    DRIVER_MODULE_NAME.parseAndSetParameter(value, operation, reader);
                    break;
                }
                default:
                    break;
            }
        }
        driverAddress.add(JDBC_DRIVER_NAME, driverName);
        driverAddress.protect();

        operation.get(OP_ADDR).set(driverAddress);


        //elements reading
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DataSources.Tag.forName(reader.getLocalName()) == DataSources.Tag.DRIVER) {
                        list.add(operation);
                        return;
                    } else {
                        if (Driver.Tag.forName(reader.getLocalName()) == Driver.Tag.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (Driver.Tag.forName(reader.getLocalName())) {
                        case DATASOURCE_CLASS: {
                            String value = rawElementText(reader);
                            DRIVER_MAJOR_VERSION.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case XA_DATASOURCE_CLASS: {
                            String value = rawElementText(reader);
                            DRIVER_XA_DATASOURCE_CLASS_NAME.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case DRIVER_CLASS: {
                            String value = rawElementText(reader);
                            DRIVER_CLASS_NAME.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        default:
                            throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
                    }
                    break;
                }
            }
        }
        throw new ParserException(bundle.unexpectedEndOfDocument());
    }

    private void parseXADataSource(XMLExtendedStreamReader reader, final List<ModelNode> list, final ModelNode parentAddress) throws XMLStreamException, ParserException,
            ValidateException {

        String poolName = null;
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        boolean enabled = ENABLED.getDefaultValue().asBoolean();
        // Don't persist the enabled flag unless the user set it
        boolean persistEnabled = false;
        for (DataSource.Attribute attribute : DataSource.Attribute.values()) {
            switch (attribute) {
                case ENABLED: {
                    String value = rawAttributeText(reader, ENABLED.getXmlName());
                    if (value != null) {
                        enabled = Boolean.parseBoolean(value);
                        //ENABLED.parseAndSetParameter(value, operation, reader);
                        persistEnabled = true;
                    }
                    break;
                }
                case JNDI_NAME: {
                    final String jndiName = rawAttributeText(reader, JNDINAME.getXmlName());
                    JNDINAME.parseAndSetParameter(jndiName, operation, reader);
                    break;
                }
                case POOL_NAME: {
                    poolName = rawAttributeText(reader, POOLNAME_NAME);
                    break;
                }
                case USE_JAVA_CONTEXT: {
                    String value = rawAttributeText(reader, USE_JAVA_CONTEXT.getXmlName());
                    if (value != null) {
                        USE_JAVA_CONTEXT.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case SPY: {
                    String value = rawAttributeText(reader, SPY.getXmlName());
                    if (value != null) {
                        SPY.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case USE_CCM: {
                    String value = rawAttributeText(reader, USE_CCM.getXmlName());
                    if (value != null) {
                        USE_CCM.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case JTA: {
                    String value = rawAttributeText(reader, JTA.getXmlName());
                    if (value != null) {
                        JTA.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                default:
                    break;
            }
        }

        final ModelNode dsAddress = parentAddress.clone();
        dsAddress.add(XA_DATASOURCE, poolName);
        dsAddress.protect();

        operation.get(OP_ADDR).set(dsAddress);
        List<ModelNode> xadatasourcePropertiesOperations = new ArrayList<ModelNode>(0);

        //elements reading
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DataSources.Tag.forName(reader.getLocalName()) == DataSources.Tag.XA_DATASOURCE) {

                        list.add(operation);
                        list.addAll(xadatasourcePropertiesOperations);
                        if (enabled) {
                            final ModelNode enableOperation = new ModelNode();
                            enableOperation.get(OP).set(ENABLE);
                            enableOperation.get(OP_ADDR).set(dsAddress);
                            enableOperation.get(PERSISTENT).set(persistEnabled);
                            list.add(enableOperation);
                        }
                        return;
                    } else {
                        if (XaDataSource.Tag.forName(reader.getLocalName()) == XaDataSource.Tag.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (XaDataSource.Tag.forName(reader.getLocalName())) {
                        case XA_DATASOURCE_PROPERTY: {
                            String name = rawAttributeText(reader, "name");
                            String value = rawElementText(reader);

                            final ModelNode configOperation = new ModelNode();
                            configOperation.get(OP).set(ADD);

                            final ModelNode configAddress = dsAddress.clone();
                            configAddress.add(XADATASOURCE_PROPERTIES.getName(), name);
                            configAddress.protect();

                            configOperation.get(OP_ADDR).set(configAddress);
                            XADATASOURCE_PROPERTY_VALUE.parseAndSetParameter(value, configOperation, reader);
                            xadatasourcePropertiesOperations.add(configOperation);
                            break;
                        }
                        case XA_DATASOURCE_CLASS: {
                            String value = rawElementText(reader);
                            XADATASOURCECLASS.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case DRIVER: {
                            String value = rawElementText(reader);
                            DATASOURCE_DRIVER.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case XA_POOL: {
                            parseXaPool(reader, operation);
                            break;
                        }
                        case NEW_CONNECTION_SQL: {
                            String value = rawElementText(reader);
                            NEW_CONNECTION_SQL.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case URL_DELIMITER: {
                            String value = rawElementText(reader);
                            URL_DELIMITER.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case URL_SELECTOR_STRATEGY_CLASS_NAME: {
                            String value = rawElementText(reader);
                            URL_SELECTOR_STRATEGY_CLASS_NAME.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case TRANSACTION_ISOLATION: {
                            String value = rawElementText(reader);
                            TRANSACTION_ISOLATION.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case SECURITY: {
                            parseDsSecurity(reader, operation);
                            break;
                        }
                        case STATEMENT: {
                            parseStatementSettings(reader, operation);
                            break;
                        }
                        case TIMEOUT: {
                            parseTimeOutSettings(reader, operation);
                            break;
                        }
                        case VALIDATION: {
                            parseValidationSetting(reader, operation);
                            break;
                        }
                        case RECOVERY: {
                            parseRecovery(reader, operation);
                            break;
                        }
                        default:
                            throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
                    }
                    break;
                }
            }
        }
        throw new ParserException(bundle.unexpectedEndOfDocument());
    }

    private void parseDsSecurity(XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException, ParserException,
            ValidateException {

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DataSource.Tag.forName(reader.getLocalName()) == DataSource.Tag.SECURITY) {

                        //it's fine, do nothing
                        return;
                    } else {
                        if (DsSecurity.Tag.forName(reader.getLocalName()) == DsSecurity.Tag.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    DsSecurity.Tag tag = DsSecurity.Tag.forName(reader.getLocalName());
                    switch (tag) {
                        case PASSWORD: {
                            String value = rawElementText(reader);
                            PASSWORD.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case USER_NAME: {
                            String value = rawElementText(reader);
                            USERNAME.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case SECURITY_DOMAIN: {
                            String value = rawElementText(reader);
                            SECURITY_DOMAIN.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case REAUTH_PLUGIN: {
                            parseExtension(reader, tag.getLocalName(), operation, REAUTHPLUGIN_CLASSNAME, REAUTHPLUGIN_PROPERTIES);
                            break;
                        }
                        default:
                            throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
                    }
                    break;
                }
            }
        }
        throw new ParserException(bundle.unexpectedEndOfDocument());
    }

    private void parseDataSource(final XMLExtendedStreamReader reader, final List<ModelNode> list, final ModelNode parentAddress) throws XMLStreamException, ParserException,
            ValidateException {

        String poolName = null;
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        boolean enabled = ENABLED.getDefaultValue().asBoolean();
        // Don't persist the enabled flag unless the user set it
        boolean persistEnabled = false;
        for (DataSource.Attribute attribute : DataSource.Attribute.values()) {
            switch (attribute) {
                case ENABLED: {
                    String value = rawAttributeText(reader, ENABLED.getXmlName());
                    if (value != null) {
                        enabled = Boolean.parseBoolean(value);
                        //ENABLED.parseAndSetParameter(value, operation, reader);
                        persistEnabled = true;
                    }
                    break;
                }
                case JNDI_NAME: {
                    final String jndiName = rawAttributeText(reader, JNDINAME.getXmlName());
                    JNDINAME.parseAndSetParameter(jndiName, operation, reader);
                    break;
                }
                case POOL_NAME: {
                    poolName = rawAttributeText(reader, POOLNAME_NAME);
                    break;
                }
                case USE_JAVA_CONTEXT: {
                    String value = rawAttributeText(reader, USE_JAVA_CONTEXT.getXmlName());
                    if (value != null) {
                        USE_JAVA_CONTEXT.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case SPY: {
                    String value = rawAttributeText(reader, SPY.getXmlName());
                    if (value != null) {
                        SPY.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case USE_CCM: {
                    String value = rawAttributeText(reader, USE_CCM.getXmlName());
                    if (value != null) {
                        USE_CCM.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case JTA: {
                    String value = rawAttributeText(reader, JTA.getXmlName());
                    if (value != null) {
                        JTA.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                default:
                    break;
            }
        }

        final ModelNode dsAddress = parentAddress.clone();
        dsAddress.add(DATA_SOURCE, poolName);
        dsAddress.protect();

        operation.get(OP_ADDR).set(dsAddress);


        List<ModelNode> configPropertiesOperations = new ArrayList<ModelNode>(0);
        //elements reading
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DataSources.Tag.forName(reader.getLocalName()) == DataSources.Tag.DATASOURCE) {

                        list.add(operation);
                        list.addAll(configPropertiesOperations);
                        if (enabled) {
                            final ModelNode enableOperation = new ModelNode();
                            enableOperation.get(OP).set(ENABLE);
                            enableOperation.get(OP_ADDR).set(dsAddress);
                            enableOperation.get(PERSISTENT).set(persistEnabled);
                            list.add(enableOperation);
                        }
                        return;
                    } else {
                        if (DataSource.Tag.forName(reader.getLocalName()) == DataSource.Tag.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (DataSource.Tag.forName(reader.getLocalName())) {
                        case CONNECTION_PROPERTY: {
                            String name = rawAttributeText(reader, "name");
                            String value = rawElementText(reader);

                            final ModelNode configOperation = new ModelNode();
                            configOperation.get(OP).set(ADD);

                            final ModelNode configAddress = dsAddress.clone();
                            configAddress.add(CONNECTION_PROPERTIES.getName(), name);
                            configAddress.protect();

                            configOperation.get(OP_ADDR).set(configAddress);
                            CONNECTION_PROPERTY_VALUE.parseAndSetParameter(value, configOperation, reader);
                            configPropertiesOperations.add(configOperation);
                            break;
                        }
                        case CONNECTION_URL: {
                            String value = rawElementText(reader);
                            CONNECTION_URL.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case DRIVER_CLASS: {
                            String value = rawElementText(reader);
                            DRIVER_CLASS.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case DATASOURCE_CLASS: {
                            String value = rawElementText(reader);
                            DATASOURCE_CLASS.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case DRIVER: {
                            String value = rawElementText(reader);
                            DATASOURCE_DRIVER.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case POOL: {
                            parsePool(reader, operation);
                            break;
                        }
                        case NEW_CONNECTION_SQL: {
                            String value = rawElementText(reader);
                            NEW_CONNECTION_SQL.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case URL_DELIMITER: {
                            String value = rawElementText(reader);
                            URL_DELIMITER.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case URL_SELECTOR_STRATEGY_CLASS_NAME: {
                            String value = rawElementText(reader);
                            URL_SELECTOR_STRATEGY_CLASS_NAME.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case TRANSACTION_ISOLATION: {
                            String value = rawElementText(reader);
                            TRANSACTION_ISOLATION.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case SECURITY: {
                            parseDsSecurity(reader, operation);
                            break;
                        }
                        case STATEMENT: {
                            parseStatementSettings(reader, operation);
                            break;
                        }
                        case TIMEOUT: {
                            parseTimeOutSettings(reader, operation);
                            break;
                        }
                        case VALIDATION: {
                            parseValidationSetting(reader, operation);
                            break;
                        }
                        default:
                            throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
                    }
                    break;
                }
            }
        }
        throw new ParserException(bundle.unexpectedEndOfDocument());
    }


    private void parsePool(XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException, ParserException,
            ValidateException {

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DataSource.Tag.forName(reader.getLocalName()) == DataSource.Tag.POOL) {
                        return;
                        //it's fine. Do nothing

                    } else {
                        if (CommonPool.Tag.forName(reader.getLocalName()) == CommonPool.Tag.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (CommonPool.Tag.forName(reader.getLocalName())) {
                        case MAX_POOL_SIZE: {
                            String value = rawElementText(reader);
                            MAX_POOL_SIZE.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case MIN_POOL_SIZE: {
                            String value = rawElementText(reader);
                            MIN_POOL_SIZE.parseAndSetParameter(value, operation, reader);
                            break;
                        }

                        case PREFILL: {
                            String value = rawElementText(reader);
                            POOL_PREFILL.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case USE_STRICT_MIN: {
                            String value = rawElementText(reader);
                            POOL_USE_STRICT_MIN.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case FLUSH_STRATEGY: {
                            String value = rawElementText(reader);
                            POOL_FLUSH_STRATEGY.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        default:
                            throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
                    }
                    break;
                }
            }
        }
        throw new ParserException(bundle.unexpectedEndOfDocument());
    }


    private void parseXaPool(XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException, ParserException,
            ValidateException {

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (XaDataSource.Tag.forName(reader.getLocalName()) == XaDataSource.Tag.XA_POOL) {

                        return;
                        //it's fine. Do nothing

                    } else {
                        if (CommonXaPool.Tag.forName(reader.getLocalName()) == CommonXaPool.Tag.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (CommonXaPool.Tag.forName(reader.getLocalName())) {
                        case MAX_POOL_SIZE: {
                            String value = rawElementText(reader);
                            MAX_POOL_SIZE.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case MIN_POOL_SIZE: {
                            String value = rawElementText(reader);
                            MIN_POOL_SIZE.parseAndSetParameter(value, operation, reader);
                            break;
                        }

                        case PREFILL: {
                            String value = rawElementText(reader);
                            POOL_PREFILL.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case USE_STRICT_MIN: {
                            String value = rawElementText(reader);
                            POOL_USE_STRICT_MIN.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case FLUSH_STRATEGY: {
                            String value = rawElementText(reader);
                            POOL_FLUSH_STRATEGY.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case INTERLEAVING: {
                            //tag presence is sufficient to set it to true
                            String value = rawElementText(reader);
                            value = value == null ? "true" : value;
                            INTERLEAVING.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case IS_SAME_RM_OVERRIDE: {
                            String value = rawElementText(reader);
                            SAME_RM_OVERRIDE.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case NO_TX_SEPARATE_POOLS: {
                            //tag presence is sufficient to set it to true
                            String value = rawElementText(reader);
                            value = value == null ? "true" : value;
                            NOTXSEPARATEPOOL.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case PAD_XID: {
                            String value = rawElementText(reader);
                            PAD_XID.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case WRAP_XA_RESOURCE: {
                            String value = rawElementText(reader);
                            WRAP_XA_RESOURCE.parseAndSetParameter(value, operation, reader);
                            break;
                        }

                        default:
                            throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
                    }
                    break;
                }
            }
        }
        throw new ParserException(bundle.unexpectedEndOfDocument());
    }


    private void parseRecovery(XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException, ParserException,
            ValidateException {

        for (Recovery.Attribute attribute : Recovery.Attribute.values()) {
            switch (attribute) {
                case NO_RECOVERY: {
                    String value = rawAttributeText(reader, NO_RECOVERY.getXmlName());
                    NO_RECOVERY.parseAndSetParameter(value, operation, reader);
                    break;
                }
                default:
                    break;
            }
        }

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (XaDataSource.Tag.forName(reader.getLocalName()) == XaDataSource.Tag.RECOVERY) {
                        return;
                    } else {
                        if (Recovery.Tag.forName(reader.getLocalName()) == Recovery.Tag.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    Recovery.Tag tag = Recovery.Tag.forName(reader.getLocalName());
                    switch (tag) {
                        case RECOVER_CREDENTIAL: {
                            parseCredential(reader, operation);
                            break;
                        }
                        case RECOVER_PLUGIN: {
                            parseExtension(reader, tag.getLocalName(), operation, RECOVERLUGIN_CLASSNAME, RECOVERLUGIN_PROPERTIES);
                            break;
                        }
                        default:
                            throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
                    }
                    break;
                }
            }
        }
        throw new ParserException(bundle.unexpectedEndOfDocument());
    }

    private void parseCredential(XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException, ParserException,
            ValidateException {

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DataSource.Tag.forName(reader.getLocalName()) == DataSource.Tag.SECURITY ||
                            Recovery.Tag.forName(reader.getLocalName()) == Recovery.Tag.RECOVER_CREDENTIAL) {

                        return;
                    } else {
                        if (Credential.Tag.forName(reader.getLocalName()) == Credential.Tag.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (Credential.Tag.forName(reader.getLocalName())) {
                        case PASSWORD: {
                            String value = rawElementText(reader);
                            RECOVERY_PASSWORD.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case USER_NAME: {
                            String value = rawElementText(reader);
                            RECOVERY_USERNAME.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case SECURITY_DOMAIN: {
                            String value = rawElementText(reader);
                            RECOVERY_SECURITY_DOMAIN.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        default:
                            throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
                    }
                    break;
                }
            }
        }
        throw new ParserException(bundle.unexpectedEndOfDocument());
    }

    private void parseValidationSetting(XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException, ParserException,
            ValidateException {

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DataSource.Tag.forName(reader.getLocalName()) == DataSource.Tag.VALIDATION) {

                        return;

                    } else {
                        if (Validation.Tag.forName(reader.getLocalName()) == Validation.Tag.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    Validation.Tag currTag = Validation.Tag.forName(reader.getLocalName());
                    switch (currTag) {
                        case BACKGROUND_VALIDATION: {
                            String value = rawElementText(reader);
                            BACKGROUNDVALIDATION.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case BACKGROUND_VALIDATION_MILLIS: {
                            String value = rawElementText(reader);
                            BACKGROUNDVALIDATIONMILLIS.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case CHECK_VALID_CONNECTION_SQL: {
                            String value = rawElementText(reader);
                            CHECKVALIDCONNECTIONSQL.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case EXCEPTION_SORTER: {
                            parseExtension(reader, currTag.getLocalName(), operation, EXCEPTIONSORTERCLASSNAME, EXCEPTIONSORTER_PROPERTIES);
                            break;
                        }
                        case STALE_CONNECTION_CHECKER: {
                            parseExtension(reader, currTag.getLocalName(), operation, STALECONNECTIONCHECKERCLASSNAME, STALECONNECTIONCHECKER_PROPERTIES);
                            break;
                        }
                        case USE_FAST_FAIL: {
                            String value = rawElementText(reader);
                            USE_FAST_FAIL.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case VALIDATE_ON_MATCH: {
                            String value = rawElementText(reader);
                            VALIDATEONMATCH.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case VALID_CONNECTION_CHECKER: {
                            parseExtension(reader, currTag.getLocalName(), operation, VALIDCONNECTIONCHECKERCLASSNAME, VALIDCONNECTIONCHECKER_PROPERTIES);
                            break;
                        }
                        default: {
                            throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
                        }
                    }
                    break;
                }
            }
        }
        throw new ParserException(bundle.unexpectedEndOfDocument());
    }

    private void parseTimeOutSettings(XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException, ParserException,
            ValidateException {

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DataSource.Tag.forName(reader.getLocalName()) == DataSource.Tag.TIMEOUT) {

                        return;
                    } else {
                        if (TimeOut.Tag.forName(reader.getLocalName()) == TimeOut.Tag.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (TimeOut.Tag.forName(reader.getLocalName())) {
                        case ALLOCATION_RETRY: {
                            String value = rawElementText(reader);
                            ALLOCATION_RETRY.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case ALLOCATION_RETRY_WAIT_MILLIS: {
                            String value = rawElementText(reader);
                            ALLOCATION_RETRY_WAIT_MILLIS.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case BLOCKING_TIMEOUT_MILLIS: {
                            String value = rawElementText(reader);
                            BLOCKING_TIMEOUT_WAIT_MILLIS.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case IDLE_TIMEOUT_MINUTES: {
                            String value = rawElementText(reader);
                            IDLETIMEOUTMINUTES.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case QUERY_TIMEOUT: {
                            String value = rawElementText(reader);
                            QUERYTIMEOUT.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case SET_TX_QUERY_TIMEOUT: {
                            //tag presence is sufficient to set it to true
                            String value = rawElementText(reader);
                            value = value == null ? "true" : value;
                            SETTXQUERYTIMEOUT.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case USE_TRY_LOCK: {
                            String value = rawElementText(reader);
                            USETRYLOCK.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case XA_RESOURCE_TIMEOUT: {
                            String value = rawElementText(reader);
                            XA_RESOURCE_TIMEOUT.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        default:
                            throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
                    }
                    break;
                }
            }
        }
        throw new ParserException(bundle.unexpectedEndOfDocument());
    }

    private void parseStatementSettings(XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException, ParserException,
            ValidateException {

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DataSource.Tag.forName(reader.getLocalName()) == DataSource.Tag.STATEMENT) {

                        return;
                    } else {
                        if (Statement.Tag.forName(reader.getLocalName()) == Statement.Tag.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (Statement.Tag.forName(reader.getLocalName())) {
                        case PREPARED_STATEMENT_CACHE_SIZE: {
                            String value = rawElementText(reader);
                            PREPAREDSTATEMENTSCACHESIZE.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case TRACK_STATEMENTS: {
                            String value = rawElementText(reader);
                            TRACKSTATEMENTS.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case SHARE_PREPARED_STATEMENTS: {
                            //tag presence is sufficient to set it to true
                            String value = rawElementText(reader);
                            value = value == null ? "true" : value;
                            SHAREPREPAREDSTATEMENTS.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        default:
                            throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
                    }
                    break;
                }
            }
        }
        throw new ParserException(bundle.unexpectedEndOfDocument());
    }

    /**
     * A Tag.
     *
     * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
     */
    public enum Tag {
        /**
         * always first
         */
        UNKNOWN(null),

        /**
         * jboss-ra tag name
         */
        DATASOURCES("datasources");

        private final String name;

        /**
         * Create a new Tag.
         *
         * @param name a name
         */
        Tag(final String name) {
            this.name = name;
        }

        /**
         * Get the local name of this element.
         *
         * @return the local name
         */
        public String getLocalName() {
            return name;
        }

        private static final Map<String, Tag> MAP;

        static {
            final Map<String, Tag> map = new HashMap<String, Tag>();
            for (Tag element : values()) {
                final String name = element.getLocalName();
                if (name != null)
                    map.put(name, element);
            }
            MAP = map;
        }

        /**
         * Static method to get enum instance given localName string
         *
         * @param localName a string used as localname (typically tag name as defined in xsd)
         * @return the enum instance
         */
        public static Tag forName(String localName) {
            final Tag element = MAP.get(localName);
            return element == null ? UNKNOWN : element;
        }

    }
}
