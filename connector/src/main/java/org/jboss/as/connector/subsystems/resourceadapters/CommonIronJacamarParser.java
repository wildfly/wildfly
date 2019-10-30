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
package org.jboss.as.connector.subsystems.resourceadapters;

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
import static org.jboss.as.connector.subsystems.common.pool.Constants.VALIDATE_ON_MATCH;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.AUTHENTICATION_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.AUTHENTICATION_CONTEXT_AND_APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CLASS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONFIG_PROPERTY_VALUE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONNECTABLE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ELYTRON_ENABLED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ENLISTMENT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ENLISTMENT_TRACE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.INTERLEAVING;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.JNDINAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.MCP;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.NOTXSEPARATEPOOL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.NO_RECOVERY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.PAD_XID;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.POOL_NAME_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERLUGIN_CLASSNAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERLUGIN_PROPERTIES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_AUTHENTICATION_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_CREDENTIAL_REFERENCE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_ELYTRON_ENABLED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_PASSWORD;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_USERNAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SAME_RM_OVERRIDE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SECURITY_DOMAIN_AND_APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SHARABLE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.TRACKING;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_CCM;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WRAP_XA_RESOURCE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.XA_RESOURCE_TIMEOUT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.connector.metadata.api.common.Credential;
import org.jboss.as.connector.metadata.api.common.Security;
import org.jboss.as.connector.util.AbstractParser;
import org.jboss.as.connector.util.ParserException;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.CommonBundle;
import org.jboss.jca.common.api.metadata.common.Capacity;
import org.jboss.jca.common.api.metadata.common.Pool;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.common.TimeOut;
import org.jboss.jca.common.api.metadata.common.Validation;
import org.jboss.jca.common.api.metadata.common.XaPool;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.metadata.ds.DsPool;
import org.jboss.jca.common.api.metadata.ds.XaDataSource;
import org.jboss.jca.common.api.metadata.resourceadapter.Activation;
import org.jboss.jca.common.api.metadata.resourceadapter.AdminObject;
import org.jboss.jca.common.api.metadata.resourceadapter.ConnectionDefinition;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.logging.Messages;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * A CommonIronJacamarParser.
 *
 * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
 */
public abstract class CommonIronJacamarParser extends AbstractParser {
    /**
     * The bundle
     */
    private static CommonBundle bundle = Messages.getBundle(CommonBundle.class);


    protected void parseConfigProperties(final XMLExtendedStreamReader reader, final Map<String, ModelNode> map) throws XMLStreamException, ParserException {
        String name = rawAttributeText(reader, "name");
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        String value = rawElementText(reader);
        CONFIG_PROPERTY_VALUE.parseAndSetParameter(value, operation, reader);

        if (map.containsKey(name)) {
            throw ParseUtils.unexpectedElement(reader);
        }
        map.put(name, operation);
    }

