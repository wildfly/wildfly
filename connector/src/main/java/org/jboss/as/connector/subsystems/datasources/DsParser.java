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
import static org.jboss.as.connector.subsystems.common.pool.Constants.BACKGROUNDVALIDATION;
import static org.jboss.as.connector.subsystems.common.pool.Constants.BACKGROUNDVALIDATIONMILLIS;
import static org.jboss.as.connector.subsystems.common.pool.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_DECREMENTER_CLASS;
import static org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_DECREMENTER_PROPERTIES;
import static org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_INCREMENTER_CLASS;
import static org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_INCREMENTER_PROPERTIES;
import static org.jboss.as.connector.subsystems.common.pool.Constants.IDLETIMEOUTMINUTES;
import static org.jboss.as.connector.subsystems.common.pool.Constants.INITIAL_POOL_SIZE;
import static org.jboss.as.connector.subsystems.common.pool.Constants.MAX_POOL_SIZE;
import static org.jboss.as.connector.subsystems.common.pool.Constants.MIN_POOL_SIZE;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_FAIR;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_FLUSH_STRATEGY;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_PREFILL;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_USE_STRICT_MIN;
import static org.jboss.as.connector.subsystems.common.pool.Constants.USE_FAST_FAIL;
import static org.jboss.as.connector.subsystems.datasources.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.datasources.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.datasources.Constants.ALLOW_MULTIPLE_USERS;
import static org.jboss.as.connector.subsystems.datasources.Constants.AUTHENTICATION_CONTEXT;
import static org.jboss.as.connector.subsystems.datasources.Constants.CHECK_VALID_CONNECTION_SQL;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTABLE;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_LISTENER_CLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_LISTENER_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_PROPERTY_VALUE;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_URL;
import static org.jboss.as.connector.subsystems.datasources.Constants.CREDENTIAL_REFERENCE;
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
import static org.jboss.as.connector.subsystems.datasources.Constants.ELYTRON_ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.ENLISTMENT_TRACE;
import static org.jboss.as.connector.subsystems.datasources.Constants.EXCEPTION_SORTER_CLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.EXCEPTION_SORTER_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.INTERLEAVING;
import static org.jboss.as.connector.subsystems.datasources.Constants.JDBC_DRIVER_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.JNDI_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.JTA;
import static org.jboss.as.connector.subsystems.datasources.Constants.MCP;
import static org.jboss.as.connector.subsystems.datasources.Constants.MODULE_SLOT;
import static org.jboss.as.connector.subsystems.datasources.Constants.NEW_CONNECTION_SQL;
import static org.jboss.as.connector.subsystems.datasources.Constants.NO_RECOVERY;
import static org.jboss.as.connector.subsystems.datasources.Constants.NO_TX_SEPARATE_POOL;
import static org.jboss.as.connector.subsystems.datasources.Constants.PAD_XID;
import static org.jboss.as.connector.subsystems.datasources.Constants.PASSWORD;
import static org.jboss.as.connector.subsystems.datasources.Constants.POOLNAME_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.PREPARED_STATEMENTS_CACHE_SIZE;
import static org.jboss.as.connector.subsystems.datasources.Constants.QUERY_TIMEOUT;
import static org.jboss.as.connector.subsystems.datasources.Constants.REAUTHPLUGIN_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.REAUTH_PLUGIN_CLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_AUTHENTICATION_CONTEXT;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_CREDENTIAL_REFERENCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_ELYTRON_ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_PASSWORD;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_USERNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVER_PLUGIN_CLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVER_PLUGIN_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.SAME_RM_OVERRIDE;
import static org.jboss.as.connector.subsystems.datasources.Constants.SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.datasources.Constants.SET_TX_QUERY_TIMEOUT;
import static org.jboss.as.connector.subsystems.datasources.Constants.SHARE_PREPARED_STATEMENTS;
import static org.jboss.as.connector.subsystems.datasources.Constants.SPY;
import static org.jboss.as.connector.subsystems.datasources.Constants.STALE_CONNECTION_CHECKER_CLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.STALE_CONNECTION_CHECKER_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.TRACKING;
import static org.jboss.as.connector.subsystems.datasources.Constants.TRACK_STATEMENTS;
import static org.jboss.as.connector.subsystems.datasources.Constants.TRANSACTION_ISOLATION;
import static org.jboss.as.connector.subsystems.datasources.Constants.URL_DELIMITER;
import static org.jboss.as.connector.subsystems.datasources.Constants.URL_PROPERTY;
import static org.jboss.as.connector.subsystems.datasources.Constants.URL_SELECTOR_STRATEGY_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.USERNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.USE_CCM;
import static org.jboss.as.connector.subsystems.datasources.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.connector.subsystems.datasources.Constants.USE_TRY_LOCK;
import static org.jboss.as.connector.subsystems.datasources.Constants.VALIDATE_ON_MATCH;
import static org.jboss.as.connector.subsystems.datasources.Constants.VALID_CONNECTION_CHECKER_CLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.VALID_CONNECTION_CHECKER_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.WRAP_XA_RESOURCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.XADATASOURCE_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.XADATASOURCE_PROPERTY_VALUE;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_DATASOURCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_DATASOURCE_CLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_RESOURCE_TIMEOUT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PERSISTENT;
import static org.jboss.as.controller.parsing.ParseUtils.isNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.connector.metadata.api.common.Credential;
import org.jboss.as.connector.metadata.api.ds.DsSecurity;
import org.jboss.as.connector.util.AbstractParser;
import org.jboss.as.connector.util.ParserException;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.CommonBundle;
import org.jboss.jca.common.api.metadata.Defaults;
import org.jboss.jca.common.api.metadata.common.Capacity;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.metadata.ds.DataSources;
import org.jboss.jca.common.api.metadata.ds.Driver;
import org.jboss.jca.common.api.metadata.ds.DsPool;
import org.jboss.jca.common.api.metadata.ds.DsXaPool;
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
                            switch (Namespace.forUri(reader.getNamespaceURI())) {
                                case DATASOURCES_1_1:
                                case DATASOURCES_2_0:
                                    parseDataSource_1_0(reader, list, parentAddress);
                                    break;
                                case DATASOURCES_1_2:
                                    parseDataSource_1_2(reader, list, parentAddress);
                                    break;
                                case DATASOURCES_3_0:
                                    parseDataSource_3_0(reader, list, parentAddress);
                                    break;
                                default:
                                    parseDataSource_4_0(reader, list, parentAddress);
                                    break;
                            }
                            break;
                        }
                        case XA_DATASOURCE: {
                            switch (Namespace.forUri(reader.getNamespaceURI())) {
                                case DATASOURCES_1_1:
                                case DATASOURCES_2_0:
                                    parseXADataSource_1_0(reader, list, parentAddress);
                                    break;
                                case DATASOURCES_1_2:
                                    parseXADataSource_1_2(reader, list, parentAddress);
                                    break;
                                case DATASOURCES_3_0:
                                    parseXADataSource_3_0(reader, list, parentAddress);
                                    break;
                                default:
                                    parseXADataSource_4_0(reader, list, parentAddress);
                                    break;
                            }
                            break;

                        }
                        case DRIVERS: {
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

    private void parseDataSource_1_2(final XMLExtendedStreamReader reader, final List<ModelNode> list, final ModelNode parentAddress) throws XMLStreamException, ParserException,
            ValidateException {

        String poolName = null;
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        boolean enabled = Defaults.ENABLED.booleanValue();
        // Persist the enabled flag because xml default is != from DMR default
        boolean persistEnabled = true;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {

            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            }
            final DataSource.Attribute attribute = DataSource.Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED: {
                    final String value = rawAttributeText(reader, ENABLED.getXmlName());
                    if (value != null) {
                        enabled = Boolean.parseBoolean(value);
                        //ENABLED.parseAndSetParameter(value, operation, reader);
                        persistEnabled = true;
                    }
                    break;
                }
                case JNDI_NAME: {
                    final String jndiName = rawAttributeText(reader, JNDI_NAME.getXmlName());
                    JNDI_NAME.parseAndSetParameter(jndiName, operation, reader);
                    break;
                }
                case POOL_NAME: {
                    poolName = rawAttributeText(reader, POOLNAME_NAME);
                    break;
                }
                case USE_JAVA_CONTEXT: {
                    final String value = rawAttributeText(reader, USE_JAVA_CONTEXT.getXmlName());
                    if (value != null) {
                        USE_JAVA_CONTEXT.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case SPY: {
                    final String value = rawAttributeText(reader, SPY.getXmlName());
                    if (value != null) {
                        SPY.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case USE_CCM: {
                    final String value = rawAttributeText(reader, USE_CCM.getXmlName());
                    if (value != null) {
                        USE_CCM.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case JTA: {
                    final String value = rawAttributeText(reader, JTA.getXmlName());
                    if (value != null) {
                        JTA.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case CONNECTABLE: {
                    final String value = rawAttributeText(reader, CONNECTABLE.getXmlName());
                    if (value != null) {
                        CONNECTABLE.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                default:
                    if (Constants.STATISTICS_ENABLED.getName().equals(reader.getAttributeLocalName(i))) {
                        final String value = rawAttributeText(reader, Constants.STATISTICS_ENABLED.getXmlName());
                        if (value != null) {
                            Constants.STATISTICS_ENABLED.parseAndSetParameter(value, operation, reader);
                        }
                        break;

                    } else {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
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

    private void parseXADataSource_1_2(XMLExtendedStreamReader reader, final List<ModelNode> list, final ModelNode parentAddress) throws XMLStreamException, ParserException,
            ValidateException {

        String poolName = null;
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        boolean enabled = Defaults.ENABLED.booleanValue();
        // Persist the enabled flag because xml default is != from DMR default
        boolean persistEnabled = true;

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            }
            final XaDataSource.Attribute attribute = XaDataSource.Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED: {
                    final String value = rawAttributeText(reader, ENABLED.getXmlName());
                    if (value != null) {
                        enabled = Boolean.parseBoolean(value);
                        //ENABLED.parseAndSetParameter(value, operation, reader);
                        persistEnabled = true;
                    }
                    break;
                }
                case JNDI_NAME: {
                    final String jndiName = rawAttributeText(reader, JNDI_NAME.getXmlName());
                    JNDI_NAME.parseAndSetParameter(jndiName, operation, reader);
                    break;
                }
                case POOL_NAME: {
                    poolName = rawAttributeText(reader, POOLNAME_NAME);
                    break;
                }
                case USE_JAVA_CONTEXT: {
                    final String value = rawAttributeText(reader, USE_JAVA_CONTEXT.getXmlName());
                    if (value != null) {
                        USE_JAVA_CONTEXT.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case SPY: {
                    final String value = rawAttributeText(reader, SPY.getXmlName());
                    if (value != null) {
                        SPY.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case USE_CCM: {
                    final String value = rawAttributeText(reader, USE_CCM.getXmlName());
                    if (value != null) {
                        USE_CCM.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                default:
                    if (Constants.STATISTICS_ENABLED.getName().equals(reader.getAttributeLocalName(i))) {
                        final String value = rawAttributeText(reader, Constants.STATISTICS_ENABLED.getXmlName());
                        if (value != null) {
                            Constants.STATISTICS_ENABLED.parseAndSetParameter(value, operation, reader);
                        }
                        break;

                    } else {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
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
                            XA_DATASOURCE_CLASS.parseAndSetParameter(value, operation, reader);
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
                    String moduleName = rawAttributeText(reader, DRIVER_MODULE_NAME.getXmlName());
                    String slot = null;
                    if (moduleName.contains(":")) {
                        slot = moduleName.substring(moduleName.indexOf(":") + 1);
                        moduleName = moduleName.substring(0, moduleName.indexOf(":"));
                    }
                    DRIVER_MODULE_NAME.parseAndSetParameter(moduleName, operation, reader);
                    if (slot != null) {
                        MODULE_SLOT.parseAndSetParameter(slot, operation, reader);
                    }
                    break;
                }
                default:
                    break;
            }
        }
        driverAddress.add(JDBC_DRIVER_NAME, driverName);
        driverAddress.protect();

        operation.get(OP_ADDR).set(driverAddress);

        boolean driverClassMatched = false;
        boolean xaDatasourceClassMatched = false;
        boolean datasourceClassMatched = false;
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
                            if (datasourceClassMatched) {
                                throw new ParserException(bundle.unexpectedElement(DRIVER_DATASOURCE_CLASS_NAME.getXmlName()));
                            }
                            String value = rawElementText(reader);
                            DRIVER_DATASOURCE_CLASS_NAME.parseAndSetParameter(value, operation, reader);
                            datasourceClassMatched = true;
                            break;
                        }
                        case XA_DATASOURCE_CLASS: {
                            if (xaDatasourceClassMatched) {
                                throw new ParserException(bundle.unexpectedElement(DRIVER_XA_DATASOURCE_CLASS_NAME.getXmlName()));
                            }
                            String value = rawElementText(reader);
                            DRIVER_XA_DATASOURCE_CLASS_NAME.parseAndSetParameter(value, operation, reader);
                            xaDatasourceClassMatched = true;
                            break;
                        }
                        case DRIVER_CLASS: {
                            if (driverClassMatched) {
                                throw new ParserException(bundle.unexpectedElement(DRIVER_CLASS_NAME.getXmlName()));
                            }
                            String value = rawElementText(reader);
                            DRIVER_CLASS_NAME.parseAndSetParameter(value, operation, reader);
                            driverClassMatched = true;
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

    private void parseXADataSource_1_0(XMLExtendedStreamReader reader, final List<ModelNode> list, final ModelNode parentAddress) throws XMLStreamException, ParserException,
            ValidateException {

        String poolName = null;
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            }
            final XaDataSource.Attribute attribute = XaDataSource.Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED: {
                    final String value = rawAttributeText(reader, ENABLED.getXmlName());
                    if (value != null) {
                        ENABLED.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case JNDI_NAME: {
                    final String jndiName = rawAttributeText(reader, JNDI_NAME.getXmlName());
                    JNDI_NAME.parseAndSetParameter(jndiName, operation, reader);
                    break;
                }
                case POOL_NAME: {
                    poolName = rawAttributeText(reader, POOLNAME_NAME);
                    break;
                }
                case USE_JAVA_CONTEXT: {
                    final String value = rawAttributeText(reader, USE_JAVA_CONTEXT.getXmlName());
                    if (value != null) {
                        USE_JAVA_CONTEXT.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case SPY: {
                    final String value = rawAttributeText(reader, SPY.getXmlName());
                    if (value != null) {
                        SPY.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case USE_CCM: {
                    final String value = rawAttributeText(reader, USE_CCM.getXmlName());
                    if (value != null) {
                        USE_CCM.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                default:
                    throw ParseUtils.unexpectedAttribute(reader, i);
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
                            XA_DATASOURCE_CLASS.parseAndSetParameter(value, operation, reader);
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
                        case URL_PROPERTY: {
                            String value = rawElementText(reader);
                            URL_PROPERTY.parseAndSetParameter(value, operation, reader);
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

    private void parseXADataSource_3_0(XMLExtendedStreamReader reader, final List<ModelNode> list, final ModelNode parentAddress) throws XMLStreamException, ParserException,
            ValidateException {

        String poolName = null;
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            }
            final XaDataSource.Attribute attribute = XaDataSource.Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED: {
                    final String value = rawAttributeText(reader, ENABLED.getXmlName());
                    if (value != null) {
                        ENABLED.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case JNDI_NAME: {
                    final String jndiName = rawAttributeText(reader, JNDI_NAME.getXmlName());
                    JNDI_NAME.parseAndSetParameter(jndiName, operation, reader);
                    break;
                }
                case POOL_NAME: {
                    poolName = rawAttributeText(reader, POOLNAME_NAME);
                    break;
                }
                case USE_JAVA_CONTEXT: {
                    final String value = rawAttributeText(reader, USE_JAVA_CONTEXT.getXmlName());
                    if (value != null) {
                        USE_JAVA_CONTEXT.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case SPY: {
                    final String value = rawAttributeText(reader, SPY.getXmlName());
                    if (value != null) {
                        SPY.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case USE_CCM: {
                    final String value = rawAttributeText(reader, USE_CCM.getXmlName());
                    if (value != null) {
                        USE_CCM.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case CONNECTABLE: {
                    final String value = rawAttributeText(reader, CONNECTABLE.getXmlName());
                    if (value != null) {
                        CONNECTABLE.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case TRACKING: {
                    final String value = rawAttributeText(reader, TRACKING.getXmlName());
                    if (value != null) {
                        TRACKING.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                default:
                    if (Constants.STATISTICS_ENABLED.getName().equals(reader.getAttributeLocalName(i))) {
                        final String value = rawAttributeText(reader, Constants.STATISTICS_ENABLED.getXmlName());
                        if (value != null) {
                            Constants.STATISTICS_ENABLED.parseAndSetParameter(value, operation, reader);
                        }
                        break;

                    } else {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
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
                            XA_DATASOURCE_CLASS.parseAndSetParameter(value, operation, reader);
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
                        case URL_PROPERTY: {
                            String value = rawElementText(reader);
                            URL_PROPERTY.parseAndSetParameter(value, operation, reader);
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

    private void parseXADataSource_4_0(XMLExtendedStreamReader reader, final List<ModelNode> list, final ModelNode parentAddress) throws XMLStreamException, ParserException,
            ValidateException {

        String poolName = null;
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            }
            final XaDataSource.Attribute attribute = XaDataSource.Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED: {
                    final String value = rawAttributeText(reader, ENABLED.getXmlName());
                    if (value != null) {
                        ENABLED.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case JNDI_NAME: {
                    final String jndiName = rawAttributeText(reader, JNDI_NAME.getXmlName());
                    JNDI_NAME.parseAndSetParameter(jndiName, operation, reader);
                    break;
                }
                case POOL_NAME: {
                    poolName = rawAttributeText(reader, POOLNAME_NAME);
                    break;
                }
                case USE_JAVA_CONTEXT: {
                    final String value = rawAttributeText(reader, USE_JAVA_CONTEXT.getXmlName());
                    if (value != null) {
                        USE_JAVA_CONTEXT.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case SPY: {
                    final String value = rawAttributeText(reader, SPY.getXmlName());
                    if (value != null) {
                        SPY.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case USE_CCM: {
                    final String value = rawAttributeText(reader, USE_CCM.getXmlName());
                    if (value != null) {
                        USE_CCM.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case CONNECTABLE: {
                    final String value = rawAttributeText(reader, CONNECTABLE.getXmlName());
                    if (value != null) {
                        CONNECTABLE.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case MCP: {
                    final String value = rawAttributeText(reader, MCP.getXmlName());
                    if (value != null) {
                        MCP.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case ENLISTMENT_TRACE: {
                    final String value = rawAttributeText(reader, ENLISTMENT_TRACE.getXmlName());
                    ENLISTMENT_TRACE.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case TRACKING: {
                    final String value = rawAttributeText(reader, TRACKING.getXmlName());
                    if (value != null) {
                        TRACKING.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                default:
                    if (Constants.STATISTICS_ENABLED.getName().equals(reader.getAttributeLocalName(i))) {
                        final String value = rawAttributeText(reader, Constants.STATISTICS_ENABLED.getXmlName());
                        if (value != null) {
                            Constants.STATISTICS_ENABLED.parseAndSetParameter(value, operation, reader);
                        }
                        break;

                    } else {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
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
                            XA_DATASOURCE_CLASS.parseAndSetParameter(value, operation, reader);
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
                        case URL_PROPERTY: {
                            String value = rawElementText(reader);
                            URL_PROPERTY.parseAndSetParameter(value, operation, reader);
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
                            switch (Namespace.forUri(reader.getNamespaceURI())) {
                                // This method is only called for version 4 and later.
                                case DATASOURCES_4_0:
                                    parseDsSecurity(reader, operation);
                                    break;
                                default:
                                    parseDsSecurity_5_0(reader, operation);
                                    break;
                            }
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

        boolean securityDomainMatched = false;
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
                    final String localName = reader.getLocalName();
                    DsSecurity.Tag tag = DsSecurity.Tag.forName(localName);
                    if (localName == null) break;
                    if (localName.equals(DsSecurity.Tag.PASSWORD.getLocalName())) {
                        String value = rawElementText(reader);
                        PASSWORD.parseAndSetParameter(value, operation, reader);
                        break;
                    } else if (localName.equals(DsSecurity.Tag.USER_NAME.getLocalName())) {
                        String value = rawElementText(reader);
                        USERNAME.parseAndSetParameter(value, operation, reader);
                        break;
                    } else if (localName.equals(DsSecurity.Tag.SECURITY_DOMAIN.getLocalName())) {
                        if (securityDomainMatched) {
                            throw new ParserException(bundle.unexpectedElement(SECURITY_DOMAIN.getXmlName()));
                        }
                        String value = rawElementText(reader);
                        SECURITY_DOMAIN.parseAndSetParameter(value, operation, reader);
                        securityDomainMatched = true;
                        break;
                    } else if (localName.equals(DsSecurity.Tag.REAUTH_PLUGIN.getLocalName())) {
                        parseExtension(reader, tag.getLocalName(), operation, REAUTH_PLUGIN_CLASSNAME, REAUTHPLUGIN_PROPERTIES);
                        break;
                    } else if (localName.equals(CREDENTIAL_REFERENCE.getXmlName())) {
                        CREDENTIAL_REFERENCE.getParser().parseElement(CREDENTIAL_REFERENCE, reader, operation);
                        break;
                    } else {
                        throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
                    }
                }
            }
        }
        throw new ParserException(bundle.unexpectedEndOfDocument());
    }

    private void parseDsSecurity_5_0(XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException, ParserException,
            ValidateException {

        boolean securityDomainMatched = false;
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
                            if (securityDomainMatched) {
                                throw new ParserException(bundle.unexpectedElement(SECURITY_DOMAIN.getXmlName()));
                            }
                            String value = rawElementText(reader);
                            SECURITY_DOMAIN.parseAndSetParameter(value, operation, reader);
                            securityDomainMatched = true;
                            break;
                        }
                        case ELYTRON_ENABLED: {
                            String value = rawElementText(reader);
                            value = value == null ? "true" : value;
                            ELYTRON_ENABLED.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case AUTHENTICATION_CONTEXT: {
                            String value = rawElementText(reader);
                            AUTHENTICATION_CONTEXT.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case REAUTH_PLUGIN: {
                            parseExtension(reader, tag.getLocalName(), operation, REAUTH_PLUGIN_CLASSNAME, REAUTHPLUGIN_PROPERTIES);
                            break;
                        }
                        case CREDENTIAL_REFERENCE: {
                            CREDENTIAL_REFERENCE.getParser().parseElement(CREDENTIAL_REFERENCE, reader, operation);
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

    private void parseDataSource_1_0(final XMLExtendedStreamReader reader, final List<ModelNode> list, final ModelNode parentAddress) throws XMLStreamException, ParserException,
            ValidateException {

        String poolName = null;
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            }
            final DataSource.Attribute attribute = DataSource.Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED: {
                    final String value = rawAttributeText(reader, ENABLED.getXmlName());
                    if (value != null) {
                        ENABLED.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case JNDI_NAME: {
                    final String jndiName = rawAttributeText(reader, JNDI_NAME.getXmlName());
                    JNDI_NAME.parseAndSetParameter(jndiName, operation, reader);
                    break;
                }
                case POOL_NAME: {
                    poolName = rawAttributeText(reader, POOLNAME_NAME);
                    break;
                }
                case USE_JAVA_CONTEXT: {
                    final String value = rawAttributeText(reader, USE_JAVA_CONTEXT.getXmlName());
                    if (value != null) {
                        USE_JAVA_CONTEXT.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case SPY: {
                    final String value = rawAttributeText(reader, SPY.getXmlName());
                    if (value != null) {
                        SPY.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case USE_CCM: {
                    final String value = rawAttributeText(reader, USE_CCM.getXmlName());
                    if (value != null) {
                        USE_CCM.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case JTA: {
                    final String value = rawAttributeText(reader, JTA.getXmlName());
                    if (value != null) {
                        JTA.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                default:
                    throw ParseUtils.unexpectedAttribute(reader, i);
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


    private void parseDataSource_3_0(final XMLExtendedStreamReader reader, final List<ModelNode> list, final ModelNode parentAddress) throws XMLStreamException, ParserException,
            ValidateException {

        String poolName = null;
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {

            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            }
            final DataSource.Attribute attribute = DataSource.Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED: {
                    final String value = rawAttributeText(reader, ENABLED.getXmlName());
                    if (value != null) {
                        ENABLED.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case JNDI_NAME: {
                    final String jndiName = rawAttributeText(reader, JNDI_NAME.getXmlName());
                    JNDI_NAME.parseAndSetParameter(jndiName, operation, reader);
                    break;
                }
                case POOL_NAME: {
                    poolName = rawAttributeText(reader, POOLNAME_NAME);
                    break;
                }
                case USE_JAVA_CONTEXT: {
                    final String value = rawAttributeText(reader, USE_JAVA_CONTEXT.getXmlName());
                    if (value != null) {
                        USE_JAVA_CONTEXT.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case SPY: {
                    final String value = rawAttributeText(reader, SPY.getXmlName());
                    if (value != null) {
                        SPY.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case USE_CCM: {
                    final String value = rawAttributeText(reader, USE_CCM.getXmlName());
                    if (value != null) {
                        USE_CCM.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case JTA: {
                    final String value = rawAttributeText(reader, JTA.getXmlName());
                    if (value != null) {
                        JTA.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case CONNECTABLE: {
                    final String value = rawAttributeText(reader, CONNECTABLE.getXmlName());
                    if (value != null) {
                        CONNECTABLE.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case TRACKING: {
                    final String value = rawAttributeText(reader, TRACKING.getXmlName());
                    if (value != null) {
                        TRACKING.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                default:
                    if (Constants.STATISTICS_ENABLED.getName().equals(reader.getAttributeLocalName(i))) {
                        final String value = rawAttributeText(reader, Constants.STATISTICS_ENABLED.getXmlName());
                        if (value != null) {
                            Constants.STATISTICS_ENABLED.parseAndSetParameter(value, operation, reader);
                        }
                        break;

                    } else {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
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

    private void parseDataSource_4_0(final XMLExtendedStreamReader reader, final List<ModelNode> list, final ModelNode parentAddress) throws XMLStreamException, ParserException,
            ValidateException {

        String poolName = null;
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {

            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            }
            final DataSource.Attribute attribute = DataSource.Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED: {
                    final String value = rawAttributeText(reader, ENABLED.getXmlName());
                    if (value != null) {
                        ENABLED.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case JNDI_NAME: {
                    final String jndiName = rawAttributeText(reader, JNDI_NAME.getXmlName());
                    JNDI_NAME.parseAndSetParameter(jndiName, operation, reader);
                    break;
                }
                case POOL_NAME: {
                    poolName = rawAttributeText(reader, POOLNAME_NAME);
                    break;
                }
                case USE_JAVA_CONTEXT: {
                    final String value = rawAttributeText(reader, USE_JAVA_CONTEXT.getXmlName());
                    if (value != null) {
                        USE_JAVA_CONTEXT.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case SPY: {
                    final String value = rawAttributeText(reader, SPY.getXmlName());
                    if (value != null) {
                        SPY.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case USE_CCM: {
                    final String value = rawAttributeText(reader, USE_CCM.getXmlName());
                    if (value != null) {
                        USE_CCM.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case JTA: {
                    final String value = rawAttributeText(reader, JTA.getXmlName());
                    if (value != null) {
                        JTA.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case CONNECTABLE: {
                    final String value = rawAttributeText(reader, CONNECTABLE.getXmlName());
                    if (value != null) {
                        CONNECTABLE.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case MCP: {
                    final String value = rawAttributeText(reader, MCP.getXmlName());
                    if (value != null) {
                        MCP.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                case ENLISTMENT_TRACE: {
                    final String value = rawAttributeText(reader, ENLISTMENT_TRACE.getXmlName());
                    ENLISTMENT_TRACE.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case TRACKING: {
                    final String value = rawAttributeText(reader, TRACKING.getXmlName());
                    if (value != null) {
                        TRACKING.parseAndSetParameter(value, operation, reader);
                    }
                    break;
                }
                default:
                    if (Constants.STATISTICS_ENABLED.getName().equals(reader.getAttributeLocalName(i))) {
                        final String value = rawAttributeText(reader, Constants.STATISTICS_ENABLED.getXmlName());
                        if (value != null) {
                            Constants.STATISTICS_ENABLED.parseAndSetParameter(value, operation, reader);
                        }
                        break;

                    } else {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
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
                            switch (Namespace.forUri(reader.getNamespaceURI())) {
                                // This method is only called for version 4 and later.
                                case DATASOURCES_4_0:
                                    parseDsSecurity(reader, operation);
                                    break;
                                default:
                                    parseDsSecurity_5_0(reader, operation);
                                    break;
                            }
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
                        if (DsPool.Tag.forName(reader.getLocalName()) == DsPool.Tag.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (DsPool.Tag.forName(reader.getLocalName())) {
                        case MAX_POOL_SIZE: {
                            String value = rawElementText(reader);
                            MAX_POOL_SIZE.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case INITIAL_POOL_SIZE: {
                            String value = rawElementText(reader);
                            INITIAL_POOL_SIZE.parseAndSetParameter(value, operation, reader);
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
                        case FAIR: {
                            String value = rawElementText(reader);
                            POOL_FAIR.parseAndSetParameter(value, operation, reader);
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
                        case ALLOW_MULTIPLE_USERS: {
                            String value = rawElementText(reader);
                            ALLOW_MULTIPLE_USERS.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case CAPACITY: {
                            parseCapacity(reader, operation);
                            break;
                        }
                        case CONNECTION_LISTENER: {
                            parseExtension(reader, reader.getLocalName(), operation, CONNECTION_LISTENER_CLASS, CONNECTION_LISTENER_PROPERTIES);
                            break;
                        }
                        case UNKNOWN: {
                            throw new ParserException(bundle.unexpectedElement(reader.getLocalName()));
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


    private void parseXaPool(XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException, ParserException,
            ValidateException {

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (XaDataSource.Tag.forName(reader.getLocalName()) == XaDataSource.Tag.XA_POOL) {
                        return;
                        //it's fine. Do nothing
                    } else {
                        if (DsXaPool.Tag.forName(reader.getLocalName()) == DsXaPool.Tag.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (DsXaPool.Tag.forName(reader.getLocalName())) {
                        case MAX_POOL_SIZE: {
                            String value = rawElementText(reader);
                            MAX_POOL_SIZE.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case INITIAL_POOL_SIZE: {
                            String value = rawElementText(reader);
                            INITIAL_POOL_SIZE.parseAndSetParameter(value, operation, reader);
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
                        case FAIR: {
                            String value = rawElementText(reader);
                            POOL_FAIR.parseAndSetParameter(value, operation, reader);
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
                        case ALLOW_MULTIPLE_USERS: {
                            String value = rawElementText(reader);
                            ALLOW_MULTIPLE_USERS.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case CONNECTION_LISTENER: {
                            parseExtension(reader, reader.getLocalName(), operation, CONNECTION_LISTENER_CLASS, CONNECTION_LISTENER_PROPERTIES);
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
                            NO_TX_SEPARATE_POOL.parseAndSetParameter(value, operation, reader);
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
                        case CAPACITY: {
                            parseCapacity(reader, operation);
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

    private void parseCapacity(XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException, ParserException,
                ValidateException {

            while (reader.hasNext()) {
                switch (reader.nextTag()) {
                    case END_ELEMENT: {
                        if (DsPool.Tag.forName(reader.getLocalName()) == DsPool.Tag.CAPACITY ) {

                            return;
                        } else {
                            if (Capacity.Tag.forName(reader.getLocalName()) == Capacity.Tag.UNKNOWN) {
                                throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                            }
                        }
                        break;
                    }
                    case START_ELEMENT: {
                        switch (Capacity.Tag.forName(reader.getLocalName())) {
                            case INCREMENTER: {
                                parseExtension(reader, reader.getLocalName(), operation, CAPACITY_INCREMENTER_CLASS , CAPACITY_INCREMENTER_PROPERTIES);
                                break;
                            }
                            case DECREMENTER: {
                                parseExtension(reader, reader.getLocalName(), operation, CAPACITY_DECREMENTER_CLASS , CAPACITY_DECREMENTER_PROPERTIES);
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
                            switch (Namespace.forUri(reader.getNamespaceURI())) {
                                case DATASOURCES_1_1:
                                case DATASOURCES_1_2:
                                case DATASOURCES_2_0:
                                case DATASOURCES_3_0:
                                case DATASOURCES_4_0:
                                    parseCredential(reader, operation);
                                    break;
                                default:
                                    parseCredential_5_0(reader, operation);
                                    break;
                            }
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

    private void parseCredential_5_0(XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException, ParserException,
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
                        case ELYTRON_ENABLED: {
                            String value = rawElementText(reader);
                            value = value == null ? "true" : value;
                            RECOVERY_ELYTRON_ENABLED.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case AUTHENTICATION_CONTEXT: {
                            String value = rawElementText(reader);
                            RECOVERY_AUTHENTICATION_CONTEXT.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case CREDENTIAL_REFERENCE:
                            RECOVERY_CREDENTIAL_REFERENCE.getParser().parseElement(RECOVERY_CREDENTIAL_REFERENCE, reader, operation);
                            break;
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
                            CHECK_VALID_CONNECTION_SQL.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case EXCEPTION_SORTER: {
                            parseExtension(reader, currTag.getLocalName(), operation, EXCEPTION_SORTER_CLASSNAME, EXCEPTION_SORTER_PROPERTIES);
                            break;
                        }
                        case STALE_CONNECTION_CHECKER: {
                            parseExtension(reader, currTag.getLocalName(), operation, STALE_CONNECTION_CHECKER_CLASSNAME, STALE_CONNECTION_CHECKER_PROPERTIES);
                            break;
                        }
                        case USE_FAST_FAIL: {
                            String value = rawElementText(reader);
                            USE_FAST_FAIL.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case VALIDATE_ON_MATCH: {
                            String value = rawElementText(reader);
                            VALIDATE_ON_MATCH.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case VALID_CONNECTION_CHECKER: {
                            parseExtension(reader, currTag.getLocalName(), operation, VALID_CONNECTION_CHECKER_CLASSNAME, VALID_CONNECTION_CHECKER_PROPERTIES);
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
                            QUERY_TIMEOUT.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case SET_TX_QUERY_TIMEOUT: {
                            //tag presence is sufficient to set it to true
                            String value = rawElementText(reader);
                            value = value == null ? "true" : value;
                            SET_TX_QUERY_TIMEOUT.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case USE_TRY_LOCK: {
                            String value = rawElementText(reader);
                            USE_TRY_LOCK.parseAndSetParameter(value, operation, reader);
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
                            PREPARED_STATEMENTS_CACHE_SIZE.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case TRACK_STATEMENTS: {
                            String value = rawElementText(reader);
                            TRACK_STATEMENTS.parseAndSetParameter(value, operation, reader);
                            break;
                        }
                        case SHARE_PREPARED_STATEMENTS: {
                            //tag presence is sufficient to set it to true
                            String value = rawElementText(reader);
                            value = value == null ? "true" : value;
                            SHARE_PREPARED_STATEMENTS.parseAndSetParameter(value, operation, reader);
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
