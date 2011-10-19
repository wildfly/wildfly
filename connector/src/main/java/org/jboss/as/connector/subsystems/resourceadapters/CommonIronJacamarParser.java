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
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ADMIN_OBJECTS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CLASS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONFIG_PROPERTIES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONFIG_PROPERTY_VALUE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONNECTIONDEFINITIONS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.INTERLEAVING;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.JNDINAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.NOTXSEPARATEPOOL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.NO_RECOVERY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.PAD_XID;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.POOL_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERLUGIN_CLASSNAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERLUGIN_PROPERTIES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_PASSWORD;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_USERNAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SAME_RM_OVERRIDE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SECURITY_DOMAIN_AND_APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_CCM;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WRAP_XA_RESOURCE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.XA_RESOURCE_TIMEOUT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.connector.util.AbstractParser;
import org.jboss.as.connector.util.ParserException;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.CommonBundle;
import org.jboss.jca.common.api.metadata.common.CommonAdminObject;
import org.jboss.jca.common.api.metadata.common.CommonConnDef;
import org.jboss.jca.common.api.metadata.common.CommonPool;
import org.jboss.jca.common.api.metadata.common.CommonSecurity;
import org.jboss.jca.common.api.metadata.common.CommonTimeOut;
import org.jboss.jca.common.api.metadata.common.CommonValidation;
import org.jboss.jca.common.api.metadata.common.CommonXaPool;
import org.jboss.jca.common.api.metadata.common.Credential;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.metadata.ds.XaDataSource;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapter;
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


    protected void parseConfigProperties(final XMLExtendedStreamReader reader, final Map<String,ModelNode> map) throws XMLStreamException {

            String name = rawAttributeText(reader, "name");


            final ModelNode operation = new ModelNode();
            operation.get(OP).set(ADD);

            String value = rawElementText(reader);

            CONFIG_PROPERTY_VALUE.parseAndSetParameter(value,operation,reader.getLocation());

            map.put(name, operation);
        }

    /**
     * parse a single connection-definition tag
     *
     * @param reader the reader
     * @return the parse {@link org.jboss.jca.common.api.metadata.common.CommonConnDef} object
     * @throws javax.xml.stream.XMLStreamException
     *          XMLStreamException
     * @throws org.jboss.jca.common.metadata.ParserException
     *          ParserException
     * @throws org.jboss.jca.common.api.validator.ValidateException
     *          ValidateException
     */
    protected void parseConnectionDefinitions(final XMLExtendedStreamReader reader, final Map<String,ModelNode> map, final Map<String,HashMap<String, ModelNode>> configMap)
            throws XMLStreamException, ParserException, ValidateException {


        final ModelNode connectionDefinitionNode = new ModelNode();
        connectionDefinitionNode.get(OP).set(ADD);

        String jndiName = null;
        int attributeSize = reader.getAttributeCount();
        boolean isXa = Boolean.FALSE;
        boolean poolDefined = Boolean.FALSE;

        for (int i = 0; i < attributeSize; i++) {
            CommonConnDef.Attribute attribute = CommonConnDef.Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {

                case ENABLED: {
                    final Location location = reader.getLocation();
                    String value = rawAttributeText(reader, ENABLED.getXmlName());
                    ENABLED.parseAndSetParameter(value, connectionDefinitionNode, location);
                    break;
                }
                case JNDI_NAME: {
                    final Location location = reader.getLocation();
                    jndiName = rawAttributeText(reader, JNDINAME.getXmlName());
                    JNDINAME.parseAndSetParameter(jndiName, connectionDefinitionNode, location);
                    break;
                }
                case POOL_NAME: {
                    final Location location = reader.getLocation();
                    String value = rawAttributeText(reader, POOL_NAME.getXmlName());
                    POOL_NAME.parseAndSetParameter(value, connectionDefinitionNode, location);
                    break;
                }
                case USE_JAVA_CONTEXT: {
                    final Location location = reader.getLocation();
                    String value = rawAttributeText(reader, USE_JAVA_CONTEXT.getXmlName());
                    USE_JAVA_CONTEXT.parseAndSetParameter(value, connectionDefinitionNode, location);
                    break;
                }

                case USE_CCM: {
                    final Location location = reader.getLocation();
                    String value = rawAttributeText(reader, USE_CCM.getXmlName());
                    USE_CCM.parseAndSetParameter(value, connectionDefinitionNode, location);
                    break;
                }

                case CLASS_NAME: {
                    final Location location = reader.getLocation();
                    String value = rawAttributeText(reader, CLASS_NAME.getXmlName());
                    CLASS_NAME.parseAndSetParameter(value, connectionDefinitionNode, location);
                    break;
                }
                default:
                    break;
            }
        }
        if (jndiName == null || jndiName.trim().equals(""))
            throw new ParserException(bundle.missingJndiName(reader.getLocalName()));

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (ResourceAdapter.Tag.forName(reader.getLocalName()) == ResourceAdapter.Tag.CONNECTION_DEFINITION) {

                        map.put(jndiName, connectionDefinitionNode);
                        return;
                    } else {
                        if (CommonConnDef.Tag.forName(reader.getLocalName()) == CommonConnDef.Tag.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (CommonConnDef.Tag.forName(reader.getLocalName())) {
                        case CONFIG_PROPERTY: {
                            if (! configMap.containsKey(jndiName)) {
                                configMap.put(jndiName, new HashMap<String, ModelNode>(0));
                            }
                            parseConfigProperties(reader, configMap.get(jndiName));
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
                            if (poolDefined)
                                throw new ParserException(bundle.multiplePools());
                            parseXaPool(reader, connectionDefinitionNode);
                            isXa = true;
                            poolDefined = true;
                            break;
                        }
                        case POOL: {
                            if (poolDefined)
                                throw new ParserException(bundle.multiplePools());
                            parsePool(reader, connectionDefinitionNode);
                            poolDefined = true;
                            break;
                        }
                        case RECOVERY: {
                            parseRecovery(reader, connectionDefinitionNode);
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

    private void parseValidation(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException, ParserException {

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (CommonConnDef.Tag.forName(reader.getLocalName()) == CommonConnDef.Tag.VALIDATION) {

                        return;
                    } else {
                        if (CommonValidation.Tag.forName(reader.getLocalName()) == CommonValidation.Tag.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (CommonValidation.Tag.forName(reader.getLocalName())) {
                        case BACKGROUND_VALIDATION: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            BACKGROUNDVALIDATION.parseAndSetParameter(value, node, location);
                            break;
                        }
                        case BACKGROUND_VALIDATION_MILLIS: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            BACKGROUNDVALIDATIONMILLIS.parseAndSetParameter(value, node, location);
                            break;
                        }
                        case USE_FAST_FAIL: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            USE_FAST_FAIL.parseAndSetParameter(value, node, location);
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

    private void parseTimeOut(XMLExtendedStreamReader reader, Boolean isXa, ModelNode node) throws XMLStreamException,
            ParserException, ValidateException {

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (CommonConnDef.Tag.forName(reader.getLocalName()) == CommonConnDef.Tag.TIMEOUT) {

                        return;
                    } else {
                        if (CommonTimeOut.Tag.forName(reader.getLocalName()) == CommonTimeOut.Tag.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (CommonTimeOut.Tag.forName(reader.getLocalName())) {
                        case ALLOCATION_RETRY: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            ALLOCATION_RETRY.parseAndSetParameter(value, node, location);
                            break;
                        }
                        case ALLOCATION_RETRY_WAIT_MILLIS: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            ALLOCATION_RETRY_WAIT_MILLIS.parseAndSetParameter(value, node, location);
                            break;
                        }
                        case BLOCKING_TIMEOUT_MILLIS: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            BLOCKING_TIMEOUT_WAIT_MILLIS.parseAndSetParameter(value, node, location);
                            break;
                        }
                        case IDLE_TIMEOUT_MINUTES: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            IDLETIMEOUTMINUTES.parseAndSetParameter(value, node, location);
                            break;
                        }
                        case XA_RESOURCE_TIMEOUT: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            XA_RESOURCE_TIMEOUT.parseAndSetParameter(value, node, location);
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


    protected void parseAdminObjects(final XMLExtendedStreamReader reader, final Map<String,ModelNode> map, final Map<String,HashMap<String, ModelNode>> configMap)
            throws XMLStreamException, ParserException, ValidateException {


        final ModelNode adminObjectNode = new ModelNode();
        adminObjectNode.get(OP).set(ADD);
        int attributeSize = reader.getAttributeCount();


        String jndiName = null;
        for (int i = 0; i < attributeSize; i++) {
            CommonAdminObject.Attribute attribute = CommonAdminObject.Attribute.forName(reader
                    .getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED: {
                    final Location location = reader.getLocation();
                    String value = rawAttributeText(reader, ENABLED.getXmlName());
                    ENABLED.parseAndSetParameter(value, adminObjectNode, location);
                    break;
                }
                case JNDI_NAME: {
                    final Location location = reader.getLocation();
                    jndiName = rawAttributeText(reader, JNDINAME.getXmlName());
                    JNDINAME.parseAndSetParameter(jndiName, adminObjectNode, location);
                    break;
                }
                case POOL_NAME: {
                    final Location location = reader.getLocation();
                    String value = rawAttributeText(reader, POOL_NAME.getXmlName());
                    POOL_NAME.parseAndSetParameter(value, adminObjectNode, location);
                    break;
                }
                case USE_JAVA_CONTEXT: {
                    final Location location = reader.getLocation();
                    String value = rawAttributeText(reader, USE_JAVA_CONTEXT.getXmlName());
                    USE_JAVA_CONTEXT.parseAndSetParameter(value, adminObjectNode, location);
                    break;
                }
                case CLASS_NAME: {
                    final Location location = reader.getLocation();
                    String value = rawAttributeText(reader, CLASS_NAME.getXmlName());
                    CLASS_NAME.parseAndSetParameter(value, adminObjectNode, location);
                    break;
                }
                default:
                    throw new ParserException(bundle.unexpectedAttribute(attribute.getLocalName(), reader.getLocalName()));
            }
        }
        if (jndiName == null || jndiName.trim().equals(""))
            throw new ParserException(bundle.missingJndiName(reader.getLocalName()));

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (ResourceAdapter.Tag.forName(reader.getLocalName()) == ResourceAdapter.Tag.ADMIN_OBJECT) {

                        map.put(jndiName, adminObjectNode);
                        return;
                    } else {
                        if (CommonAdminObject.Tag.forName(reader.getLocalName()) == CommonAdminObject.Tag.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (CommonAdminObject.Tag.forName(reader.getLocalName())) {
                        case CONFIG_PROPERTY: {
                            if (! configMap.containsKey(jndiName)) {
                                configMap.put(jndiName, new HashMap<String, ModelNode>(0));
                            }
                            parseConfigProperties(reader, configMap.get(jndiName));
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
     * parse a {@link org.jboss.jca.common.api.metadata.common.CommonXaPool} object
     *
     * @param reader reader
     * @return the parsed {@link org.jboss.jca.common.api.metadata.common.CommonXaPool} object
     * @throws XMLStreamException XMLStreamException
     * @throws org.jboss.jca.common.metadata.ParserException
     *                            ParserException
     * @throws ValidateException  ValidateException
     */
    protected void parseXaPool(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException, ParserException,
            ValidateException {


        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (XaDataSource.Tag.forName(reader.getLocalName()) == XaDataSource.Tag.XA_POOL) {

                        return;

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
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            MAX_POOL_SIZE.parseAndSetParameter(value, node, location);
                            break;
                        }
                        case MIN_POOL_SIZE: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            MIN_POOL_SIZE.parseAndSetParameter(value, node, location);
                            break;
                        }

                        case PREFILL: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            POOL_PREFILL.parseAndSetParameter(value, node, location);
                            break;
                        }
                        case USE_STRICT_MIN: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            POOL_USE_STRICT_MIN.parseAndSetParameter(value, node, location);
                            break;
                        }
                        case FLUSH_STRATEGY: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            POOL_FLUSH_STRATEGY.parseAndSetParameter(value, node, location);
                            break;
                        }
                        case INTERLEAVING: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            INTERLEAVING.parseAndSetParameter(value, node, location);
                            break;
                        }
                        case IS_SAME_RM_OVERRIDE: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            SAME_RM_OVERRIDE.parseAndSetParameter(value, node, location);
                            break;
                        }
                        case NO_TX_SEPARATE_POOLS: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            NOTXSEPARATEPOOL.parseAndSetParameter(value, node, location);
                            break;
                        }
                        case PAD_XID: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            PAD_XID.parseAndSetParameter(value, node, location);
                            break;
                        }
                        case WRAP_XA_RESOURCE: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            WRAP_XA_RESOURCE.parseAndSetParameter(value, node, location);
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


    protected void parsePool(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException, ParserException,
            ValidateException {

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DataSource.Tag.forName(reader.getLocalName()) == DataSource.Tag.POOL) {

                        return;

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
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            MAX_POOL_SIZE.parseAndSetParameter(value, node, location);
                            break;
                        }
                        case MIN_POOL_SIZE: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            MIN_POOL_SIZE.parseAndSetParameter(value, node, location);
                            break;
                        }

                        case PREFILL: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            POOL_PREFILL.parseAndSetParameter(value, node, location);
                            break;
                        }
                        case USE_STRICT_MIN: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            POOL_USE_STRICT_MIN.parseAndSetParameter(value, node, location);
                            break;
                        }
                        case FLUSH_STRATEGY: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            POOL_FLUSH_STRATEGY.parseAndSetParameter(value, node, location);
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
                    final Location location = reader.getLocation();
                    String value = rawAttributeText(reader, NO_RECOVERY.getXmlName());
                    NO_RECOVERY.parseAndSetParameter(value, node, location);
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
                            parseRecoveryCredential(reader, node);
                            break;
                        }
                        case RECOVER_PLUGIN: {
                            parseExtension(reader, tag.getLocalName(), node, RECOVERLUGIN_CLASSNAME, RECOVERLUGIN_PROPERTIES);
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

    private void parseSecuritySettings(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException, ParserException,
            ValidateException {

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DataSource.Tag.forName(reader.getLocalName()) == DataSource.Tag.SECURITY) {

                        return;
                    } else {
                        if (CommonSecurity.Tag.forName(reader.getLocalName()) == CommonSecurity.Tag.UNKNOWN) {
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (CommonSecurity.Tag.forName(reader.getLocalName())) {

                        case SECURITY_DOMAIN: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            SECURITY_DOMAIN.parseAndSetParameter(value, node, location);
                            break;
                        }
                        case SECURITY_DOMAIN_AND_APPLICATION: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            SECURITY_DOMAIN_AND_APPLICATION.parseAndSetParameter(value, node, location);
                            break;
                        }
                        case APPLICATION: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            APPLICATION.parseAndSetParameter(value, node, location);
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
                            throw new ParserException(bundle.unexpectedEndTag(reader.getLocalName()));
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (Credential.Tag.forName(reader.getLocalName())) {
                        case PASSWORD: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            RECOVERY_PASSWORD.parseAndSetParameter(value, node, location);
                            break;
                        }
                        case USER_NAME: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            RECOVERY_USERNAME.parseAndSetParameter(value, node, location);
                            break;
                        }
                        case SECURITY_DOMAIN: {
                            final Location location = reader.getLocation();
                            String value = rawElementText(reader);
                            RECOVERY_SECURITY_DOMAIN.parseAndSetParameter(value, node, location);
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
}
