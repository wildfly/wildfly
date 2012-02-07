/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.parsing;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.controller.ControllerLogger.ROOT_LOGGER;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHENTICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHORIZATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HTTP_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JAAS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LDAP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LDAP_CONNECTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NATIVE_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NATIVE_REMOTING_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PLAIN_TEXT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECRET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_IDENTITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SSL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TRUSTSTORE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERS;
import static org.jboss.as.controller.parsing.ParseUtils.isNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.missingOneOf;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequiredElement;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNamespace;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireSingleAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.domain.management.connections.ldap.LdapConnectionResourceDefinition;
import org.jboss.as.domain.management.security.JaasAuthenticationResourceDefinition;
import org.jboss.as.domain.management.security.KeystoreAttributes;
import org.jboss.as.domain.management.security.LdapAuthenticationResourceDefinition;
import org.jboss.as.domain.management.security.PropertiesAuthenticationResourceDefinition;
import org.jboss.as.domain.management.security.PropertiesAuthorizationResourceDefinition;
import org.jboss.as.domain.management.security.SSLServerIdentityResourceDefinition;
import org.jboss.as.domain.management.security.SecretServerIdentityResourceDefinition;
import org.jboss.as.domain.management.security.UserResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Bits of parsing and marshaling logic that are related to {@code <management>} elements in domain.xml, host.xml
 * and standalone.xml.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ManagementXml {

    /** Handles config-file specific aspects of parsing and marshalling {@code <management>} elements */
    public interface Delegate {

        void parseManagementInterfaces(XMLExtendedStreamReader reader, ModelNode address, Namespace expectedNs, List<ModelNode> list) throws XMLStreamException;

        void writeNativeManagementProtocol(XMLExtendedStreamWriter writer, ModelNode protocol) throws XMLStreamException;

        void writeHttpManagementProtocol(XMLExtendedStreamWriter writer, ModelNode protocol) throws XMLStreamException;
    }

    private final Delegate delegate;


    public ManagementXml(Delegate delegate) {
        this.delegate = delegate;
    }

    public void parseManagement(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list,
                                   boolean allowInterfaces, boolean requireNativeInterface) throws XMLStreamException {
        int securityRealmsCount = 0;
        int connectionsCount = 0;
        int managementInterfacesCount = 0;

        final ModelNode managementAddress = address.clone().add(CORE_SERVICE, MANAGEMENT);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case SECURITY_REALMS: {
                    if (++securityRealmsCount > 1) {
                        throw unexpectedElement(reader);
                    }
                    parseSecurityRealms(reader, managementAddress, expectedNs, list);

                    break;
                }
                case OUTBOUND_CONNECTIONS: {
                    if (++connectionsCount > 1) {
                        throw unexpectedElement(reader);
                    }
                    parseConnections(reader, managementAddress, expectedNs, list);
                    break;
                }
                case MANAGEMENT_INTERFACES: {
                    if (allowInterfaces) {
                        if (++managementInterfacesCount > 1) {
                            throw unexpectedElement(reader);
                        }

                        delegate.parseManagementInterfaces(reader, managementAddress, expectedNs, list);

                    } else {
                        ROOT_LOGGER.warn(ParseUtils.getWarningMessage(MESSAGES.elementNotSupported(element.getLocalName(), "domain.xml"), reader.getLocation()));
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        if (requireNativeInterface && managementInterfacesCount < 1) {
            throw missingRequiredElement(reader, EnumSet.of(Element.MANAGEMENT_INTERFACES));
        }
    }

    private void parseConnections(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list)
            throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case LDAP: {
                    parseLdapConnection(reader, address, list);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseLdapConnection(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list)
            throws XMLStreamException {

        final ModelNode add = new ModelNode();
        add.get(OP).set(ADD);

        list.add(add);

        Set<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.URL, Attribute.SEARCH_DN, Attribute.SEARCH_CREDENTIAL);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                required.remove(attribute);
                switch (attribute) {
                    case NAME: {
                        add.get(OP_ADDR).set(address).add(LDAP_CONNECTION, value);
                        break;
                    }
                    case URL: {
                        LdapConnectionResourceDefinition.URL.parseAndSetParameter(value, add, reader);
                        break;
                    }
                    case SEARCH_DN: {
                        LdapConnectionResourceDefinition.SEARCH_DN.parseAndSetParameter(value,  add, reader);
                        break;
                    }
                    case SEARCH_CREDENTIAL: {
                        LdapConnectionResourceDefinition.SEARCH_CREDENTIAL.parseAndSetParameter(value, add, reader);
                        break;
                    }
                    case INITIAL_CONTEXT_FACTORY: {
                        LdapConnectionResourceDefinition.INITIAL_CONTEXT_FACTORY.parseAndSetParameter(value, add, reader);
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

        if (required.size() > 0) {
            throw missingRequired(reader, required);
        }

        requireNoContent(reader);
    }

    private void parseSecurityRealms(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list)
            throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);

            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case SECURITY_REALM: {
                    switch (expectedNs) {
                        case DOMAIN_1_0:
                            parseSecurityRealm_1_0(reader, address, expectedNs, list);
                            break;
                        default:
                            parseSecurityRealm_1_1(reader, address, expectedNs, list);
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseSecurityRealm_1_0(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list)
            throws XMLStreamException {
        requireSingleAttribute(reader, Attribute.NAME.getLocalName());
        // After double checking the name of the only attribute we can retrieve it.
        final String realmName = reader.getAttributeValue(0);

        final ModelNode realmAddress = address.clone();
        realmAddress.add(SECURITY_REALM, realmName);
        final ModelNode add = new ModelNode();
        add.get(OP_ADDR).set(realmAddress);
        add.get(OP).set(ADD);
        list.add(add);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);

            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case SERVER_IDENTITIES:
                    parseServerIdentities(reader, expectedNs, realmAddress, list);
                    break;
                case AUTHENTICATION: {
                    parseAuthentication_1_0(reader, expectedNs, realmAddress, list);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseSecurityRealm_1_1(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list)
            throws XMLStreamException {
        requireSingleAttribute(reader, Attribute.NAME.getLocalName());
        // After double checking the name of the only attribute we can retrieve it.
        final String realmName = reader.getAttributeValue(0);

        final ModelNode realmAddress = address.clone();
        realmAddress.add(SECURITY_REALM, realmName);
        final ModelNode add = new ModelNode();
        add.get(OP_ADDR).set(realmAddress);
        add.get(OP).set(ADD);
        list.add(add);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);

            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case SERVER_IDENTITIES:
                    parseServerIdentities(reader, expectedNs, realmAddress, list);
                    break;
                case AUTHENTICATION: {
                    parseAuthentication_1_1(reader, expectedNs, realmAddress, list);
                    break;
                }
                case AUTHORIZATION:
                    parseAuthorization(reader, expectedNs, realmAddress, list);
                    break;
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseServerIdentities(final XMLExtendedStreamReader reader, final Namespace expectedNs, final ModelNode realmAddress, final List<ModelNode> list)
            throws XMLStreamException {

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);

            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case SECRET: {
                    parseSecret(reader, realmAddress, list);
                    break;
                }
                case SSL: {
                    parseSSL(reader, expectedNs, realmAddress, list);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseSecret(final XMLExtendedStreamReader reader, final ModelNode realmAddress, final List<ModelNode> list)
            throws XMLStreamException {

        ModelNode secret = new ModelNode();
        secret.get(OP).set(ADD);
        secret.get(OP_ADDR).set(realmAddress).add(SERVER_IDENTITY, SECRET);
        String secretValue = readStringAttributeElement(reader, Attribute.VALUE.getLocalName());
        SecretServerIdentityResourceDefinition.VALUE.parseAndSetParameter(secretValue, secret, reader);

        list.add(secret);
    }

    private void parseSSL(final XMLExtendedStreamReader reader, final Namespace expectedNs, final ModelNode realmAddress, final List<ModelNode> list) throws XMLStreamException {

        ModelNode ssl = new ModelNode();
        ssl.get(OP).set(ADD);
        ssl.get(OP_ADDR).set(realmAddress).add(SERVER_IDENTITY, SSL);
        list.add(ssl);

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case PROTOCOL: {
                        SSLServerIdentityResourceDefinition.PROTOCOL.parseAndSetParameter(value, ssl, reader);
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);

            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case KEYSTORE: {
                    parseKeystore(reader, ssl);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

    }

    private void parseKeystore(final XMLExtendedStreamReader reader, final ModelNode addOperation)
            throws XMLStreamException {

        Set<Attribute> required = EnumSet.of(Attribute.PATH, Attribute.PASSWORD);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                required.remove(attribute);
                switch (attribute) {
                    case PATH:
                        KeystoreAttributes.KEYSTORE_PATH.parseAndSetParameter(value, addOperation, reader);
                        break;
                    case PASSWORD: {
                        KeystoreAttributes.KEYSTORE_PASSWORD.parseAndSetParameter(value, addOperation, reader);
                        break;
                    }
                    case RELATIVE_TO: {
                        KeystoreAttributes.KEYSTORE_RELATIVE_TO.parseAndSetParameter(value, addOperation, reader);
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

        if (required.size() > 0) {
            throw missingRequired(reader, required);
        }

        requireNoContent(reader);
    }

    private void parseAuthentication_1_0(final XMLExtendedStreamReader reader, final Namespace expectedNs, final ModelNode realmAddress, final List<ModelNode> list)
            throws XMLStreamException {
        int userCount = 0;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            // Only a single user element within the authentication element is currently supported.
            if (++userCount > 1) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case LDAP: {
                    parseLdapAuthentication_1_0(reader, realmAddress, list);
                    break;
                }
                case PROPERTIES: {
                    parsePropertiesAuthentication_1_0(reader, realmAddress, list);
                    break;
                }
                case USERS: {
                    parseUsersAuthentication(reader, expectedNs, realmAddress, list);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }

        }
    }

    private void parseAuthentication_1_1(final XMLExtendedStreamReader reader, final Namespace expectedNs,
            final ModelNode realmAddress, final List<ModelNode> list) throws XMLStreamException {

        // Only one truststore can be defined.
        boolean trustStoreFound = false;
        // Only one of ldap, properties or users can be defined.
        boolean usernamePasswordFound = false;

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());

            switch (element) {
                case JAAS: {
                    if (usernamePasswordFound) {
                        throw unexpectedElement(reader);
                    }
                    parseJaasAuthentication(reader, expectedNs, realmAddress, list);
                    usernamePasswordFound = true;
                    break;
                }
                case LDAP: {
                    if (usernamePasswordFound) {
                        throw unexpectedElement(reader);
                    }
                    parseLdapAuthentication_1_1(reader, expectedNs, realmAddress, list);
                    usernamePasswordFound = true;
                    break;
                }
                case PROPERTIES: {
                    if (usernamePasswordFound) {
                        throw unexpectedElement(reader);
                    }
                    parsePropertiesAuthentication_1_1(reader, realmAddress, list);
                    usernamePasswordFound = true;
                    break;
                }
                case TRUSTSTORE: {
                    if (trustStoreFound) {
                        throw unexpectedElement(reader);
                    }
                    parseTruststore(reader, realmAddress, list);
                    trustStoreFound = true;
                    break;
                }
                case USERS: {
                    if (usernamePasswordFound) {
                        throw unexpectedElement(reader);
                    }
                    parseUsersAuthentication(reader, expectedNs, realmAddress, list);
                    usernamePasswordFound = true;
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }

        }
    }

    private void parseLdapAuthentication_1_0(final XMLExtendedStreamReader reader, final ModelNode realmAddress, final List<ModelNode> list)
            throws XMLStreamException {

        ModelNode addr = realmAddress.clone().add(AUTHENTICATION, LDAP);
        ModelNode ldapAuthentication = Util.getEmptyOperation(ADD, addr);

        list.add(ldapAuthentication);

        Set<Attribute> required = EnumSet.of(Attribute.CONNECTION, Attribute.BASE_DN, Attribute.USERNAME_ATTRIBUTE);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                required.remove(attribute);
                switch (attribute) {
                    case CONNECTION: {
                        LdapAuthenticationResourceDefinition.CONNECTION.parseAndSetParameter(value, ldapAuthentication, reader);
                        break;
                    }
                    case BASE_DN: {
                        LdapAuthenticationResourceDefinition.BASE_DN.parseAndSetParameter(value, ldapAuthentication, reader);
                        break;
                    }
                    case USERNAME_ATTRIBUTE: {
                        LdapAuthenticationResourceDefinition.USERNAME_FILTER.parseAndSetParameter(value, ldapAuthentication, reader);
                        break;
                    }
                    case RECURSIVE: {
                        LdapAuthenticationResourceDefinition.RECURSIVE.parseAndSetParameter(value, ldapAuthentication, reader);
                        break;
                    }
                    case USER_DN: {
                        LdapAuthenticationResourceDefinition.USER_DN.parseAndSetParameter(value, ldapAuthentication, reader);
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

        if (required.size() > 0)
            throw missingRequired(reader, required);

        requireNoContent(reader);
    }

    private void parseJaasAuthentication(final XMLExtendedStreamReader reader, final Namespace expectedNs,
            final ModelNode realmAddress, final List<ModelNode> list) throws XMLStreamException {
        ModelNode addr = realmAddress.clone().add(AUTHENTICATION, JAAS);
        ModelNode jaas = Util.getEmptyOperation(ADD, addr);
        list.add(jaas);

        boolean nameFound = false;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME:
                        if (nameFound) {
                            throw unexpectedAttribute(reader, i);
                        }
                        nameFound = true;
                        JaasAuthenticationResourceDefinition.NAME.parseAndSetParameter(value, jaas, reader);
                        break;
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }
        if (nameFound == false) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        requireNoContent(reader);
    }

    private void parseLdapAuthentication_1_1(final XMLExtendedStreamReader reader, final Namespace expectedNs,
                                             final ModelNode realmAddress, final List<ModelNode> list)
            throws XMLStreamException {
        ModelNode addr = realmAddress.clone().add(AUTHENTICATION, LDAP);
        ModelNode ldapAuthentication = Util.getEmptyOperation(ADD, addr);

        list.add(ldapAuthentication);

        Set<Attribute> required = EnumSet.of(Attribute.CONNECTION, Attribute.BASE_DN);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                required.remove(attribute);
                switch (attribute) {
                    case CONNECTION: {
                        LdapAuthenticationResourceDefinition.CONNECTION.parseAndSetParameter(value, ldapAuthentication, reader);
                        break;
                    }
                    case BASE_DN: {
                        LdapAuthenticationResourceDefinition.BASE_DN.parseAndSetParameter(value, ldapAuthentication, reader);
                        break;
                    }
                    case RECURSIVE: {
                        LdapAuthenticationResourceDefinition.RECURSIVE.parseAndSetParameter(value, ldapAuthentication, reader);
                        break;
                    }
                    case USER_DN: {
                        LdapAuthenticationResourceDefinition.USER_DN.parseAndSetParameter(value, ldapAuthentication, reader);
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

        if (required.size() > 0) {
            throw missingRequired(reader, required);
        }

        boolean choiceFound = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if (choiceFound) {
                throw unexpectedElement(reader);
            }
            choiceFound = true;
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ADVANCED_FILTER:
                    String filter = readStringAttributeElement(reader, Attribute.FILTER.getLocalName());
                    LdapAuthenticationResourceDefinition.ADVANCED_FILTER.parseAndSetParameter(filter, ldapAuthentication, reader);
                    break;
                case USERNAME_FILTER: {
                    String usernameAttr = readStringAttributeElement(reader, Attribute.ATTRIBUTE.getLocalName());
                    LdapAuthenticationResourceDefinition.USERNAME_FILTER.parseAndSetParameter(usernameAttr, ldapAuthentication, reader);
                    break;
                }

                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if (!choiceFound) {
            throw missingOneOf(reader, EnumSet.of(Element.ADVANCED_FILTER, Element.USERNAME_FILTER));
        }
    }

    private void parsePropertiesAuthentication_1_0(final XMLExtendedStreamReader reader,
                                                   final ModelNode realmAddress, final List<ModelNode> list)
            throws XMLStreamException {
        ModelNode addr = realmAddress.clone().add(AUTHENTICATION, PROPERTIES);
        ModelNode properties = Util.getEmptyOperation(ADD, addr);

        list.add(properties);

        String path = null;

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case PATH:
                        path = value;
                        PropertiesAuthenticationResourceDefinition.PATH.parseAndSetParameter(value, properties, reader);
                        break;
                    case RELATIVE_TO: {
                        PropertiesAuthenticationResourceDefinition.RELATIVE_TO.parseAndSetParameter(value, properties, reader);
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

        if (path == null)
            throw missingRequired(reader, Collections.singleton(Attribute.PATH));

        requireNoContent(reader);
        // This property was not supported in version 1.0 of the schema, however it is set to true here to ensure
        // the default behaviour if a document based on 1.0 of the schema is parsed, 1.1 now defaults this to false.
        properties.get(PLAIN_TEXT).set(true);
    }

    private void parsePropertiesAuthentication_1_1(final XMLExtendedStreamReader reader,
                                                   final ModelNode realmAddress, final List<ModelNode> list)
            throws XMLStreamException {
        ModelNode addr = realmAddress.clone().add(AUTHENTICATION, PROPERTIES);
        ModelNode properties = Util.getEmptyOperation(ADD, addr);
        list.add(properties);

        String path = null;

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case PATH:
                        path = value;
                        PropertiesAuthenticationResourceDefinition.PATH.parseAndSetParameter(value, properties, reader);
                        break;
                    case RELATIVE_TO: {
                        PropertiesAuthenticationResourceDefinition.RELATIVE_TO.parseAndSetParameter(value, properties, reader);
                        break;
                    }
                    case PLAIN_TEXT: {
                        PropertiesAuthenticationResourceDefinition.PLAIN_TEXT.parseAndSetParameter(value, properties, reader);
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

        if (path == null)
            throw missingRequired(reader, Collections.singleton(Attribute.PATH));

        requireNoContent(reader);
    }

    // The users element defines users within the domain model, it is a simple authentication for some out of the box users.
    private void parseUsersAuthentication(final XMLExtendedStreamReader reader, final Namespace expectedNs,
                                          final ModelNode realmAddress, final List<ModelNode> list)
            throws XMLStreamException {
        final ModelNode usersAddress = realmAddress.clone().add(AUTHENTICATION, USERS);
        list.add(Util.getEmptyOperation(ADD, usersAddress));

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case USER: {
                    parseUser(reader, expectedNs, usersAddress, list);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseUser(final XMLExtendedStreamReader reader, final Namespace expectedNs,
                           final ModelNode usersAddress, final List<ModelNode> list) throws XMLStreamException {


        requireSingleAttribute(reader, Attribute.USERNAME.getLocalName());
        // After double checking the name of the only attribute we can retrieve it.
        final String userName = reader.getAttributeValue(0);
        final ModelNode userAddress = usersAddress.clone().add(USER, userName);
        ModelNode user = Util.getEmptyOperation(ADD, userAddress);

        list.add(user);

        String password = null;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PASSWORD: {
                    password = reader.getElementText();
                    UserResourceDefinition.PASSWORD.parseAndSetParameter(password, user, reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        if (password == null) {
            throw missingRequiredElement(reader, EnumSet.of(Element.PASSWORD));
        }
    }

    private void parseTruststore(final XMLExtendedStreamReader reader, final ModelNode realmAddress,
                                 final List<ModelNode> list) throws XMLStreamException {
        final ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).set(realmAddress).add(ModelDescriptionConstants.AUTHENTICATION, ModelDescriptionConstants.TRUSTSTORE);

        parseKeystore(reader, op);

        list.add(op);
    }

    private void parseAuthorization(final XMLExtendedStreamReader reader, final Namespace expectedNs,
            final ModelNode realmAddress, final List<ModelNode> list) throws XMLStreamException {
        boolean authzFound = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            // Only a single element within the authorization element is currently supported.
            if (authzFound) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case PROPERTIES: {
                    parsePropertiesAuthorization(reader, realmAddress, list);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }

        }
    }

    private void parsePropertiesAuthorization(final XMLExtendedStreamReader reader, final ModelNode realmAddress,
            final List<ModelNode> list) throws XMLStreamException {
        ModelNode addr = realmAddress.clone().add(AUTHORIZATION, PROPERTIES);
        ModelNode properties = Util.getEmptyOperation(ADD, addr);
        list.add(properties);

        String path = null;

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case PATH:
                        path = value;
                        PropertiesAuthorizationResourceDefinition.PATH.parseAndSetParameter(value, properties, reader);
                        break;
                    case RELATIVE_TO: {
                        PropertiesAuthorizationResourceDefinition.RELATIVE_TO.parseAndSetParameter(value, properties, reader);
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

        if (path == null)
            throw missingRequired(reader, Collections.singleton(Attribute.PATH));

        requireNoContent(reader);
    }

    public void writeManagement(final XMLExtendedStreamWriter writer, final ModelNode management, boolean allowInterfaces)
            throws XMLStreamException {
        boolean hasSecurityRealm = management.hasDefined(SECURITY_REALM);
        boolean hasConnection = management.hasDefined(LDAP_CONNECTION);
        boolean hasInterface = allowInterfaces && management.hasDefined(MANAGEMENT_INTERFACE);

        if (!hasSecurityRealm && !hasConnection && !hasInterface) {
            return;
        }

        writer.writeStartElement(Element.MANAGEMENT.getLocalName());
        if (hasSecurityRealm) {
            writeSecurityRealm(writer, management);
        }

        if (hasConnection) {
            writeOutboundConnections(writer, management);
        }

        if (allowInterfaces && hasInterface) {
            writeManagementInterfaces(writer, management);
        }

        writer.writeEndElement();
    }

    private void writeSecurityRealm(XMLExtendedStreamWriter writer, ModelNode management) throws XMLStreamException {
        ModelNode securityRealms = management.get(SECURITY_REALM);
        writer.writeStartElement(Element.SECURITY_REALMS.getLocalName());

        for (Property variable : securityRealms.asPropertyList()) {
            writer.writeStartElement(Element.SECURITY_REALM.getLocalName());
            writeAttribute(writer, Attribute.NAME, variable.getName());

            ModelNode realm = variable.getValue();
            if (realm.hasDefined(SERVER_IDENTITY)) {
                writeServerIdentities(writer, realm);
            }

            if (realm.hasDefined(AUTHENTICATION)) {
                writeAuthentication(writer, realm);
            }

            if (realm.hasDefined(AUTHORIZATION)) {
                writeAuthorization(writer, realm);
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeServerIdentities(XMLExtendedStreamWriter writer, ModelNode realm) throws XMLStreamException {
        writer.writeStartElement(Element.SERVER_IDENTITIES.getLocalName());
        ModelNode serverIdentities = realm.get(SERVER_IDENTITY);
        if (serverIdentities.hasDefined(SSL)) {
            writer.writeStartElement(Element.SSL.getLocalName());
            ModelNode ssl = serverIdentities.get(SSL);
            SSLServerIdentityResourceDefinition.PROTOCOL.marshallAsAttribute(ssl, writer);
            if (ssl.hasDefined(KeystoreAttributes.KEYSTORE_PATH.getName())) {
                writer.writeEmptyElement(Element.KEYSTORE.getLocalName());
                KeystoreAttributes.KEYSTORE_PATH.marshallAsAttribute(ssl, writer);
                KeystoreAttributes.KEYSTORE_RELATIVE_TO.marshallAsAttribute(ssl, writer);
                KeystoreAttributes.KEYSTORE_PASSWORD.marshallAsAttribute(ssl, writer);
            }
            writer.writeEndElement();
        }
        if (serverIdentities.hasDefined(SECRET)) {
            ModelNode secret = serverIdentities.get(SECRET);
            writer.writeEmptyElement(Element.SECRET.getLocalName());
            SecretServerIdentityResourceDefinition.VALUE.marshallAsAttribute(secret, writer);
        }

        writer.writeEndElement();
    }

    private void writeAuthentication(XMLExtendedStreamWriter writer, ModelNode realm) throws XMLStreamException {
        writer.writeStartElement(Element.AUTHENTICATION.getLocalName());
        ModelNode authentication = realm.require(AUTHENTICATION);

        if (authentication.hasDefined(TRUSTSTORE)) {
            ModelNode truststore = authentication.require(TRUSTSTORE);
            writer.writeEmptyElement(Element.TRUSTSTORE.getLocalName());
            KeystoreAttributes.KEYSTORE_PATH.marshallAsAttribute(truststore, writer);
            KeystoreAttributes.KEYSTORE_RELATIVE_TO.marshallAsAttribute(truststore, writer);
            KeystoreAttributes.KEYSTORE_PASSWORD.marshallAsAttribute(truststore, writer);
        }

        if (authentication.hasDefined(JAAS)) {
            ModelNode jaas = authentication.get(JAAS);
            writer.writeStartElement(Element.JAAS.getLocalName());
            JaasAuthenticationResourceDefinition.NAME.marshallAsAttribute(jaas, writer);
            writer.writeEndElement();
        } else if (authentication.hasDefined(LDAP)) {
            ModelNode userLdap = authentication.get(LDAP);
            writer.writeStartElement(Element.LDAP.getLocalName());
            LdapAuthenticationResourceDefinition.CONNECTION.marshallAsAttribute(userLdap, writer);
            LdapAuthenticationResourceDefinition.BASE_DN.marshallAsAttribute(userLdap, writer);
            LdapAuthenticationResourceDefinition.RECURSIVE.marshallAsAttribute(userLdap, writer);
            LdapAuthenticationResourceDefinition.USER_DN.marshallAsAttribute(userLdap, writer);

            if (LdapAuthenticationResourceDefinition.USERNAME_FILTER.isMarshallable(userLdap)) {
                writer.writeEmptyElement(Element.USERNAME_FILTER.getLocalName());
                LdapAuthenticationResourceDefinition.USERNAME_FILTER.marshallAsAttribute(userLdap, writer);
            } else if (LdapAuthenticationResourceDefinition.ADVANCED_FILTER.isMarshallable(userLdap)) {
                writer.writeEmptyElement(Element.ADVANCED_FILTER.getLocalName());
                LdapAuthenticationResourceDefinition.ADVANCED_FILTER.marshallAsAttribute(userLdap, writer);
            }
            writer.writeEndElement();
        } else if (authentication.hasDefined(PROPERTIES)) {
            ModelNode properties = authentication.require(PROPERTIES);
            writer.writeEmptyElement(Element.PROPERTIES.getLocalName());
            PropertiesAuthenticationResourceDefinition.PATH.marshallAsAttribute(properties, writer);
            PropertiesAuthenticationResourceDefinition.RELATIVE_TO.marshallAsAttribute(properties, writer);
            PropertiesAuthenticationResourceDefinition.PLAIN_TEXT.marshallAsAttribute(properties, writer);
        } else if (authentication.hasDefined(USERS)) {
            ModelNode userDomain = authentication.get(USERS);
            ModelNode users = userDomain.hasDefined(USER) ? userDomain.require(USER) : new ModelNode().setEmptyObject();
            writer.writeStartElement(Element.USERS.getLocalName());
            for (Property userProps : users.asPropertyList()) {
                String userName = userProps.getName();
                ModelNode currentUser = userProps.getValue();
                writer.writeStartElement(Element.USER.getLocalName());
                writer.writeAttribute(Attribute.USERNAME.getLocalName(), userName);
                UserResourceDefinition.PASSWORD.marshallAsElement(currentUser, writer);
                writer.writeEndElement();
            }

            writer.writeEndElement();
        }

        writer.writeEndElement();
    }

    private void writeAuthorization(XMLExtendedStreamWriter writer, ModelNode realm) throws XMLStreamException {
        writer.writeStartElement(Element.AUTHORIZATION.getLocalName());
        ModelNode authorization = realm.require(AUTHORIZATION);
        if (authorization.hasDefined(PROPERTIES)) {
            ModelNode properties = authorization.require(PROPERTIES);
            writer.writeEmptyElement(Element.PROPERTIES.getLocalName());
            PropertiesAuthorizationResourceDefinition.PATH.marshallAsAttribute(properties, writer);
            PropertiesAuthorizationResourceDefinition.RELATIVE_TO.marshallAsAttribute(properties, writer);
        }

        writer.writeEndElement();
    }

    private void writeOutboundConnections(XMLExtendedStreamWriter writer, ModelNode management) throws XMLStreamException {

        writer.writeStartElement(Element.OUTBOUND_CONNECTIONS.getLocalName());

        for (Property variable : management.get(LDAP_CONNECTION).asPropertyList()) {
            ModelNode connection = variable.getValue();
            writer.writeEmptyElement(Element.LDAP.getLocalName());
            writer.writeAttribute(Attribute.NAME.getLocalName(), variable.getName());
            LdapConnectionResourceDefinition.URL.marshallAsAttribute(connection, writer);
            LdapConnectionResourceDefinition.SEARCH_DN.marshallAsAttribute(connection, writer);
            LdapConnectionResourceDefinition.SEARCH_CREDENTIAL.marshallAsAttribute(connection, writer);
            LdapConnectionResourceDefinition.INITIAL_CONTEXT_FACTORY.marshallAsAttribute(connection, writer);
        }
        writer.writeEndElement();
    }

    private void writeManagementInterfaces(XMLExtendedStreamWriter writer, ModelNode management) throws XMLStreamException {
        writer.writeStartElement(Element.MANAGEMENT_INTERFACES.getLocalName());
        ModelNode managementInterfaces = management.get(MANAGEMENT_INTERFACE);

        if (managementInterfaces.hasDefined(NATIVE_REMOTING_INTERFACE)) {
            writer.writeEmptyElement(Element.NATIVE_REMOTING_INTERFACE.getLocalName());
        }

        if (managementInterfaces.hasDefined(NATIVE_INTERFACE)) {
            delegate.writeNativeManagementProtocol(writer, managementInterfaces.get(NATIVE_INTERFACE));
        }

        if (managementInterfaces.hasDefined(HTTP_INTERFACE)) {
            delegate.writeHttpManagementProtocol(writer, managementInterfaces.get(HTTP_INTERFACE));
        }

        writer.writeEndElement();
    }

    private static void writeAttribute(XMLExtendedStreamWriter writer, Attribute attribute, String value)
            throws XMLStreamException {
        writer.writeAttribute(attribute.getLocalName(), value);
    }
}
