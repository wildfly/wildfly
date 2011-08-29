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
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.*;
import static org.jboss.as.connector.pool.Constants.*;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

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
    protected void parseConnectionDefinitions(final XMLStreamReader reader, final ModelNode operation) throws XMLStreamException,
            ParserException, ValidateException {

        ModelNode connectionDefinitionNode = new ModelNode();
        String jndiName = null;
        int attributeSize = reader.getAttributeCount();
        boolean isXa = Boolean.FALSE;
        boolean poolDefined = Boolean.FALSE;

        for (int i = 0; i < attributeSize; i++) {
            CommonConnDef.Attribute attribute = CommonConnDef.Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED: {
                    final Boolean value = attributeAsBoolean(reader, attribute.getLocalName());
                    if (value != null) connectionDefinitionNode.get(ENABLED).set(value);
                    break;
                }
                case JNDI_NAME: {
                    jndiName = attributeAsString(reader, attribute.getLocalName());
                    if (jndiName != null && jndiName.trim().length() != 0)
                        connectionDefinitionNode.get(JNDINAME).set(jndiName);
                    break;
                }
                case POOL_NAME: {
                    final String
                            value = attributeAsString(reader, attribute.getLocalName());
                    if (value != null && value.trim().length() != 0) connectionDefinitionNode.get(POOL_NAME).set(value);
                    break;
                }
                case USE_JAVA_CONTEXT: {
                    final Boolean value = attributeAsBoolean(reader, attribute.getLocalName());
                    if (value != null)
                        connectionDefinitionNode.get(USE_JAVA_CONTEXT).set(value);
                    break;
                }
                case CLASS_NAME: {
                    String className = attributeAsString(reader, attribute.getLocalName());
                    if (className != null && className.trim().length() != 0)
                        connectionDefinitionNode.get(CLASS_NAME).set(className);
                    break;
                }
                case USE_CCM: {
                    final Boolean value = attributeAsBoolean(reader, attribute.getLocalName());
                    if (value != null)
                        connectionDefinitionNode.get(USE_CCM).set(value);
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

                        operation.get(CONNECTIONDEFINITIONS).add(connectionDefinitionNode);
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
                            connectionDefinitionNode.get(CONFIG_PROPERTIES, attributeAsString(reader, "name")).set(elementAsString(reader));
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

    private void parseValidation(XMLStreamReader reader, ModelNode node) throws XMLStreamException, ParserException {

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
                        case BACKGROUND_VALIDATION_MILLIS: {
                            node.get(BACKGROUNDVALIDATIONMILLIS).set(elementAsLong(reader));
                            break;
                        }
                        case BACKGROUND_VALIDATION: {
                            node.get(BACKGROUNDVALIDATION).set(elementAsBoolean(reader));
                            break;
                        }
                        case USE_FAST_FAIL: {
                            node.get(USE_FAST_FAIL).set(elementAsBoolean(reader));
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

    private void parseTimeOut(XMLStreamReader reader, Boolean isXa, ModelNode node) throws XMLStreamException,
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
                        case ALLOCATION_RETRY_WAIT_MILLIS: {
                            node.get(ALLOCATION_RETRY_WAIT_MILLIS).set(elementAsLong(reader));
                            break;
                        }
                        case ALLOCATION_RETRY: {
                            node.get(ALLOCATION_RETRY).set(elementAsInteger(reader));
                            break;
                        }
                        case BLOCKING_TIMEOUT_MILLIS: {
                            node.get(BLOCKING_TIMEOUT_WAIT_MILLIS).set(elementAsLong(reader));
                            break;
                        }
                        case IDLE_TIMEOUT_MINUTES: {
                            node.get(IDLETIMEOUTMINUTES).set(elementAsLong(reader));
                            break;
                        }
                        case XA_RESOURCE_TIMEOUT: {
                            if (isXa != null && Boolean.FALSE.equals(isXa))
                                throw new ParserException(bundle.unsupportedElement(reader.getLocalName()));
                            node.get(XA_RESOURCE_TIMEOUT).set(elementAsInteger(reader));
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
     * parse a single admin-oject tag
     *
     * @param reader the reader
     * @return the parsed {@link org.jboss.jca.common.api.metadata.common.CommonAdminObject}
     * @throws javax.xml.stream.XMLStreamException
     *          XMLStreamException
     * @throws org.jboss.jca.common.metadata.ParserException
     *          ParserException
     */
    protected void parseAdminObjects(final XMLStreamReader reader, final ModelNode operation) throws XMLStreamException,
            ParserException {

        int attributeSize = reader.getAttributeCount();

        ModelNode adminObjectNode = new ModelNode();

        String jndiName = null;
        for (int i = 0; i < attributeSize; i++) {
            CommonAdminObject.Attribute attribute = CommonAdminObject.Attribute.forName(reader
                    .getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED: {
                    final Boolean value = attributeAsBoolean(reader, attribute.getLocalName());
                    if (value != null) adminObjectNode.get(ENABLED).set(value);
                    break;

                }
                case JNDI_NAME: {
                    jndiName = attributeAsString(reader, attribute.getLocalName());
                    if (jndiName != null && jndiName.trim().length() != 0) adminObjectNode.get(JNDINAME).set(jndiName);
                    break;
                }
                case CLASS_NAME: {
                    String className = attributeAsString(reader, attribute.getLocalName());
                    if (className != null && className.trim().length() != 0)
                        adminObjectNode.get(CLASS_NAME).set(className);
                    break;
                }
                case USE_JAVA_CONTEXT: {
                    final Boolean value = attributeAsBoolean(reader, attribute.getLocalName());
                    if (value != null)
                        adminObjectNode.get(USE_JAVA_CONTEXT).set(value);
                    break;
                }
                case POOL_NAME: {
                    final String
                            value = attributeAsString(reader, attribute.getLocalName());
                    if (value != null && value.trim().length() != 0) adminObjectNode.get(POOL_NAME).set(value);
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

                        operation.get(ADMIN_OBJECTS).add(adminObjectNode);
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
                            adminObjectNode.get(CONFIG_PROPERTIES, attributeAsString(reader, "name")).set(elementAsString(reader));
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
    protected void parseXaPool(XMLStreamReader reader, ModelNode node) throws XMLStreamException, ParserException,
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
                            node.get(MAX_POOL_SIZE).set(elementAsInteger(reader));
                            break;
                        }
                        case MIN_POOL_SIZE: {
                            node.get(MIN_POOL_SIZE).set(elementAsInteger(reader));
                            break;
                        }
                        case INTERLEAVING: {
                            node.get(INTERLEAVING).set(elementAsBoolean(reader));
                            break;
                        }
                        case IS_SAME_RM_OVERRIDE: {
                            node.get(SAME_RM_OVERRIDE).set(elementAsBoolean(reader));
                            break;
                        }
                        case NO_TX_SEPARATE_POOLS: {
                            node.get(NOTXSEPARATEPOOL).set(elementAsBoolean(reader));
                            break;
                        }
                        case PAD_XID: {
                            node.get(PAD_XID).set(elementAsBoolean(reader));
                            break;
                        }
                        case WRAP_XA_RESOURCE: {
                            node.get(WRAP_XA_RESOURCE).set(elementAsBoolean(reader));
                            break;
                        }
                        case PREFILL: {
                            node.get(POOL_PREFILL).set(elementAsBoolean(reader));
                            break;
                        }
                        case USE_STRICT_MIN: {
                            node.get(POOL_USE_STRICT_MIN).set(elementAsBoolean(reader));
                            break;
                        }
                        case FLUSH_STRATEGY: {
                            node.get(FLUSH_STRATEGY).set(elementAsString(reader));
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


    protected void parsePool(XMLStreamReader reader, ModelNode node) throws XMLStreamException, ParserException,
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
                            node.get(MAX_POOL_SIZE).set(elementAsInteger(reader));
                            break;
                        }
                        case MIN_POOL_SIZE: {
                            node.get(MIN_POOL_SIZE).set(elementAsInteger(reader));
                            break;
                        }
                        case PREFILL: {
                            node.get(POOL_PREFILL).set(elementAsBoolean(reader));
                            break;
                        }
                        case USE_STRICT_MIN: {
                            node.get(POOL_USE_STRICT_MIN).set(elementAsBoolean(reader));
                            break;
                        }
                        case FLUSH_STRATEGY: {
                            node.get(FLUSH_STRATEGY).set(elementAsString(reader));
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


    protected void parseRecovery(XMLStreamReader reader, ModelNode node) throws XMLStreamException, ParserException,
            ValidateException {


        for (Recovery.Attribute attribute : Recovery.Attribute.values()) {
            switch (attribute) {
                case NO_RECOVERY: {
                    node.get(NO_RECOVERY).set(attributeAsBoolean(reader, attribute.getLocalName()));
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

    private void parseSecuritySettings(XMLStreamReader reader, ModelNode node) throws XMLStreamException, ParserException,
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
                            node.get(SECURITY_DOMAIN).set(elementAsString(reader));
                            break;
                        }
                        case SECURITY_DOMAIN_AND_APPLICATION: {
                            node.get(SECURITY_DOMAIN_AND_APPLICATION).set(elementAsString(reader));
                            break;
                        }
                        case APPLICATION: {
                            node.get(APPLICATION).set(elementAsBoolean(reader));
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

    private void parseRecoveryCredential(XMLStreamReader reader, ModelNode node) throws XMLStreamException, ParserException,
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
                            node.get(RECOVERY_PASSWORD).set(elementAsString(reader));
                            break;
                        }
                        case USER_NAME: {
                            node.get(RECOVERY_USERNAME).set(elementAsString(reader));
                            break;
                        }
                        case SECURITY_DOMAIN: {
                            node.get(RECOVERY_SECURITY_DOMAIN).set(elementAsString(reader));
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
