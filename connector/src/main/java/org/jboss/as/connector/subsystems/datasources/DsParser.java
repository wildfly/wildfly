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
import static org.jboss.as.connector.pool.Constants.POOL_USE_STRICT_MIN;
import static org.jboss.as.connector.pool.Constants.USE_FAST_FAIL;
import static org.jboss.as.connector.subsystems.datasources.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.datasources.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.datasources.Constants.CHECKVALIDCONNECTIONSQL;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_DRIVER;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATA_SOURCE;
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
import static org.jboss.as.connector.subsystems.datasources.Constants.FLUSH_STRATEGY;
import static org.jboss.as.connector.subsystems.datasources.Constants.INTERLEAVING;
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
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVER_PLUGIN_CLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVER_PLUGIN_PROPERTIES;
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
import static org.jboss.as.connector.subsystems.datasources.Constants.WRAP_XA_RESOURCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.XADATASOURCECLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.XADATASOURCEPROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_DATASOURCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_RESOURCE_TIMEOUT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

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


    public void parse(final XMLStreamReader reader, final List<ModelNode> list, ModelNode parentAddress) throws Exception {

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

    private void parseDataSources(final XMLStreamReader reader, final List<ModelNode> list, final ModelNode parentAddress) throws XMLStreamException, ParserException,
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

    private void parseDriver(final XMLStreamReader reader, final List<ModelNode> list, final ModelNode parentAddress) throws XMLStreamException, ParserException,
            ValidateException {
        final ModelNode driverAddress = parentAddress.clone();
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);

        String driverName = null;
        for (org.jboss.jca.common.api.metadata.ds.Driver.Attribute attribute : Driver.Attribute.values()) {
            switch (attribute) {

                case NAME: {
                    driverName = attributeAsString(reader, attribute.getLocalName());
                    if (driverName != null && driverName.trim().length()!=0) operation.get(DRIVER_NAME).set(driverName);
                    break;
                }
                case MAJOR_VERSION: {
                    final Integer value = attributeAsInt(reader, attribute.getLocalName());
                    if (value != null) operation.get(DRIVER_MAJOR_VERSION).set(value);
                    break;
                }
                case MINOR_VERSION: {
                    final Integer value = attributeAsInt(reader, attribute.getLocalName());
                    if (value != null) operation.get(DRIVER_MINOR_VERSION).set(value);
                    break;
                }
                case MODULE: {
                    final String value = attributeAsString(reader, attribute.getLocalName());
                    if (value != null && value.trim().length() != 0) operation.get(DRIVER_MODULE_NAME).set(value);
                    break;
                }
                default:
                    break;
            }
        }
        driverAddress.add(JDBC_DRIVER, driverName);
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
                            operation.get(DRIVER_DATASOURCE_CLASS_NAME).set(elementAsString(reader));
                            break;
                        }
                        case XA_DATASOURCE_CLASS: {
                            operation.get(DRIVER_XA_DATASOURCE_CLASS_NAME).set(elementAsString(reader));
                            break;
                        }
                        case DRIVER_CLASS: {
                            operation.get(DRIVER_CLASS_NAME).set(elementAsString(reader));
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

    private void parseXADataSource(XMLStreamReader reader, final List<ModelNode> list, final ModelNode parentAddress) throws XMLStreamException, ParserException,
            ValidateException {

        String jndiName = null;
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);

        for (DataSource.Attribute attribute : DataSource.Attribute.values()) {
            switch (attribute) {
                case ENABLED: {
                    final Boolean value = attributeAsBoolean(reader, attribute.getLocalName());
                    if (value != null)
                        operation.get(ENABLED).set(value);
                    break;
                }
                case JNDI_NAME: {
                    jndiName = attributeAsString(reader, attribute.getLocalName());
                    if (jndiName != null && jndiName.trim().length() != 0) operation.get(JNDINAME).set(jndiName);
                    break;
                }
                case POOL_NAME: {
                    final String value = attributeAsString(reader, attribute.getLocalName());
                    if (value != null && value.trim().length() != 0)
                        operation.get(POOLNAME).set(value);
                    break;
                }
                case USE_JAVA_CONTEXT: {
                    final Boolean value = attributeAsBoolean(reader, attribute.getLocalName());
                    if (value != null)

                        operation.get(USE_JAVA_CONTEXT).set(value);
                    break;
                }
                case SPY: {
                    final Boolean value = attributeAsBoolean(reader, attribute.getLocalName());
                    if (value != null)
                        operation.get(SPY).set(value);
                    break;
                }
                case USE_CCM: {
                    final Boolean value = attributeAsBoolean(reader, attribute.getLocalName());
                    if (value != null)

                        operation.get(USE_CCM).set(value);
                    break;
                }
                case JTA: {
                    final Boolean value = attributeAsBoolean(reader, attribute.getLocalName());
                    if (value != null)
                        operation.get(JTA).set(value);
                    break;
                }
                default:
                    break;
            }
        }

        final ModelNode dsAddress = parentAddress.clone();
        dsAddress.add(XA_DATASOURCE, jndiName);
        dsAddress.protect();

        operation.get(OP_ADDR).set(dsAddress);

        //elements reading
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DataSources.Tag.forName(reader.getLocalName()) == DataSources.Tag.XA_DATASOURCE) {

                        list.add(operation);
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
                            operation.get(XADATASOURCEPROPERTIES, attributeAsString(reader, "name")).set(elementAsString(reader));
                            break;
                        }
                        case XA_DATASOURCE_CLASS: {
                            operation.get(XADATASOURCECLASS).set(elementAsString(reader));
                            break;
                        }
                        case DRIVER: {
                            operation.get(DATASOURCE_DRIVER).set(elementAsString(reader));
                            break;
                        }
                        case XA_POOL: {
                            parseXaPool(reader, operation);
                            break;
                        }
                        case NEW_CONNECTION_SQL: {
                            operation.get(NEW_CONNECTION_SQL).set(elementAsString(reader));
                            break;
                        }
                        case URL_DELIMITER: {
                            operation.get(URL_DELIMITER).set(elementAsString(reader));
                            break;
                        }
                        case URL_SELECTOR_STRATEGY_CLASS_NAME: {
                            operation.get(URL_SELECTOR_STRATEGY_CLASS_NAME).set(elementAsString(reader));
                            break;
                        }
                        case TRANSACTION_ISOLATION: {
                            operation.get(TRANSACTION_ISOLOATION).set(elementAsString(reader));
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

    private void parseDsSecurity(XMLStreamReader reader, final ModelNode operation) throws XMLStreamException, ParserException,
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
                            operation.get(PASSWORD).set(elementAsString(reader));
                            break;
                        }
                        case USER_NAME: {
                            operation.get(USERNAME).set(elementAsString(reader));
                            break;
                        }
                        case SECURITY_DOMAIN: {
                            operation.get(SECURITY_DOMAIN).set(elementAsString(reader));
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

    private void parseDataSource(final XMLStreamReader reader, final List<ModelNode> list, final ModelNode parentAddress) throws XMLStreamException, ParserException,
            ValidateException {

        String jndiName = null;
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);

        for (DataSource.Attribute attribute : DataSource.Attribute.values()) {
            switch (attribute) {
                case ENABLED: {
                    final Boolean value = attributeAsBoolean(reader, attribute.getLocalName());
                    if (value != null) operation.get(ENABLED).set(value);
                    break;
                }
                case JNDI_NAME: {
                    jndiName = attributeAsString(reader, attribute.getLocalName());
                    if (jndiName != null && jndiName.trim().length() != 0) operation.get(JNDINAME).set(jndiName);
                    break;
                }
                case POOL_NAME: {
                    final String
                            value = attributeAsString(reader, attribute.getLocalName());
                    if (value != null && value.trim().length() != 0) operation.get(POOLNAME).set(value);
                    break;
                }
                case USE_JAVA_CONTEXT: {
                    final Boolean value = attributeAsBoolean(reader, attribute.getLocalName());
                    if (value != null)
                        operation.get(USE_JAVA_CONTEXT).set(value);
                    break;
                }
                case SPY: {
                    final Boolean value = attributeAsBoolean(reader, attribute.getLocalName());
                    if (value != null)
                        operation.get(SPY).set(value);
                    break;
                }
                case USE_CCM: {
                    final Boolean value = attributeAsBoolean(reader, attribute.getLocalName());
                    if (value != null)
                        operation.get(USE_CCM).set(value);
                    break;
                }
                case JTA: {
                    final Boolean value = attributeAsBoolean(reader, attribute.getLocalName());
                    if (value != null)
                        operation.get(JTA).set(value);
                    break;
                }
                default:
                    break;
            }
        }

        final ModelNode dsAddress = parentAddress.clone();
        dsAddress.add(DATA_SOURCE, jndiName);
        dsAddress.protect();

        operation.get(OP_ADDR).set(dsAddress);


        //elements reading
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DataSources.Tag.forName(reader.getLocalName()) == DataSources.Tag.DATASOURCE) {

                        list.add(operation);
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
                            operation.get(Constants.CONNECTION_PROPERTIES, attributeAsString(reader, "name")).set(elementAsString(reader));
                            break;
                        }
                        case CONNECTION_URL: {
                            operation.get(Constants.CONNECTION_URL).set(elementAsString(reader));
                            break;
                        }
                        case DRIVER_CLASS: {
                            operation.get(Constants.DRIVER_CLASS_NAME).set(elementAsString(reader));
                            break;
                        }
                        case DATASOURCE_CLASS: {
                            operation.get(Constants.DATASOURCE_CLASS).set(elementAsString(reader));
                            break;
                        }
                        case DRIVER: {
                            operation.get(DATASOURCE_DRIVER).set(elementAsString(reader));
                            break;
                        }
                        case POOL: {
                            parsePool(reader, operation);
                            break;
                        }
                        case NEW_CONNECTION_SQL: {
                            operation.get(NEW_CONNECTION_SQL).set(elementAsString(reader));
                            break;
                        }
                        case URL_DELIMITER: {
                            operation.get(URL_DELIMITER).set(elementAsString(reader));
                            break;
                        }
                        case URL_SELECTOR_STRATEGY_CLASS_NAME: {
                            operation.get(URL_SELECTOR_STRATEGY_CLASS_NAME).set(elementAsString(reader));
                            break;
                        }
                        case TRANSACTION_ISOLATION: {
                            operation.get(TRANSACTION_ISOLOATION).set(elementAsString(reader));
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


    private void parsePool(XMLStreamReader reader, final ModelNode operation) throws XMLStreamException, ParserException,
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
                            operation.get(MAX_POOL_SIZE).set(elementAsInteger(reader));
                            break;
                        }
                        case MIN_POOL_SIZE: {
                            operation.get(MIN_POOL_SIZE).set(elementAsInteger(reader));
                            break;
                        }

                        case PREFILL: {
                            operation.get(DATASOURCE_DRIVER).set(elementAsBoolean(reader));
                            break;
                        }
                        case USE_STRICT_MIN: {
                            operation.get(POOL_USE_STRICT_MIN).set(elementAsBoolean(reader));
                            break;
                        }
                        case FLUSH_STRATEGY: {
                            operation.get(FLUSH_STRATEGY).set(elementAsString(reader));
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


    private void parseXaPool(XMLStreamReader reader, final ModelNode operation) throws XMLStreamException, ParserException,
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
                            operation.get(MAX_POOL_SIZE).set(elementAsInteger(reader));
                            break;
                        }
                        case MIN_POOL_SIZE: {
                            operation.get(MIN_POOL_SIZE).set(elementAsInteger(reader));
                            break;
                        }

                        case PREFILL: {
                            operation.get(DATASOURCE_DRIVER).set(elementAsBoolean(reader));
                            break;
                        }
                        case USE_STRICT_MIN: {
                            operation.get(POOL_USE_STRICT_MIN).set(elementAsBoolean(reader));
                            break;
                        }
                        case FLUSH_STRATEGY: {
                            operation.get(FLUSH_STRATEGY).set(elementAsString(reader));
                            break;
                        }
                        case INTERLEAVING: {
                            operation.get(INTERLEAVING).set(elementAsBoolean(reader));
                            break;
                        }
                        case IS_SAME_RM_OVERRIDE: {
                            operation.get(SAME_RM_OVERRIDE).set(elementAsBoolean(reader));
                            break;
                        }
                        case NO_TX_SEPARATE_POOLS: {
                            operation.get(NOTXSEPARATEPOOL).set(elementAsBoolean(reader));
                            break;
                        }
                        case PAD_XID: {
                            operation.get(PAD_XID).set(elementAsBoolean(reader));
                            break;
                        }
                        case WRAP_XA_RESOURCE: {
                            operation.get(WRAP_XA_RESOURCE).set(elementAsBoolean(reader));
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


    private void parseRecovery(XMLStreamReader reader, final ModelNode operation) throws XMLStreamException, ParserException,
            ValidateException {

        for (Recovery.Attribute attribute : Recovery.Attribute.values()) {
            switch (attribute) {
                case NO_RECOVERY: {
                    final Boolean value = attributeAsBoolean(reader, attribute.getLocalName());
                                        if (value != null)

                    operation.get(NO_RECOVERY).set(value);
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
                            parseExtension(reader, tag.getLocalName(), operation, RECOVER_PLUGIN_CLASSNAME, RECOVER_PLUGIN_PROPERTIES);
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

    private void parseCredential(XMLStreamReader reader, final ModelNode operation) throws XMLStreamException, ParserException,
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
                            operation.get(PASSWORD).set(elementAsString(reader));
                            break;
                        }
                        case USER_NAME: {
                            operation.get(USERNAME).set(elementAsString(reader));
                            break;
                        }
                        case SECURITY_DOMAIN: {
                            operation.get(SECURITY_DOMAIN).set(elementAsString(reader));
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

    private void parseValidationSetting(XMLStreamReader reader, final ModelNode operation) throws XMLStreamException, ParserException,
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
                            operation.get(BACKGROUNDVALIDATION).set(elementAsBoolean(reader));
                            break;
                        }
                        case BACKGROUND_VALIDATION_MILLIS: {
                            operation.get(BACKGROUNDVALIDATIONMILLIS).set(elementAsLong(reader));
                            break;
                        }
                        case CHECK_VALID_CONNECTION_SQL: {
                            operation.get(CHECKVALIDCONNECTIONSQL).set(elementAsString(reader));
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
                            operation.get(USE_FAST_FAIL).set(elementAsBoolean(reader));
                            break;
                        }
                        case VALIDATE_ON_MATCH: {
                            operation.get(VALIDATEONMATCH).set(elementAsBoolean(reader));
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

    private void parseTimeOutSettings(XMLStreamReader reader, final ModelNode operation) throws XMLStreamException, ParserException,
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
                            operation.get(ALLOCATION_RETRY).set(elementAsInteger(reader));
                            break;
                        }
                        case ALLOCATION_RETRY_WAIT_MILLIS: {
                            operation.get(ALLOCATION_RETRY_WAIT_MILLIS).set(elementAsLong(reader));
                            break;
                        }
                        case BLOCKING_TIMEOUT_MILLIS: {
                            operation.get(BLOCKING_TIMEOUT_WAIT_MILLIS).set(elementAsLong(reader));
                            break;
                        }
                        case IDLE_TIMEOUT_MINUTES: {
                            operation.get(IDLETIMEOUTMINUTES).set(elementAsLong(reader));
                            break;
                        }
                        case QUERY_TIMEOUT: {
                            operation.get(QUERYTIMEOUT).set(elementAsLong(reader));
                            break;
                        }
                        case SET_TX_QUERY_TIMEOUT: {
                            operation.get(SETTXQUERYTIMEOUT).set(elementAsBoolean(reader));
                            break;
                        }
                        case USE_TRY_LOCK: {
                            operation.get(USETRYLOCK).set(elementAsLong(reader));
                            break;
                        }
                        case XA_RESOURCE_TIMEOUT: {
                            operation.get(XA_RESOURCE_TIMEOUT).set(elementAsInteger(reader));
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

    private void parseStatementSettings(XMLStreamReader reader, final ModelNode operation) throws XMLStreamException, ParserException,
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
                            operation.get(PREPAREDSTATEMENTSCACHESIZE).set(elementAsLong(reader));
                            break;
                        }
                        case TRACK_STATEMENTS: {
                            operation.get(TRACKSTATEMENTS).set(elementAsString(reader));
                            break;
                        }
                        case SHARE_PREPARED_STATEMENTS: {
                            operation.get(SHAREPREPAREDSTATEMENTS).set(elementAsBoolean(reader));
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