    /**
     * parse a single connection-definition tag
     *
     * @param reader the reader
     * @throws javax.xml.stream.XMLStreamException
     *                         XMLStreamException
     * @throws ParserException ParserException
     * @throws org.jboss.jca.common.api.validator.ValidateException
     *                         ValidateException
     */
    protected void parseConnectionDefinitions_3_0(final XMLExtendedStreamReader reader, final Map<String, ModelNode> map,
                                              final Map<String, HashMap<String, ModelNode>> configMap, final boolean isXa)
            throws XMLStreamException, ParserException, ValidateException {


        final ModelNode connectionDefinitionNode = new ModelNode();
        connectionDefinitionNode.get(OP).set(ADD);

        String poolName = null;
        String jndiName = null;
        int attributeSize = reader.getAttributeCount();
        boolean poolDefined = Boolean.FALSE;

        for (int i = 0; i < attributeSize; i++) {
            ConnectionDefinition.Attribute attribute = ConnectionDefinition.Attribute.forName(reader.getAttributeLocalName(i));
            String value = reader.getAttributeValue(i);
            switch (attribute) {
                case ENABLED: {
                    ENABLED.parseAndSetParameter(value, connectionDefinitionNode, reader);

                    break;
                }
                case CONNECTABLE: {
                    CONNECTABLE.parseAndSetParameter(value, connectionDefinitionNode, reader);

                    break;
                }
                case TRACKING: {
                    TRACKING.parseAndSetParameter(value, connectionDefinitionNode, reader);

                    break;
                }
                case JNDI_NAME: {
                    jndiName = value;
                    JNDINAME.parseAndSetParameter(jndiName, connectionDefinitionNode, reader);
                    break;
                }
                case POOL_NAME: {
                    poolName = value;
                    break;
                }
                case USE_JAVA_CONTEXT: {
                    USE_JAVA_CONTEXT.parseAndSetParameter(value, connectionDefinitionNode, reader);

                    break;
                }

                case USE_CCM: {
                    USE_CCM.parseAndSetParameter(value, connectionDefinitionNode, reader);
                    break;
                }

                case SHARABLE: {
                    SHARABLE.parseAndSetParameter(value, connectionDefinitionNode, reader);
                    break;
                }

                case ENLISTMENT: {
                    ENLISTMENT.parseAndSetParameter(value, connectionDefinitionNode, reader);
                    break;
                }

                case CLASS_NAME: {
                    CLASS_NAME.parseAndSetParameter(value, connectionDefinitionNode, reader);
                    break;
                }
                default:
                    throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }
        if (poolName == null || poolName.trim().equals("")) {
            if (jndiName != null && jndiName.trim().length() != 0) {
                if (jndiName.contains("/")) {
                    poolName = jndiName.substring(jndiName.lastIndexOf("/") + 1);
                } else {
                    poolName = jndiName.substring(jndiName.lastIndexOf(":") + 1);
                }
            } else {
                throw ParseUtils.missingRequired(reader, EnumSet.of(ConnectionDefinition.Attribute.JNDI_NAME));
            }
        }


        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (Activation.Tag.forName(reader.getLocalName()) == Activation.Tag.CONNECTION_DEFINITION) {

                        map.put(poolName, connectionDefinitionNode);
                        return;
                    } else {
                        if (ConnectionDefinition.Tag.forName(reader.getLocalName()) == ConnectionDefinition.Tag.UNKNOWN) {
                            throw ParseUtils.unexpectedEndElement(reader);
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (ConnectionDefinition.Tag.forName(reader.getLocalName())) {
                        case CONFIG_PROPERTY: {
                            if (!configMap.containsKey(poolName)) {
                                configMap.put(poolName, new HashMap<String, ModelNode>(0));
                            }
                            parseConfigProperties(reader, configMap.get(poolName));
                            break;
                        }
                        case SECURITY: {
                            parseSecuritySettings(reader, connectionDefinitionNode);
                            break;
                        }
                        case TIMEOUT: {
                            parseTimeOut(reader, isXa, connectionDefinitionNode);
                            break;
                        }
                        case VALIDATION: {
                            parseValidation(reader, connectionDefinitionNode);
                            break;
                        }
                        case XA_POOL: {
                            if (!isXa) {
                                throw ParseUtils.unexpectedElement(reader);
                            }
                            if (poolDefined) {
                                throw new ParserException(bundle.multiplePools());
                            }
                            parseXaPool(reader, connectionDefinitionNode);
                            poolDefined = true;
                            break;
                        }
                        case POOL: {
                            if (isXa) {
                                throw ParseUtils.unexpectedElement(reader);
                            }
                            if (poolDefined) {
                                throw new ParserException(bundle.multiplePools());
                            }
                            parsePool(reader, connectionDefinitionNode);
                            poolDefined = true;
                            break;
                        }
                        case RECOVERY: {
                            parseRecovery(reader, connectionDefinitionNode);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
            }
        }
        throw ParseUtils.unexpectedEndElement(reader);

    }

    /**
     * parse a single connection-definition tag
     *
     * @param reader the reader
     * @throws javax.xml.stream.XMLStreamException
     *                         XMLStreamException
     * @throws ParserException ParserException
     * @throws org.jboss.jca.common.api.validator.ValidateException
     *                         ValidateException
     */
    protected void parseConnectionDefinitions_4_0(final XMLExtendedStreamReader reader, final Map<String, ModelNode> map,
                                                  final Map<String, HashMap<String, ModelNode>> configMap, final boolean isXa)
            throws XMLStreamException, ParserException, ValidateException {


        final ModelNode connectionDefinitionNode = new ModelNode();
        connectionDefinitionNode.get(OP).set(ADD);

        String poolName = null;
        String jndiName = null;
        int attributeSize = reader.getAttributeCount();
        boolean poolDefined = Boolean.FALSE;

        for (int i = 0; i < attributeSize; i++) {
            ConnectionDefinition.Attribute attribute = ConnectionDefinition.Attribute.forName(reader.getAttributeLocalName(i));
            String value = reader.getAttributeValue(i);
            switch (attribute) {
                case ENABLED: {
                    ENABLED.parseAndSetParameter(value, connectionDefinitionNode, reader);

                    break;
                }
                case CONNECTABLE: {
                    CONNECTABLE.parseAndSetParameter(value, connectionDefinitionNode, reader);

                    break;
                }
                case TRACKING: {
                    TRACKING.parseAndSetParameter(value, connectionDefinitionNode, reader);

                    break;
                }
                case JNDI_NAME: {
                    jndiName = value;
                    JNDINAME.parseAndSetParameter(jndiName, connectionDefinitionNode, reader);
                    break;
                }
                case POOL_NAME: {
                    poolName = value;
                    break;
                }
                case USE_JAVA_CONTEXT: {
                    USE_JAVA_CONTEXT.parseAndSetParameter(value, connectionDefinitionNode, reader);

                    break;
                }

                case USE_CCM: {
                    USE_CCM.parseAndSetParameter(value, connectionDefinitionNode, reader);
                    break;
                }

                case SHARABLE: {
                    SHARABLE.parseAndSetParameter(value, connectionDefinitionNode, reader);
                    break;
                }

                case ENLISTMENT: {
                    ENLISTMENT.parseAndSetParameter(value, connectionDefinitionNode, reader);
                    break;
                }

                case CLASS_NAME: {
                    CLASS_NAME.parseAndSetParameter(value, connectionDefinitionNode, reader);
                    break;
                }
                case MCP: {
                    MCP.parseAndSetParameter(value, connectionDefinitionNode, reader);
                    break;
                }
                case ENLISTMENT_TRACE:
                    ENLISTMENT_TRACE.parseAndSetParameter(value, connectionDefinitionNode, reader);
                    break;
                default:
                    throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }
        if (poolName == null || poolName.trim().equals("")) {
            if (jndiName != null && jndiName.trim().length() != 0) {
                if (jndiName.contains("/")) {
                    poolName = jndiName.substring(jndiName.lastIndexOf("/") + 1);
                } else {
                    poolName = jndiName.substring(jndiName.lastIndexOf(":") + 1);
                }
            } else {
                throw ParseUtils.missingRequired(reader, EnumSet.of(ConnectionDefinition.Attribute.JNDI_NAME));
            }
        }


        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (Activation.Tag.forName(reader.getLocalName()) == Activation.Tag.CONNECTION_DEFINITION) {

                        map.put(poolName, connectionDefinitionNode);
                        return;
                    } else {
                        if (ConnectionDefinition.Tag.forName(reader.getLocalName()) == ConnectionDefinition.Tag.UNKNOWN) {
                            throw ParseUtils.unexpectedEndElement(reader);
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (ConnectionDefinition.Tag.forName(reader.getLocalName())) {
                        case CONFIG_PROPERTY: {
                            if (!configMap.containsKey(poolName)) {
                                configMap.put(poolName, new HashMap<String, ModelNode>(0));
                            }
                            parseConfigProperties(reader, configMap.get(poolName));
                            break;
                        }
                        case SECURITY: {
                            parseSecuritySettings(reader, connectionDefinitionNode);
                            break;
                        }
                        case TIMEOUT: {
                            parseTimeOut(reader, isXa, connectionDefinitionNode);
                            break;
                        }
                        case VALIDATION: {
                            parseValidation(reader, connectionDefinitionNode);
                            break;
                        }
                        case XA_POOL: {
                            if (!isXa) {
                                throw ParseUtils.unexpectedElement(reader);
                            }
                            if (poolDefined) {
                                throw new ParserException(bundle.multiplePools());
                            }
                            parseXaPool(reader, connectionDefinitionNode);
                            poolDefined = true;
                            break;
                        }
                        case POOL: {
                            if (isXa) {
                                throw ParseUtils.unexpectedElement(reader);
                            }
                            if (poolDefined) {
                                throw new ParserException(bundle.multiplePools());
                            }
                            parsePool(reader, connectionDefinitionNode);
                            poolDefined = true;
                            break;
                        }
                        case RECOVERY: {
                            parseRecovery(reader, connectionDefinitionNode);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
            }
        }
        throw ParseUtils.unexpectedEndElement(reader);

    }

    /**
     * Parses connection attributes for version 5.0
     * @param reader the xml reader
     * @param connectionDefinitionNode the connection definition add node
     * @return the pool name
     * @throws XMLStreamException
     */
    private String parseConnectionAttributes_5_0(final XMLExtendedStreamReader reader,  final ModelNode connectionDefinitionNode)
            throws XMLStreamException {
        String poolName = null;
        String jndiName = null;
        int attributeSize = reader.getAttributeCount();

        for (int i = 0; i < attributeSize; i++) {
            ConnectionDefinition.Attribute attribute = ConnectionDefinition.Attribute.forName(reader.getAttributeLocalName(i));
            String value = reader.getAttributeValue(i);
            switch (attribute) {
                case ENABLED: {
                    ENABLED.parseAndSetParameter(value, connectionDefinitionNode, reader);

                    break;
                }
                case CONNECTABLE: {
                    CONNECTABLE.parseAndSetParameter(value, connectionDefinitionNode, reader);

                    break;
                }
                case TRACKING: {
                    TRACKING.parseAndSetParameter(value, connectionDefinitionNode, reader);

                    break;
                }
                case JNDI_NAME: {
                    jndiName = value;
                    JNDINAME.parseAndSetParameter(jndiName, connectionDefinitionNode, reader);
                    break;
                }
                case POOL_NAME: {
                    poolName = value;
                    break;
                }
                case USE_JAVA_CONTEXT: {
                    USE_JAVA_CONTEXT.parseAndSetParameter(value, connectionDefinitionNode, reader);

                    break;
                }

                case USE_CCM: {
                    USE_CCM.parseAndSetParameter(value, connectionDefinitionNode, reader);
                    break;
                }

                case SHARABLE: {
                    SHARABLE.parseAndSetParameter(value, connectionDefinitionNode, reader);
                    break;
                }

                case ENLISTMENT: {
                    ENLISTMENT.parseAndSetParameter(value, connectionDefinitionNode, reader);
                    break;
                }

                case CLASS_NAME: {
                    CLASS_NAME.parseAndSetParameter(value, connectionDefinitionNode, reader);
                    break;
                }
                case MCP: {
                    MCP.parseAndSetParameter(value, connectionDefinitionNode, reader);
                    break;
                }
                case ENLISTMENT_TRACE:
                    ENLISTMENT_TRACE.parseAndSetParameter(value, connectionDefinitionNode, reader);
                    break;
                default:
                    throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }
        if (poolName == null || poolName.trim().equals("")) {
            if (jndiName != null && jndiName.trim().length() != 0) {
                if (jndiName.contains("/")) {
                    poolName = jndiName.substring(jndiName.lastIndexOf("/") + 1);
                } else {
                    poolName = jndiName.substring(jndiName.lastIndexOf(":") + 1);
                }
            } else {
                throw ParseUtils.missingRequired(reader, EnumSet.of(ConnectionDefinition.Attribute.JNDI_NAME));
            }
        }
        return poolName;
    }

    /**
     * parse a single connection-definition tag
     *
     * @param reader the reader
     * @throws javax.xml.stream.XMLStreamException
     *                         XMLStreamException
     * @throws ParserException ParserException
     * @throws org.jboss.jca.common.api.validator.ValidateException
     *                         ValidateException
     */
    protected void parseConnectionDefinitions_5_0(final XMLExtendedStreamReader reader, final Map<String, ModelNode> map,
                                                  final Map<String, HashMap<String, ModelNode>> configMap, final boolean isXa)
            throws XMLStreamException, ParserException, ValidateException {


        final ModelNode connectionDefinitionNode = new ModelNode();
        connectionDefinitionNode.get(OP).set(ADD);

        final String poolName = parseConnectionAttributes_5_0(reader, connectionDefinitionNode);
        boolean poolDefined = Boolean.FALSE;

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (Activation.Tag.forName(reader.getLocalName()) == Activation.Tag.CONNECTION_DEFINITION) {

                        map.put(poolName, connectionDefinitionNode);
                        return;
                    } else {
                        if (ConnectionDefinition.Tag.forName(reader.getLocalName()) == ConnectionDefinition.Tag.UNKNOWN) {
                            throw ParseUtils.unexpectedEndElement(reader);
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (ConnectionDefinition.Tag.forName(reader.getLocalName())) {
                        case CONFIG_PROPERTY: {
                            if (!configMap.containsKey(poolName)) {
                                configMap.put(poolName, new HashMap<String, ModelNode>(0));
                            }
                            parseConfigProperties(reader, configMap.get(poolName));
                            break;
                        }
                        case SECURITY: {
                            parseElytronSupportedSecuritySettings(reader, connectionDefinitionNode);
                            break;
                        }
                        case TIMEOUT: {
                            parseTimeOut(reader, isXa, connectionDefinitionNode);
                            break;
                        }
                        case VALIDATION: {
                            parseValidation(reader, connectionDefinitionNode);
                            break;
                        }
                        case XA_POOL: {
                            if (!isXa) {
                                throw ParseUtils.unexpectedElement(reader);
                            }
                            if (poolDefined) {
                                throw new ParserException(bundle.multiplePools());
                            }
                            parseXaPool(reader, connectionDefinitionNode);
                            poolDefined = true;
                            break;
                        }
                        case POOL: {
                            if (isXa) {
                                throw ParseUtils.unexpectedElement(reader);
                            }
                            if (poolDefined) {
                                throw new ParserException(bundle.multiplePools());
                            }
                            parsePool(reader, connectionDefinitionNode);
                            poolDefined = true;
                            break;
                        }
                        case RECOVERY: {
                            parseElytronSupportedRecovery(reader, connectionDefinitionNode);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
            }
        }
        throw ParseUtils.unexpectedEndElement(reader);

    }

    /**
         * parse a single connection-definition tag
         *
         * @param reader the reader
         * @throws javax.xml.stream.XMLStreamException
         *                         XMLStreamException
         * @throws ParserException ParserException
         * @throws org.jboss.jca.common.api.validator.ValidateException
         *                         ValidateException
         */
        protected void parseConnectionDefinitions_1_0(final XMLExtendedStreamReader reader, final Map<String, ModelNode> map,
                                                  final Map<String, HashMap<String, ModelNode>> configMap, final boolean isXa)
                throws XMLStreamException, ParserException, ValidateException {


            final ModelNode connectionDefinitionNode = new ModelNode();
            connectionDefinitionNode.get(OP).set(ADD);

            String poolName = null;
            String jndiName = null;
            int attributeSize = reader.getAttributeCount();
            boolean poolDefined = Boolean.FALSE;

            for (int i = 0; i < attributeSize; i++) {
                ConnectionDefinition.Attribute attribute = ConnectionDefinition.Attribute.forName(reader.getAttributeLocalName(i));
                String value = reader.getAttributeValue(i);
                switch (attribute) {
                    case ENABLED: {
                        ENABLED.parseAndSetParameter(value, connectionDefinitionNode, reader);

                        break;
                    }
                    case JNDI_NAME: {
                        jndiName = value;
                        JNDINAME.parseAndSetParameter(jndiName, connectionDefinitionNode, reader);
                        break;
                    }
                    case POOL_NAME: {
                        poolName = value;
                        break;
                    }
                    case USE_JAVA_CONTEXT: {
                        USE_JAVA_CONTEXT.parseAndSetParameter(value, connectionDefinitionNode, reader);

                        break;
                    }

                    case USE_CCM: {
                        USE_CCM.parseAndSetParameter(value, connectionDefinitionNode, reader);
                        break;
                    }

                    case SHARABLE: {
                        SHARABLE.parseAndSetParameter(value, connectionDefinitionNode, reader);
                        break;
                    }

                    case ENLISTMENT: {
                        ENLISTMENT.parseAndSetParameter(value, connectionDefinitionNode, reader);
                        break;
                    }

                    case CLASS_NAME: {
                        CLASS_NAME.parseAndSetParameter(value, connectionDefinitionNode, reader);
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader,i);
                }
            }
            if (poolName == null || poolName.trim().equals("")) {
                if (jndiName != null && jndiName.trim().length() != 0) {
                    if (jndiName.contains("/")) {
                        poolName = jndiName.substring(jndiName.lastIndexOf("/") + 1);
                    } else {
                        poolName = jndiName.substring(jndiName.lastIndexOf(":") + 1);
                    }
                } else {
                    throw ParseUtils.missingRequired(reader, EnumSet.of(ConnectionDefinition.Attribute.JNDI_NAME));
                }
            }


            while (reader.hasNext()) {
                switch (reader.nextTag()) {
                    case END_ELEMENT: {
                        if (Activation.Tag.forName(reader.getLocalName()) == Activation.Tag.CONNECTION_DEFINITION) {

                            map.put(poolName, connectionDefinitionNode);
                            return;
                        } else {
                            if (ConnectionDefinition.Tag.forName(reader.getLocalName()) == ConnectionDefinition.Tag.UNKNOWN) {
                                throw ParseUtils.unexpectedEndElement(reader);
                            }
                        }
                        break;
                    }
                    case START_ELEMENT: {
                        switch (ConnectionDefinition.Tag.forName(reader.getLocalName())) {
                            case CONFIG_PROPERTY: {
                                if (!configMap.containsKey(poolName)) {
                                    configMap.put(poolName, new HashMap<String, ModelNode>(0));
                                }
                                parseConfigProperties(reader, configMap.get(poolName));
                                break;
                            }
                            case SECURITY: {
                                parseSecuritySettings(reader, connectionDefinitionNode);
                                break;
                            }
                            case TIMEOUT: {
                                parseTimeOut(reader, isXa, connectionDefinitionNode);
                                break;
                            }
                            case VALIDATION: {
                                parseValidation(reader, connectionDefinitionNode);
                                break;
                            }
                            case XA_POOL: {
                                if (!isXa) {
                                    throw ParseUtils.unexpectedElement(reader);
                                }
                                if (poolDefined) {
                                    throw new ParserException(bundle.multiplePools());
                                }
                                parseXaPool(reader, connectionDefinitionNode);
                                poolDefined = true;
                                break;
                            }
                            case POOL: {
                                if (isXa) {
                                    throw ParseUtils.unexpectedElement(reader);
                                }
                                if (poolDefined) {
                                    throw new ParserException(bundle.multiplePools());
                                }
                                parsePool(reader, connectionDefinitionNode);
                                poolDefined = true;
                                break;
                            }
                            case RECOVERY: {
                                parseRecovery(reader, connectionDefinitionNode);
                                break;
                            }
                            default:
                                throw ParseUtils.unexpectedElement(reader);
                        }
                        break;
                    }
                }
            }
            throw ParseUtils.unexpectedEndElement(reader);

        }



    private void parseValidation(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException, ParserException {

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (ConnectionDefinition.Tag.forName(reader.getLocalName()) == ConnectionDefinition.Tag.VALIDATION) {

                        return;
                    } else {
                        if (ConnectionDefinition.Tag.forName(reader.getLocalName()) == ConnectionDefinition.Tag.UNKNOWN) {
                            throw ParseUtils.unexpectedEndElement(reader);
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (Validation.Tag.forName(reader.getLocalName())) {
                        case BACKGROUND_VALIDATION: {
                            String value = rawElementText(reader);
                            BACKGROUNDVALIDATION.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case BACKGROUND_VALIDATION_MILLIS: {
                            String value = rawElementText(reader);
                            BACKGROUNDVALIDATIONMILLIS.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case USE_FAST_FAIL: {
                            String value = rawElementText(reader);
                            USE_FAST_FAIL.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case VALIDATE_ON_MATCH: {
                            String value = rawElementText(reader);
                            VALIDATE_ON_MATCH.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);

                    }
                    break;
                }
            }
        }
        throw ParseUtils.unexpectedEndElement(reader);
    }

    private void parseTimeOut(XMLExtendedStreamReader reader, Boolean isXa, ModelNode node) throws XMLStreamException,
            ParserException, ValidateException {

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (ConnectionDefinition.Tag.forName(reader.getLocalName()) == ConnectionDefinition.Tag.TIMEOUT) {

                        return;
                    } else {
                        if (TimeOut.Tag.forName(reader.getLocalName()) == TimeOut.Tag.UNKNOWN) {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    String value = rawElementText(reader);
                    switch (TimeOut.Tag.forName(reader.getLocalName())) {
                        case ALLOCATION_RETRY: {
                            ALLOCATION_RETRY.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case ALLOCATION_RETRY_WAIT_MILLIS: {
                            ALLOCATION_RETRY_WAIT_MILLIS.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case BLOCKING_TIMEOUT_MILLIS: {
                            BLOCKING_TIMEOUT_WAIT_MILLIS.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case IDLE_TIMEOUT_MINUTES: {
                            IDLETIMEOUTMINUTES.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case XA_RESOURCE_TIMEOUT: {
                            XA_RESOURCE_TIMEOUT.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
            }
        }
        throw ParseUtils.unexpectedEndElement(reader);
    }


    protected void parseAdminObjects(final XMLExtendedStreamReader reader, final Map<String, ModelNode> map, final Map<String, HashMap<String, ModelNode>> configMap)
            throws XMLStreamException, ParserException, ValidateException {


        final ModelNode adminObjectNode = new ModelNode();
        adminObjectNode.get(OP).set(ADD);
        int attributeSize = reader.getAttributeCount();


        String poolName = null;
        String jndiName = null;
        for (int i = 0; i < attributeSize; i++) {
            AdminObject.Attribute attribute = AdminObject.Attribute.forName(reader
                    .getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED: {
                    String value = rawAttributeText(reader, ENABLED.getXmlName());
                    if (value != null) {
                        ENABLED.parseAndSetParameter(value, adminObjectNode, reader);
                    }
                    break;
                }
                case JNDI_NAME: {
                    jndiName = rawAttributeText(reader, JNDINAME.getXmlName());
                    if (jndiName != null) {
                        JNDINAME.parseAndSetParameter(jndiName, adminObjectNode, reader);
                    }
                    break;
                }
                case POOL_NAME: {
                    poolName = rawAttributeText(reader, POOL_NAME_NAME);
                    break;
                }
                case USE_JAVA_CONTEXT: {
                    String value = rawAttributeText(reader, USE_JAVA_CONTEXT.getXmlName());
                    if (value != null) {
                        USE_JAVA_CONTEXT.parseAndSetParameter(value, adminObjectNode, reader);
                    }
                    break;
                }
                case CLASS_NAME: {
                    String value = rawAttributeText(reader, CLASS_NAME.getXmlName());
                    if (value != null) {
                        CLASS_NAME.parseAndSetParameter(value, adminObjectNode, reader);
                    }
                    break;
                }
                default:
                    throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }
        if (poolName == null || poolName.trim().equals("")) {
            if (jndiName != null && jndiName.trim().length() != 0) {
                if (jndiName.contains("/")) {
                    poolName = jndiName.substring(jndiName.lastIndexOf("/") + 1);
                } else {
                    poolName = jndiName.substring(jndiName.lastIndexOf(":") + 1);
                }
            } else {
                throw ParseUtils.missingRequired(reader, EnumSet.of(AdminObject.Attribute.JNDI_NAME));

            }
        }
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (Activation.Tag.forName(reader.getLocalName()) == Activation.Tag.ADMIN_OBJECT) {

                        map.put(poolName, adminObjectNode);
                        return;
                    } else {
                        if (AdminObject.Tag.forName(reader.getLocalName()) == AdminObject.Tag.UNKNOWN) {
                            throw ParseUtils.unexpectedEndElement(reader);
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (AdminObject.Tag.forName(reader.getLocalName())) {
                        case CONFIG_PROPERTY: {
                            if (!configMap.containsKey(poolName)) {
                                configMap.put(poolName, new HashMap<String, ModelNode>(0));
                            }
                            parseConfigProperties(reader, configMap.get(poolName));
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
            }
        }
        throw ParseUtils.unexpectedEndElement(reader);
    }

    /**
     * parse a {@link XaPool} object
     *
     * @param reader reader
     * @throws XMLStreamException XMLStreamException
     * @throws ParserException
     * @throws ValidateException  ValidateException
     */
    protected void parseXaPool(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException, ParserException, ValidateException {


        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (XaDataSource.Tag.forName(reader.getLocalName()) == XaDataSource.Tag.XA_POOL) {

                        return;

                    } else {
                        if (XaPool.Tag.forName(reader.getLocalName()) == XaPool.Tag.UNKNOWN) {
                            throw ParseUtils.unexpectedEndElement(reader);
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (XaPool.Tag.forName(reader.getLocalName())) {
                        case MAX_POOL_SIZE: {
                            String value = rawElementText(reader);
                            MAX_POOL_SIZE.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case MIN_POOL_SIZE: {
                            String value = rawElementText(reader);
                            MIN_POOL_SIZE.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case INITIAL_POOL_SIZE: {
                            String value = rawElementText(reader);
                            INITIAL_POOL_SIZE.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case PREFILL: {
                            String value = rawElementText(reader);
                            POOL_PREFILL.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case FAIR: {
                            String value = rawElementText(reader);
                            POOL_FAIR.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case USE_STRICT_MIN: {
                            String value = rawElementText(reader);
                            POOL_USE_STRICT_MIN.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case FLUSH_STRATEGY: {
                            String value = rawElementText(reader);
                            POOL_FLUSH_STRATEGY.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case INTERLEAVING: {
                            String value = rawElementText(reader);
                            //just presence means true
                            value = value == null ? "true" : value;
                            INTERLEAVING.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case IS_SAME_RM_OVERRIDE: {
                            String value = rawElementText(reader);
                            SAME_RM_OVERRIDE.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case NO_TX_SEPARATE_POOLS: {
                            String value = rawElementText(reader);
                            //just presence means true
                            value = value == null ? "true" : value;
                            NOTXSEPARATEPOOL.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case PAD_XID: {
                            String value = rawElementText(reader);
                            PAD_XID.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case WRAP_XA_RESOURCE: {
                            String value = rawElementText(reader);
                            WRAP_XA_RESOURCE.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case CAPACITY: {
                            parseCapacity(reader, node);
                            break;
                        }

                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
            }
        }
        throw ParseUtils.unexpectedEndElement(reader);
    }


    protected void parsePool(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException, ParserException,
            ValidateException {

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DataSource.Tag.forName(reader.getLocalName()) == DataSource.Tag.POOL) {

                        return;

                    } else {
                        if (Pool.Tag.forName(reader.getLocalName()) == Pool.Tag.UNKNOWN) {
                            throw ParseUtils.unexpectedEndElement(reader);
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (Pool.Tag.forName(reader.getLocalName())) {
                        case MAX_POOL_SIZE: {
                            String value = rawElementText(reader);
                            MAX_POOL_SIZE.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case INITIAL_POOL_SIZE: {
                            String value = rawElementText(reader);
                            INITIAL_POOL_SIZE.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case MIN_POOL_SIZE: {
                            String value = rawElementText(reader);
                            MIN_POOL_SIZE.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case PREFILL: {
                            String value = rawElementText(reader);
                            POOL_PREFILL.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case FAIR: {
                            String value = rawElementText(reader);
                            POOL_FAIR.parseAndSetParameter( value, node, reader );
                            break;
                        }
                        case USE_STRICT_MIN: {
                            String value = rawElementText(reader);
                            POOL_USE_STRICT_MIN.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case FLUSH_STRATEGY: {
                            String value = rawElementText(reader);
                            POOL_FLUSH_STRATEGY.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case CAPACITY: {
                            parseCapacity(reader, node);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
            }
        }
        throw ParseUtils.unexpectedEndElement(reader);
    }

    private void parseCapacity(XMLExtendedStreamReader reader, final ModelNode operation) throws XMLStreamException, ParserException,
            ValidateException {

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DsPool.Tag.forName(reader.getLocalName()) == DsPool.Tag.CAPACITY) {

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
                            parseExtension(reader, reader.getLocalName(), operation, CAPACITY_INCREMENTER_CLASS, CAPACITY_INCREMENTER_PROPERTIES);
                            break;
                        }
                        case DECREMENTER: {
                            parseExtension(reader, reader.getLocalName(), operation, CAPACITY_DECREMENTER_CLASS, CAPACITY_DECREMENTER_PROPERTIES);
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

    protected void parseRecovery(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException, ParserException,
            ValidateException {


        for (Recovery.Attribute attribute : Recovery.Attribute.values()) {
            switch (attribute) {
                case NO_RECOVERY: {
                    String value = rawAttributeText(reader, NO_RECOVERY.getXmlName());
                    if (value != null) {
                        NO_RECOVERY.parseAndSetParameter(value, node, reader);
                    }
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
                            throw ParseUtils.unexpectedEndElement(reader);
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    Recovery.Tag tag = Recovery.Tag.forName(reader.getLocalName());
                    switch (tag) {
                        case RECOVER_CREDENTIAL: {
                            parseRecoveryCredential(reader, node);
                            break;
                        }
                        case RECOVER_PLUGIN: {
                            parseExtension(reader, tag.getLocalName(), node, RECOVERLUGIN_CLASSNAME, RECOVERLUGIN_PROPERTIES);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
            }
        }
        throw ParseUtils.unexpectedEndElement(reader);
    }

    protected void parseElytronSupportedRecovery(XMLExtendedStreamReader reader, ModelNode node)
            throws XMLStreamException, ParserException, ValidateException {

        for (Recovery.Attribute attribute : Recovery.Attribute.values()) {
            switch (attribute) {
                case NO_RECOVERY: {
                    String value = rawAttributeText(reader, NO_RECOVERY.getXmlName());
                    if (value != null) {
                        NO_RECOVERY.parseAndSetParameter(value, node, reader);
                    }
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
                            throw ParseUtils.unexpectedEndElement(reader);
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    Recovery.Tag tag = Recovery.Tag.forName(reader.getLocalName());
                    switch (tag) {
                        case RECOVER_CREDENTIAL: {
                            parseElytronSupportedRecoveryCredential(reader, node);
                            break;
                        }
                        case RECOVER_PLUGIN: {
                            parseExtension(reader, tag.getLocalName(), node, RECOVERLUGIN_CLASSNAME, RECOVERLUGIN_PROPERTIES);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
            }
        }
        throw ParseUtils.unexpectedEndElement(reader);
    }


    private void parseSecuritySettings(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException, ParserException,
            ValidateException {

        boolean securtyDomainMatched = false;
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DataSource.Tag.forName(reader.getLocalName()) == DataSource.Tag.SECURITY) {

                        return;
                    } else {
                        if (Security.Tag.forName(reader.getLocalName()) == Security.Tag.UNKNOWN) {
                            throw ParseUtils.unexpectedEndElement(reader);
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (Security.Tag.forName(reader.getLocalName())) {

                        case SECURITY_DOMAIN: {
                            if (securtyDomainMatched) {
                                throw ParseUtils.unexpectedElement(reader);
                            }
                            String value = rawElementText(reader);
                            SECURITY_DOMAIN.parseAndSetParameter(value, node, reader);
                            securtyDomainMatched = true;
                            break;
                        }
                        case SECURITY_DOMAIN_AND_APPLICATION: {
                            String value = rawElementText(reader);
                            SECURITY_DOMAIN_AND_APPLICATION.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case APPLICATION: {
                            String value = rawElementText(reader);
                            //just presence means true
                            value = value == null ? "true" : value;
                            APPLICATION.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
            }
        }
        throw ParseUtils.unexpectedEndElement(reader);
    }

    private void parseElytronSupportedSecuritySettings(XMLExtendedStreamReader reader, ModelNode node)
            throws XMLStreamException, ParserException, ValidateException {

        boolean securityDomainMatched = false;
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DataSource.Tag.forName(reader.getLocalName()) == DataSource.Tag.SECURITY) {

                        return;
                    } else {
                        if (Security.Tag.forName(reader.getLocalName()) == Security.Tag.UNKNOWN) {
                            throw ParseUtils.unexpectedEndElement(reader);
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (Security.Tag.forName(reader.getLocalName())) {

                        case SECURITY_DOMAIN: {
                            if (securityDomainMatched) {
                                throw ParseUtils.unexpectedElement(reader);
                            }
                            String value = rawElementText(reader);
                            SECURITY_DOMAIN.parseAndSetParameter(value, node, reader);
                            securityDomainMatched = true;
                            break;
                        }
                        case SECURITY_DOMAIN_AND_APPLICATION: {
                            String value = rawElementText(reader);
                            SECURITY_DOMAIN_AND_APPLICATION.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case ELYTRON_ENABLED: {
                            String value = rawElementText(reader);
                            value = value == null? "true": value;
                            ELYTRON_ENABLED.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case AUTHENTICATION_CONTEXT: {
                            String value = rawElementText(reader);
                            AUTHENTICATION_CONTEXT.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case AUTHENTICATION_CONTEXT_AND_APPLICATION: {
                            String value = rawElementText(reader);
                            AUTHENTICATION_CONTEXT_AND_APPLICATION.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case APPLICATION: {
                            String value = rawElementText(reader);
                            // just presence means true
                            value = value == null ? "true" : value;
                            APPLICATION.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
            }
        }
        throw ParseUtils.unexpectedEndElement(reader);
    }

    private void parseRecoveryCredential(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException, ParserException,
            ValidateException {


        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DataSource.Tag.forName(reader.getLocalName()) == DataSource.Tag.SECURITY ||
                            Recovery.Tag.forName(reader.getLocalName()) == Recovery.Tag.RECOVER_CREDENTIAL) {

                        return;
                    } else {
                        if (Credential.Tag.forName(reader.getLocalName()) == Credential.Tag.UNKNOWN) {
                            throw ParseUtils.unexpectedEndElement(reader);
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (Credential.Tag.forName(reader.getLocalName())) {
                        case PASSWORD: {
                            String value = rawElementText(reader);
                            RECOVERY_PASSWORD.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case USER_NAME: {
                            String value = rawElementText(reader);
                            RECOVERY_USERNAME.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case CREDENTIAL_REFERENCE: {
                            RECOVERY_CREDENTIAL_REFERENCE.getParser().parseAndSetParameter(RECOVERY_CREDENTIAL_REFERENCE, null, node, reader);
                        }
                        case SECURITY_DOMAIN: {
                            String value = rawElementText(reader);
                            RECOVERY_SECURITY_DOMAIN.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
            }
        }
        throw ParseUtils.unexpectedEndElement(reader);
    }

    private void parseElytronSupportedRecoveryCredential(XMLExtendedStreamReader reader, ModelNode node)
            throws XMLStreamException, ParserException, ValidateException {

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DataSource.Tag.forName(reader.getLocalName()) == DataSource.Tag.SECURITY
                            || Recovery.Tag.forName(reader.getLocalName()) == Recovery.Tag.RECOVER_CREDENTIAL) {

                        return;
                    } else {
                        if (Credential.Tag.forName(reader.getLocalName()) == Credential.Tag.UNKNOWN) {
                            throw ParseUtils.unexpectedEndElement(reader);
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (Credential.Tag.forName(reader.getLocalName())) {
                        case PASSWORD: {
                            String value = rawElementText(reader);
                            RECOVERY_PASSWORD.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case CREDENTIAL_REFERENCE: {
                            RECOVERY_CREDENTIAL_REFERENCE.getParser().parseAndSetParameter(RECOVERY_CREDENTIAL_REFERENCE, null, node, reader);
                            break;
                        }
                        case USER_NAME: {
                            String value = rawElementText(reader);
                            RECOVERY_USERNAME.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case SECURITY_DOMAIN: {
                            String value = rawElementText(reader);
                            RECOVERY_SECURITY_DOMAIN.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case ELYTRON_ENABLED: {
                            String value = rawElementText(reader);
                            value = value == null? "true": value;
                            RECOVERY_ELYTRON_ENABLED.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case AUTHENTICATION_CONTEXT: {
                            String value = rawElementText(reader);
                            RECOVERY_AUTHENTICATION_CONTEXT.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
            }
        }
        throw ParseUtils.unexpectedEndElement(reader);
    }

}