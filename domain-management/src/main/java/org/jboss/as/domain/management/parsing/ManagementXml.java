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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADVANCED_FILTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.APPLICATION_CLASSIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUDIT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHENTICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHORIZATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CLASSIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONSTRAINT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXCLUDE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP_SEARCH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP_TO_PRINCIPAL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST_SCOPED_ROLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST_SCOPED_ROLES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HTTP_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JAAS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LDAP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LDAP_CONNECTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NATIVE_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NATIVE_REMOTING_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PLAIN_TEXT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRINCIPAL_TO_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLE_MAPPING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECRET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SENSITIVITY_CLASSIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP_SCOPED_ROLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_IDENTITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SSL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TRUSTSTORE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERNAME_FILTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERNAME_IS_DN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERNAME_TO_DN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT_EXPRESSION;
import static org.jboss.as.controller.parsing.ParseUtils.invalidAttributeValue;
import static org.jboss.as.controller.parsing.ParseUtils.isNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.missingOneOf;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequiredElement;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNamespace;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireSingleAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.domain.management.DomainManagementLogger.ROOT_LOGGER;
import static org.jboss.as.domain.management.ModelDescriptionConstants.CACHE;
import static org.jboss.as.domain.management.ModelDescriptionConstants.BY_ACCESS_TIME;
import static org.jboss.as.domain.management.ModelDescriptionConstants.BY_SEARCH_TIME;
import static org.jboss.as.domain.management.ModelDescriptionConstants.DEFAULT_DEFAULT_USER;
import static org.jboss.as.domain.management.ModelDescriptionConstants.DEFAULT_USER;
import static org.jboss.as.domain.management.ModelDescriptionConstants.KEYSTORE_PROVIDER;
import static org.jboss.as.domain.management.ModelDescriptionConstants.JKS;
import static org.jboss.as.domain.management.ModelDescriptionConstants.LOCAL;
import static org.jboss.as.domain.management.ModelDescriptionConstants.PLUG_IN;
import static org.jboss.as.domain.management.ModelDescriptionConstants.PROPERTY;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.domain.management.access.AccessAuthorizationResourceDefinition;
import org.jboss.as.domain.management.access.ApplicationClassificationConfigResourceDefinition;
import org.jboss.as.domain.management.access.ApplicationClassificationTypeResourceDefinition;
import org.jboss.as.domain.management.access.HostScopedRolesResourceDefinition;
import org.jboss.as.domain.management.access.PrincipalResourceDefinition;
import org.jboss.as.domain.management.access.RoleMappingResourceDefinition;
import org.jboss.as.domain.management.access.SensitivityClassificationTypeResourceDefinition;
import org.jboss.as.domain.management.access.SensitivityResourceDefinition;
import org.jboss.as.domain.management.access.ServerGroupScopedRoleResourceDefinition;
import org.jboss.as.domain.management.connections.ldap.LdapConnectionPropertyResourceDefinition;
import org.jboss.as.domain.management.connections.ldap.LdapConnectionResourceDefinition;
import org.jboss.as.domain.management.security.AbstractPlugInAuthResourceDefinition;
import org.jboss.as.domain.management.security.AdvancedUserSearchResourceDefintion;
import org.jboss.as.domain.management.security.BaseLdapGroupSearchResource;
import org.jboss.as.domain.management.security.BaseLdapUserSearchResource;
import org.jboss.as.domain.management.security.GroupToPrincipalResourceDefinition;
import org.jboss.as.domain.management.security.JaasAuthenticationResourceDefinition;
import org.jboss.as.domain.management.security.KeystoreAttributes;
import org.jboss.as.domain.management.security.LdapAuthenticationResourceDefinition;
import org.jboss.as.domain.management.security.LdapAuthorizationResourceDefinition;
import org.jboss.as.domain.management.security.LdapCacheResourceDefinition;
import org.jboss.as.domain.management.security.LocalAuthenticationResourceDefinition;
import org.jboss.as.domain.management.security.PlugInAuthenticationResourceDefinition;
import org.jboss.as.domain.management.security.PrincipalToGroupResourceDefinition;
import org.jboss.as.domain.management.security.PropertiesAuthenticationResourceDefinition;
import org.jboss.as.domain.management.security.PropertiesAuthorizationResourceDefinition;
import org.jboss.as.domain.management.security.PropertyResourceDefinition;
import org.jboss.as.domain.management.security.SSLServerIdentityResourceDefinition;
import org.jboss.as.domain.management.security.SecretServerIdentityResourceDefinition;
import org.jboss.as.domain.management.security.SecurityRealmResourceDefinition;
import org.jboss.as.domain.management.security.UserIsDnResourceDefintion;
import org.jboss.as.domain.management.security.UserResourceDefinition;
import org.jboss.as.domain.management.security.UserSearchResourceDefintion;
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
    public abstract static class Delegate {

        /**
         * Parse {@link Element#SECURITY_REALMS} content.
         * <p>This default implementation does standard parsing; override to disable.</p>
         * @param reader the xml reader
         * @param address the address of the parent resource for any added resources
         * @param expectedNs the expected namespace for any children
         * @param operationsList list to which any operations should be added
         * @throws XMLStreamException
         */
        protected void parseSecurityRealms(XMLExtendedStreamReader reader, ModelNode address, Namespace expectedNs, List<ModelNode> operationsList) throws XMLStreamException {
            ManagementXml.parseSecurityRealms(reader, address, expectedNs, operationsList);
        }

        /**
         * Parse {@link Element#OUTBOUND_CONNECTIONS} content.
         * <p>This default implementation does standard parsing; override to disable.</p>
         *
         * @param reader the xml reader
         * @param address the address of the parent resource for any added resources
         * @param expectedNs the expected namespace for any children
         * @param operationsList list to which any operations should be added
         * @throws XMLStreamException
         */
        protected void parseOutboundConnections(XMLExtendedStreamReader reader, ModelNode address, Namespace expectedNs, List<ModelNode> operationsList) throws XMLStreamException {
            ManagementXml.parseOutboundConnections(reader, address, expectedNs, operationsList);
        }

        /**
         * Parse {@link Element#MANAGEMENT_INTERFACES} content.
         * <p>This default implementation throws {@code UnsupportedOperationException}; override to support.</p>
         *
         * @param reader the xml reader
         * @param address the address of the parent resource for any added resources
         * @param expectedNs the expected namespace for any children
         * @param operationsList list to which any operations should be added
         * @throws XMLStreamException
         */
        protected void parseManagementInterfaces(XMLExtendedStreamReader reader, ModelNode address, Namespace expectedNs, List<ModelNode> operationsList) throws XMLStreamException {
            throw new UnsupportedOperationException();
        }

        /**
         * Parse {@link Element#ACCESS_CONTROL} content.
         * <p>This default implementation throws {@code UnsupportedOperationException}; override to support.</p>
         *
         * @param reader the xml reader
         * @param address the address of the parent resource for any added resources
         * @param expectedNs the expected namespace for any children
         * @param operationsList list to which any operations should be added
         * @throws XMLStreamException
         */
        protected void parseAccessControl(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs,
                                final List<ModelNode> operationsList) throws XMLStreamException {
            // Must override if supported
            throw new UnsupportedOperationException();
        }

        protected void parseAuditLog(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list) throws XMLStreamException {
            //Must override if supported
            throw new UnsupportedOperationException();
        }
        /**
         * Write the {@link Element#NATIVE_INTERFACE} element.
         * <p>This default implementation does standard writing</p>
         *
         * @param writer  the xml writer
         * @param accessAuthorization the access=authorization configuration
         * @throws XMLStreamException
         */
        protected void writeAccessControl(XMLExtendedStreamWriter writer, ModelNode accessAuthorization) throws XMLStreamException {
            ManagementXml.writeAccessControl(writer, accessAuthorization);
        }

        /**
         * Write the {@link Element#NATIVE_INTERFACE} element.
         * <p>This default implementation throws {@code UnsupportedOperationException}; override to support.</p>
         *
         * @param writer  the xml writer
         * @param protocol the interface configuration
         * @throws XMLStreamException
         */
        protected void writeNativeManagementProtocol(XMLExtendedStreamWriter writer, ModelNode protocol) throws XMLStreamException {
            // Must override if supported
            throw new UnsupportedOperationException();
        }

        /**
         * Write the {@link Element#HTTP_INTERFACE} element.
         * <p>This default implementation throws {@code UnsupportedOperationException}; override to support.</p>
         *
         * @param writer  the xml writer
         * @param protocol the interface configuration
         * @throws XMLStreamException
         */
        protected void writeHttpManagementProtocol(XMLExtendedStreamWriter writer, ModelNode protocol) throws XMLStreamException {
            // Must override if supported
            throw new UnsupportedOperationException();
        }

        protected void writeAuditLog(XMLExtendedStreamWriter writer, ModelNode auditLog) throws XMLStreamException {
            // Must override if supported
            throw new UnsupportedOperationException();
        }
    }

    private final Delegate delegate;


    public ManagementXml(Delegate delegate) {
        this.delegate = delegate;
    }

    public void parseManagement(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs,
            final List<ModelNode> list, boolean requireNativeInterface) throws XMLStreamException {
        switch (expectedNs) {
            case DOMAIN_1_0:
            case DOMAIN_1_1:
            case DOMAIN_1_2:
            case DOMAIN_1_3:
            case DOMAIN_1_4:
                parseManagement_1_0(reader, address, expectedNs, list, requireNativeInterface);
                break;
            default:
                parseManagement_1_5(reader, address, expectedNs, list, requireNativeInterface);
        }
    }

    private void parseManagement_1_0(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs,
            final List<ModelNode> list, boolean requireNativeInterface) throws XMLStreamException {
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
                    delegate.parseSecurityRealms(reader, managementAddress, expectedNs, list);

                    break;
                }
                case OUTBOUND_CONNECTIONS: {
                    if (++connectionsCount > 1) {
                        throw unexpectedElement(reader);
                    }
                    delegate.parseOutboundConnections(reader, managementAddress, expectedNs, list);
                    break;
                }
                case MANAGEMENT_INTERFACES: {
                    if (++managementInterfacesCount > 1) {
                        throw unexpectedElement(reader);
                    }

                    delegate.parseManagementInterfaces(reader, managementAddress, expectedNs, list);
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

    private void parseManagement_1_5(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs,
            final List<ModelNode> list, boolean requireNativeInterface) throws XMLStreamException {
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
                    delegate.parseSecurityRealms(reader, managementAddress, expectedNs, list);

                    break;
                }
                case OUTBOUND_CONNECTIONS: {
                    if (++connectionsCount > 1) {
                        throw unexpectedElement(reader);
                    }
                    delegate.parseOutboundConnections(reader, managementAddress, expectedNs, list);
                    break;
                }
                case MANAGEMENT_INTERFACES: {
                    if (++managementInterfacesCount > 1) {
                        throw unexpectedElement(reader);
                    }

                    delegate.parseManagementInterfaces(reader, managementAddress, expectedNs, list);
                    break;
                }
                case AUDIT_LOG: {
                    delegate.parseAuditLog(reader, managementAddress, expectedNs, list);
                    break;
                }
                case ACCESS_CONTROL: {
                    delegate.parseAccessControl(reader, managementAddress, expectedNs, list);
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

    private static void parseOutboundConnections(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs,
                                          final List<ModelNode> list) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case LDAP: {
                    switch (expectedNs) {
                        case DOMAIN_1_0:
                        case DOMAIN_1_1:
                        case DOMAIN_1_2:
                        case DOMAIN_1_3:
                            parseLdapConnection_1_0(reader, address, list);
                            break;
                        case DOMAIN_1_4:
                        case DOMAIN_1_5:
                            parseLdapConnection_1_4(reader, address, list);
                            break;
                        default:
                            parseLdapConnection_2_0(reader, address, expectedNs, list);
                            break;
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private static void parseLdapConnection_1_0(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list)
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

    private static void parseLdapConnection_1_4(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list)
            throws XMLStreamException {

        final ModelNode add = new ModelNode();
        add.get(OP).set(ADD);

        list.add(add);

        Set<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.URL);
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
                    case SECURITY_REALM: {
                        LdapConnectionResourceDefinition.SECURITY_REALM.parseAndSetParameter(value, add, reader);
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

    private static void parseLdapConnection_2_0(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list)
            throws XMLStreamException {

        final ModelNode add = new ModelNode();
        add.get(OP).set(ADD);

        list.add(add);

        ModelNode connectionAddress = null;

        Set<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.URL);
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
                        connectionAddress = address.clone().add(LDAP_CONNECTION, value);
                        add.get(OP_ADDR).set(connectionAddress);
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
                    case SECURITY_REALM: {
                        LdapConnectionResourceDefinition.SECURITY_REALM.parseAndSetParameter(value, add, reader);
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

        boolean propertiesFound = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);

            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PROPERTIES: {
                    if (propertiesFound) {
                        throw unexpectedElement(reader);
                    }
                    propertiesFound = true;
                    parseLdapConnectionProperties(reader, connectionAddress, expectedNs, list);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private static void parseLdapConnectionProperties(final XMLExtendedStreamReader reader, final ModelNode address,
            final Namespace expectedNs, final List<ModelNode> list) throws XMLStreamException {

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);

            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PROPERTY: {
                    Set<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.VALUE);
                    final ModelNode add = new ModelNode();
                    add.get(OP).set(ADD);

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
                                    add.get(OP_ADDR).set(address.clone()).add(PROPERTY, value);
                                    break;
                                }
                                case VALUE: {
                                    LdapConnectionPropertyResourceDefinition.VALUE.parseAndSetParameter(value, add, reader);
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

                    list.add(add);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private static void parseSecurityRealms(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list)
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
                        case DOMAIN_1_1:
                        case DOMAIN_1_2:
                            parseSecurityRealm_1_1(reader, address, expectedNs, list);
                            break;
                        case DOMAIN_1_3:
                            parseSecurityRealm_1_3(reader, address, expectedNs, list);
                            break;
                        default:
                            parseSecurityRealm_1_4(reader, address, expectedNs, list);
                            break;
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private static void parseSecurityRealm_1_0(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list)
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

    private static void parseSecurityRealm_1_1(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list)
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
                    parseAuthorization_1_1(reader, expectedNs, add, list);
                    break;
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private static void parseSecurityRealm_1_3(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list)
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
                case PLUG_INS:
                    parsePlugIns(reader, expectedNs, realmAddress, list);
                    break;
                case SERVER_IDENTITIES:
                    parseServerIdentities(reader, expectedNs, realmAddress, list);
                    break;
                case AUTHENTICATION: {
                    parseAuthentication_1_3(reader, expectedNs, realmAddress, list);
                    break;
                }
                case AUTHORIZATION:
                    switch (expectedNs) {
                        case DOMAIN_1_3:
                        case DOMAIN_1_4:
                            parseAuthorization_1_3(reader, expectedNs, realmAddress, list);
                            break;
                        default:
                            parseAuthorization_1_5(reader, expectedNs, add, list);
                    }
                    break;
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private static void parseSecurityRealm_1_4(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list)
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
                case PLUG_INS:
                    parsePlugIns(reader, expectedNs, realmAddress, list);
                    break;
                case SERVER_IDENTITIES:
                    parseServerIdentities(reader, expectedNs, realmAddress, list);
                    break;
                case AUTHENTICATION: {
                    parseAuthentication_1_3(reader, expectedNs, realmAddress, list);
                    break;
                }
                case AUTHORIZATION:
                    switch (expectedNs) {
                        case DOMAIN_1_4:
                            parseAuthorization_1_3(reader, expectedNs, realmAddress, list);
                            break;
                        default:
                            parseAuthorization_1_5(reader, expectedNs, add, list);
                    }
                    break;
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }


    private static void parsePlugIns(final XMLExtendedStreamReader reader, final Namespace expectedNs, final ModelNode realmAddress, final List<ModelNode> list)
            throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);

            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PLUG_IN: {
                    ModelNode plugIn = new ModelNode();
                    plugIn.get(OP).set(ADD);
                    String moduleValue = readStringAttributeElement(reader, Attribute.MODULE.getLocalName());
                    final ModelNode newAddress = realmAddress.clone();
                    newAddress.add(PLUG_IN, moduleValue);
                    plugIn.get(OP_ADDR).set(newAddress);

                    list.add(plugIn);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private static void parseServerIdentities(final XMLExtendedStreamReader reader, final Namespace expectedNs, final ModelNode realmAddress, final List<ModelNode> list)
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

    private static void parseSecret(final XMLExtendedStreamReader reader, final ModelNode realmAddress, final List<ModelNode> list)
            throws XMLStreamException {

        ModelNode secret = new ModelNode();
        secret.get(OP).set(ADD);
        secret.get(OP_ADDR).set(realmAddress).add(SERVER_IDENTITY, SECRET);
        String secretValue = readStringAttributeElement(reader, Attribute.VALUE.getLocalName());
        SecretServerIdentityResourceDefinition.VALUE.parseAndSetParameter(secretValue, secret, reader);

        list.add(secret);
    }

    private static void parseSSL(final XMLExtendedStreamReader reader, final Namespace expectedNs, final ModelNode realmAddress, final List<ModelNode> list) throws XMLStreamException {

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
                    switch (expectedNs) {
                        case DOMAIN_1_0:
                        case DOMAIN_1_1:
                        case DOMAIN_1_2:
                            parseKeystore_1_0(reader, ssl);
                            break;
                        case DOMAIN_1_3:
                        case DOMAIN_1_4:
                        case DOMAIN_1_5:
                        case DOMAIN_2_0:
                            parseKeystore_1_3(reader, ssl, true);
                            break;
                        default:
                            parseKeystore_2_1(reader, ssl, true);
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

    }

    private static void parseKeystore_1_0(final XMLExtendedStreamReader reader, final ModelNode addOperation)
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

    private static void parseKeystore_1_3(final XMLExtendedStreamReader reader, final ModelNode addOperation, final boolean extended)
            throws XMLStreamException {
        Set<Attribute> required = EnumSet.of(Attribute.PATH, Attribute.KEYSTORE_PASSWORD);
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
                        // TODO - Support for this attribute can later be removed, would suggest removing at the
                        //        start of AS 8 development.
                        ROOT_LOGGER.passwordAttributeDeprecated();
                        required.remove(Attribute.KEYSTORE_PASSWORD);
                        KeystoreAttributes.KEYSTORE_PASSWORD.parseAndSetParameter(value, addOperation, reader);
                        break;
                    }
                    case KEYSTORE_PASSWORD: {
                        KeystoreAttributes.KEYSTORE_PASSWORD.parseAndSetParameter(value, addOperation, reader);
                        break;
                    }
                    case RELATIVE_TO: {
                        KeystoreAttributes.KEYSTORE_RELATIVE_TO.parseAndSetParameter(value, addOperation, reader);
                        break;
                    }
                    /*
                     * The 'extended' attributes when a true keystore and not just a keystore acting as a truststore.
                     */
                    case ALIAS: {
                        if (extended) {
                            KeystoreAttributes.ALIAS.parseAndSetParameter(value, addOperation, reader);
                        } else {
                            throw unexpectedAttribute(reader, i);
                        }
                        break;
                    }
                    case KEY_PASSWORD: {
                        if (extended) {
                            KeystoreAttributes.KEY_PASSWORD.parseAndSetParameter(value, addOperation, reader);
                        } else {
                            throw unexpectedAttribute(reader, i);
                        }
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

    private static void parseKeystore_2_1(final XMLExtendedStreamReader reader, final ModelNode addOperation, final boolean extended)
            throws XMLStreamException {
        boolean pathSet = false;
        boolean keystorePasswordSet = false;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case PROVIDER:
                        KeystoreAttributes.KEYSTORE_PROVIDER.parseAndSetParameter(value, addOperation, reader);
                        break;
                    case PATH:
                        KeystoreAttributes.KEYSTORE_PATH.parseAndSetParameter(value, addOperation, reader);
                        pathSet = true;
                        break;
                    case KEYSTORE_PASSWORD: {
                        KeystoreAttributes.KEYSTORE_PASSWORD.parseAndSetParameter(value, addOperation, reader);
                        keystorePasswordSet = true;
                        break;
                    }
                    case RELATIVE_TO: {
                        KeystoreAttributes.KEYSTORE_RELATIVE_TO.parseAndSetParameter(value, addOperation, reader);
                        break;
                    }
                    /*
                     * The 'extended' attributes when a true keystore and not just a keystore acting as a truststore.
                     */
                    case ALIAS: {
                        if (extended) {
                            KeystoreAttributes.ALIAS.parseAndSetParameter(value, addOperation, reader);
                        } else {
                            throw unexpectedAttribute(reader, i);
                        }
                        break;
                    }
                    case KEY_PASSWORD: {
                        if (extended) {
                            KeystoreAttributes.KEY_PASSWORD.parseAndSetParameter(value, addOperation, reader);
                        } else {
                            throw unexpectedAttribute(reader, i);
                        }
                        break;
                    }

                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

        /*
         * The only mandatory attribute now is the KEYSTORE_PASSWORD.
         */
        if (keystorePasswordSet == false) {
            throw missingRequired(reader, EnumSet.of(Attribute.KEYSTORE_PASSWORD));
        }

        requireNoContent(reader);
    }

    private static void parseAuthentication_1_0(final XMLExtendedStreamReader reader, final Namespace expectedNs, final ModelNode realmAddress, final List<ModelNode> list)
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
        addLegacyLocalAuthentication(realmAddress, list);
    }

    private static void parseAuthentication_1_1(final XMLExtendedStreamReader reader, final Namespace expectedNs,
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
                    parseJaasAuthentication(reader, realmAddress, list);
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
                    parseTruststore(reader, expectedNs, realmAddress, list);
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
        addLegacyLocalAuthentication(realmAddress, list);
    }

    private static void parseAuthentication_1_3(final XMLExtendedStreamReader reader, final Namespace expectedNs,
            final ModelNode realmAddress, final List<ModelNode> list) throws XMLStreamException {

        // Only one truststore can be defined.
        boolean trustStoreFound = false;
        // Only one local can be defined.
        boolean localFound = false;
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
                    parseJaasAuthentication(reader, realmAddress, list);
                    usernamePasswordFound = true;
                    break;
                }
                case LDAP: {
                    if (usernamePasswordFound) {
                        throw unexpectedElement(reader);
                    }
                    // This method is specific to version 1.3 of the schema and beyond - no need to
                    // consider namespaces before this point.
                    switch (expectedNs) {
                        case DOMAIN_1_3:
                            parseLdapAuthentication_1_1(reader, expectedNs, realmAddress, list);
                            break;
                        case DOMAIN_1_4:
                        case DOMAIN_1_5:
                            parseLdapAuthentication_1_4(reader, expectedNs, realmAddress, list);
                            break;
                        default:
                            parseLdapAuthentication_2_0(reader, expectedNs, realmAddress, list);
                            break;
                    }
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
                    parseTruststore(reader, expectedNs, realmAddress, list);
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
                case PLUG_IN: {
                    if (usernamePasswordFound) {
                        throw unexpectedElement(reader);
                    }
                    ModelNode parentAddress = realmAddress.clone().add(AUTHENTICATION);
                    parsePlugIn_Authentication(reader, expectedNs, parentAddress, list);
                    usernamePasswordFound = true;
                    break;
                }
                case LOCAL: {
                    if (localFound) {
                        throw unexpectedElement(reader);
                    }
                    parseLocalAuthentication(reader, expectedNs, realmAddress, list);
                    localFound = true;
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private static void parseJaasAuthentication(final XMLExtendedStreamReader reader,
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

    private static void parseLdapAuthentication_1_0(final XMLExtendedStreamReader reader, final ModelNode realmAddress, final List<ModelNode> list)
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

    private static void parseLdapAuthentication_1_1(final XMLExtendedStreamReader reader, final Namespace expectedNs,
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


    private static void parseLdapAuthentication_1_4(final XMLExtendedStreamReader reader, final Namespace expectedNs,
            final ModelNode realmAddress, final List<ModelNode> list) throws XMLStreamException {
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
                    case ALLOW_EMPTY_PASSWORDS: {
                        LdapAuthenticationResourceDefinition.ALLOW_EMPTY_PASSWORDS.parseAndSetParameter(value, ldapAuthentication, reader);
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
                    LdapAuthenticationResourceDefinition.ADVANCED_FILTER.parseAndSetParameter(filter, ldapAuthentication,
                            reader);
                    break;
                case USERNAME_FILTER: {
                    String usernameAttr = readStringAttributeElement(reader, Attribute.ATTRIBUTE.getLocalName());
                    LdapAuthenticationResourceDefinition.USERNAME_FILTER.parseAndSetParameter(usernameAttr, ldapAuthentication,
                            reader);
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

    private static void parseLdapAuthentication_2_0(final XMLExtendedStreamReader reader, final Namespace expectedNs,
            final ModelNode realmAddress, final List<ModelNode> list) throws XMLStreamException {
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
                    case ALLOW_EMPTY_PASSWORDS: {
                        LdapAuthenticationResourceDefinition.ALLOW_EMPTY_PASSWORDS.parseAndSetParameter(value, ldapAuthentication, reader);
                        break;
                    }
                    case USERNAME_LOAD: {
                        LdapAuthenticationResourceDefinition.USERNAME_LOAD.parseAndSetParameter(value, ldapAuthentication, reader);
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

        ModelNode addLdapCache = null;
        boolean choiceFound = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if (choiceFound) {
                throw unexpectedElement(reader);
            }
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case CACHE:
                    if (addLdapCache != null) {
                        throw unexpectedElement(reader);
                    }
                    addLdapCache = parseLdapCache(reader);
                    break;
                case ADVANCED_FILTER:
                    choiceFound = true;
                    String filter = readStringAttributeElement(reader, Attribute.FILTER.getLocalName());
                    LdapAuthenticationResourceDefinition.ADVANCED_FILTER.parseAndSetParameter(filter, ldapAuthentication,
                            reader);
                    break;
                case USERNAME_FILTER: {
                    choiceFound = true;
                    String usernameAttr = readStringAttributeElement(reader, Attribute.ATTRIBUTE.getLocalName());
                    LdapAuthenticationResourceDefinition.USERNAME_FILTER.parseAndSetParameter(usernameAttr, ldapAuthentication,
                            reader);
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

        if (addLdapCache != null) {
            correctCacheAddress(ldapAuthentication, addLdapCache);
            list.add(addLdapCache);
        }
    }

    private static ModelNode parseLdapCache(final XMLExtendedStreamReader reader) throws XMLStreamException {
        ModelNode addr = new ModelNode();
        ModelNode addCacheOp = Util.getEmptyOperation(ADD, addr);

        String type = BY_SEARCH_TIME;

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case TYPE: {
                        if (BY_ACCESS_TIME.equals(value) || BY_SEARCH_TIME.equals(value)) {
                            type = value;
                        } else {
                            throw invalidAttributeValue(reader, i);
                        }
                        break;
                    }
                    case EVICTION_TIME: {
                        LdapCacheResourceDefinition.EVICTION_TIME.parseAndSetParameter(value, addCacheOp, reader);
                        break;
                    }
                    case CACHE_FAILURES: {
                        LdapCacheResourceDefinition.CACHE_FAILURES.parseAndSetParameter(value, addCacheOp, reader);
                        break;
                    }
                    case MAX_CACHE_SIZE: {
                        LdapCacheResourceDefinition.MAX_CACHE_SIZE.parseAndSetParameter(value, addCacheOp, reader);
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

        requireNoContent(reader);
        addCacheOp.get(OP_ADDR).add(CACHE, type);
        return addCacheOp;
    }

    private static void correctCacheAddress(ModelNode parentAdd, ModelNode cacheAdd) {
        List<Property> addressList = cacheAdd.get(OP_ADDR).asPropertyList();
        ModelNode cacheAddress = parentAdd.get(OP_ADDR).clone();
        for (Property current : addressList) {
            cacheAddress.add(current.getName(), current.getValue().asString());
        }

        cacheAdd.get(OP_ADDR).set(cacheAddress);
    }

    private static void addLegacyLocalAuthentication(final ModelNode realmAddress, final List<ModelNode> list) {
        /*
         * Before version 1.3 of the domain schema there was no configuration for the local mechanism, however it was always
         * enabled - this adds an arbitrary add local op to recreate this behaviour in the older schema versions.
         */
        ModelNode addr = realmAddress.clone().add(AUTHENTICATION, LOCAL);
        ModelNode local = Util.getEmptyOperation(ADD, addr);
        local.get(DEFAULT_USER).set(DEFAULT_DEFAULT_USER);
        list.add(local);
    }

    private static void parseLocalAuthentication(final XMLExtendedStreamReader reader, final Namespace expectedNs,
            final ModelNode realmAddress, final List<ModelNode> list) throws XMLStreamException {
        ModelNode addr = realmAddress.clone().add(AUTHENTICATION, LOCAL);
        ModelNode local = Util.getEmptyOperation(ADD, addr);
        list.add(local);

        final int count = reader.getAttributeCount();
        Set<Attribute> attributesFound = new HashSet<Attribute>(count);

        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                if (attributesFound.contains(attribute)) {
                    throw unexpectedAttribute(reader, i);
                }
                attributesFound.add(attribute);

                switch (attribute) {
                    case DEFAULT_USER:
                        LocalAuthenticationResourceDefinition.DEFAULT_USER.parseAndSetParameter(value, local, reader);
                        break;
                    case ALLOWED_USERS:
                        LocalAuthenticationResourceDefinition.ALLOWED_USERS.parseAndSetParameter(value, local, reader);
                        break;
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }
        // All attributes are optional.

        requireNoContent(reader);
    }

    private static void parsePropertiesAuthentication_1_0(final XMLExtendedStreamReader reader,
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

    private static void parsePropertiesAuthentication_1_1(final XMLExtendedStreamReader reader,
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
    private static void parseUsersAuthentication(final XMLExtendedStreamReader reader, final Namespace expectedNs,
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

    private static void parseUser(final XMLExtendedStreamReader reader, final Namespace expectedNs,
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

    private static void parseTruststore(final XMLExtendedStreamReader reader, final Namespace expectedNs, final ModelNode realmAddress,
                                 final List<ModelNode> list) throws XMLStreamException {
        final ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).set(realmAddress).add(ModelDescriptionConstants.AUTHENTICATION, ModelDescriptionConstants.TRUSTSTORE);

        switch (expectedNs) {
            case DOMAIN_1_0:
            case DOMAIN_1_1:
            case DOMAIN_1_2:
                parseKeystore_1_0(reader, op);
                break;
            case DOMAIN_1_3:
            case DOMAIN_1_4:
            case DOMAIN_1_5:
            case DOMAIN_2_0:
                parseKeystore_1_3(reader, op, false);
                break;
            default:
                parseKeystore_2_1(reader, op, false);
        }

        list.add(op);
    }

    private static void parseAuthorization_1_1(final XMLExtendedStreamReader reader, final Namespace expectedNs,
            final ModelNode realmAdd, final List<ModelNode> list) throws XMLStreamException {
        ModelNode realmAddress = realmAdd.get(OP_ADDR);

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
                    authzFound = true;
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }

        }
    }

    private static void parseAuthorization_1_3(final XMLExtendedStreamReader reader, final Namespace expectedNs,
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
                    authzFound = true;
                    break;
                }
                case PLUG_IN: {
                    ModelNode parentAddress = realmAddress.clone().add(AUTHORIZATION);
                    parsePlugIn_Authorization(reader, expectedNs, parentAddress, list);
                    authzFound = true;
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }

        }
    }

    private static void parseAuthorization_1_5(final XMLExtendedStreamReader reader, final Namespace expectedNs,
            final ModelNode realmAdd, final List<ModelNode> list) throws XMLStreamException {
        ModelNode realmAddress = realmAdd.get(OP_ADDR);

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case MAP_GROUPS_TO_ROLES:
                        SecurityRealmResourceDefinition.MAP_GROUPS_TO_ROLES.parseAndSetParameter(value, realmAdd, reader);
                        break;
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

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
                    authzFound = true;
                    break;
                }
                case PLUG_IN: {
                    ModelNode parentAddress = realmAddress.clone().add(AUTHORIZATION);
                    parsePlugIn_Authorization(reader, expectedNs, parentAddress, list);
                    authzFound = true;
                    break;
                }
                case LDAP: {
                    parseLdapAuthorization_1_5(reader, expectedNs, realmAddress, list);
                    authzFound = true;
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }

        }
    }



    private static void parseLdapAuthorization_1_5(final XMLExtendedStreamReader reader, final Namespace expectedNs,
            final ModelNode realmAddress, final List<ModelNode> list) throws XMLStreamException {
        ModelNode addr = realmAddress.clone().add(AUTHORIZATION, LDAP);
        ModelNode ldapAuthorization = Util.getEmptyOperation(ADD, addr);

        list.add(ldapAuthorization);

        Set<Attribute> required = EnumSet.of(Attribute.CONNECTION);
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
                        LdapAuthorizationResourceDefinition.CONNECTION.parseAndSetParameter(value, ldapAuthorization, reader);
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

        if (required.isEmpty() == false) {
            throw missingRequired(reader, required);
        }

        Set<Element> foundElements = new HashSet<Element>();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            if (foundElements.add(element) == false) {
                throw unexpectedElement(reader); // Only one of each allowed.
            }
            switch (element) {
                case USERNAME_TO_DN: {
                    switch (expectedNs) {
                        case DOMAIN_1_5: // This method not called for earlier schemas.
                            parseUsernameToDn_1_5(reader, expectedNs, addr, list);
                            break;
                        default:
                            parseUsernameToDn_2_0(reader, expectedNs, addr, list);
                            break;
                    }
                    break;
                }
                case GROUP_SEARCH: {
                    switch (expectedNs) {
                        case DOMAIN_1_5:
                            parseGroupSearch_1_5(reader, expectedNs, addr, list);
                            break;
                        default:
                            parseGroupSearch_2_0(reader, expectedNs, addr, list);
                            break;
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private static void parseUsernameToDn_1_5(final XMLExtendedStreamReader reader, final Namespace expectedNs,
            final ModelNode ldapAddress, final List<ModelNode> list) throws XMLStreamException {
        // Add operation to be defined by parsing a child element, however the attribute FORCE is common here.
        final ModelNode childAdd = new ModelNode();
        childAdd.get(OP).set(ADD);

        boolean forceFound = false;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case FORCE:
                        forceFound = true;
                        BaseLdapUserSearchResource.FORCE.parseAndSetParameter(value, childAdd, reader);
                        break;
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

        if (forceFound == false) {
            throw missingRequired(reader, Collections.singleton(Attribute.FORCE));
        }

        boolean filterFound = false;
        ModelNode address = ldapAddress.clone().add(USERNAME_TO_DN);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);

            final Element element = Element.forName(reader.getLocalName());
            if (filterFound) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case USERNAME_IS_DN:
                    filterFound = true;
                    parseUsernameIsDn(reader, address, childAdd);
                    break;
                case USERNAME_FILTER:
                    filterFound = true;
                    parseUsernameFilter(reader, address, childAdd);
                    break;
                case ADVANCED_FILTER:
                    filterFound = true;
                    parseAdvancedFilter(reader, address, childAdd);
                    break;
                default: {
                    throw unexpectedElement(reader);
                }
            }

        }

        if (filterFound == false) {
            throw missingOneOf(reader, EnumSet.of(Element.USERNAME_IS_DN, Element.USERNAME_FILTER, Element.ADVANCED_FILTER));
        }

        list.add(childAdd);
    }

    private static void parseUsernameToDn_2_0(final XMLExtendedStreamReader reader, final Namespace expectedNs,
            final ModelNode ldapAddress, final List<ModelNode> list) throws XMLStreamException {
        // Add operation to be defined by parsing a child element, however the attribute FORCE is common here.
        final ModelNode childAdd = new ModelNode();
        childAdd.get(OP).set(ADD);

        boolean forceFound = false;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case FORCE:
                        forceFound = true;
                        BaseLdapUserSearchResource.FORCE.parseAndSetParameter(value, childAdd, reader);
                        break;
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

        if (forceFound == false) {
            throw missingRequired(reader, Collections.singleton(Attribute.FORCE));
        }

        boolean filterFound = false;
        ModelNode cacheAdd = null;
        ModelNode address = ldapAddress.clone().add(USERNAME_TO_DN);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);

            final Element element = Element.forName(reader.getLocalName());
            if (filterFound) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case CACHE:
                    if (cacheAdd != null) {
                        throw unexpectedElement(reader);
                    }
                    cacheAdd = parseLdapCache(reader);
                    break;
                case USERNAME_IS_DN:
                    filterFound = true;
                    parseUsernameIsDn(reader, address, childAdd);
                    break;
                case USERNAME_FILTER:
                    filterFound = true;
                    parseUsernameFilter(reader, address, childAdd);
                    break;
                case ADVANCED_FILTER:
                    filterFound = true;
                    parseAdvancedFilter(reader, address, childAdd);
                    break;
                default: {
                    throw unexpectedElement(reader);
                }
            }

        }

        if (filterFound == false) {
            throw missingOneOf(reader, EnumSet.of(Element.USERNAME_IS_DN, Element.USERNAME_FILTER, Element.ADVANCED_FILTER));
        }

        list.add(childAdd);
        if (cacheAdd != null) {
            correctCacheAddress(childAdd, cacheAdd);
            list.add(cacheAdd);
        }
    }

    private static void parseUsernameIsDn(final XMLExtendedStreamReader reader,
            final ModelNode parentAddress, final ModelNode addOp) throws XMLStreamException {
        requireNoAttributes(reader);
        requireNoContent(reader);

        addOp.get(OP_ADDR).set(parentAddress.clone().add(USERNAME_IS_DN));
    }

    private static void parseUsernameFilter(final XMLExtendedStreamReader reader, final ModelNode parentAddress,
            final ModelNode addOp) throws XMLStreamException {

        boolean baseDnFound = false;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case BASE_DN: {
                        baseDnFound = true;
                        UserSearchResourceDefintion.BASE_DN.parseAndSetParameter(value, addOp, reader);
                        break;
                    }
                    case RECURSIVE: {
                        UserSearchResourceDefintion.RECURSIVE.parseAndSetParameter(value, addOp, reader);
                        break;
                    }
                    case USER_DN_ATTRIBUTE: {
                        UserSearchResourceDefintion.USER_DN_ATTRIBUTE.parseAndSetParameter(value, addOp, reader);
                        break;
                    }
                    case ATTRIBUTE: {
                        UserSearchResourceDefintion.ATTRIBUTE.parseAndSetParameter(value, addOp, reader);
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

        if (baseDnFound == false) {
            throw missingRequired(reader, Collections.singleton(Attribute.BASE_DN));
        }

        requireNoContent(reader);

        addOp.get(OP_ADDR).set(parentAddress.clone().add(USERNAME_FILTER));
    }

    private static void parseAdvancedFilter(final XMLExtendedStreamReader reader, final ModelNode parentAddress,
            final ModelNode addOp) throws XMLStreamException {

        boolean baseDnFound = false;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case BASE_DN: {
                        baseDnFound = true;
                        AdvancedUserSearchResourceDefintion.BASE_DN.parseAndSetParameter(value, addOp, reader);
                        break;
                    }
                    case RECURSIVE: {
                        AdvancedUserSearchResourceDefintion.RECURSIVE.parseAndSetParameter(value, addOp, reader);
                        break;
                    }
                    case USER_DN_ATTRIBUTE: {
                        UserSearchResourceDefintion.USER_DN_ATTRIBUTE.parseAndSetParameter(value, addOp, reader);
                        break;
                    }
                    case FILTER: {
                        AdvancedUserSearchResourceDefintion.FILTER.parseAndSetParameter(value, addOp, reader);
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

        if (baseDnFound == false) {
            throw missingRequired(reader, Collections.singleton(Attribute.BASE_DN));
        }

        requireNoContent(reader);

        addOp.get(OP_ADDR).set(parentAddress.clone().add(ADVANCED_FILTER));
    }

    private static void parseGroupSearch_1_5(final XMLExtendedStreamReader reader, final Namespace expectedNs,
            final ModelNode ldapAddress, final List<ModelNode> list) throws XMLStreamException {
        // Add operation to be defined by parsing a child element, however the attribute FORCE is common here.
        final ModelNode childAdd = new ModelNode();
        childAdd.get(OP).set(ADD);

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case GROUP_NAME:
                        BaseLdapGroupSearchResource.GROUP_NAME.parseAndSetParameter(value, childAdd, reader);
                        break;
                    case ITERATIVE:
                        BaseLdapGroupSearchResource.ITERATIVE.parseAndSetParameter(value, childAdd, reader);
                        break;
                    case GROUP_DN_ATTRIBUTE:
                        BaseLdapGroupSearchResource.GROUP_DN_ATTRIBUTE.parseAndSetParameter(value, childAdd, reader);
                        break;
                    case GROUP_NAME_ATTRIBUTE:
                        BaseLdapGroupSearchResource.GROUP_NAME_ATTRIBUTE.parseAndSetParameter(value, childAdd, reader);
                        break;
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

        boolean filterFound = false;
        ModelNode address = ldapAddress.clone().add(GROUP_SEARCH);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);

            final Element element = Element.forName(reader.getLocalName());
            if (filterFound) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case GROUP_TO_PRINCIPAL:
                    filterFound = true;
                    parseGroupToPrincipal(reader, expectedNs, address, childAdd);
                    break;
                case PRINCIPAL_TO_GROUP:
                    filterFound = true;
                    parsePrincipalToGroup(reader, expectedNs, address, childAdd);
                    break;
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        if (filterFound == false) {
            throw missingOneOf(reader, EnumSet.of(Element.GROUP_TO_PRINCIPAL, Element.PRINCIPAL_TO_GROUP));
        }

        list.add(childAdd);
    }

    private static void parseGroupSearch_2_0(final XMLExtendedStreamReader reader, final Namespace expectedNs,
            final ModelNode ldapAddress, final List<ModelNode> list) throws XMLStreamException {
        // Add operation to be defined by parsing a child element, however the attribute FORCE is common here.
        final ModelNode childAdd = new ModelNode();
        childAdd.get(OP).set(ADD);

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case GROUP_NAME:
                        BaseLdapGroupSearchResource.GROUP_NAME.parseAndSetParameter(value, childAdd, reader);
                        break;
                    case ITERATIVE:
                        BaseLdapGroupSearchResource.ITERATIVE.parseAndSetParameter(value, childAdd, reader);
                        break;
                    case GROUP_DN_ATTRIBUTE:
                        BaseLdapGroupSearchResource.GROUP_DN_ATTRIBUTE.parseAndSetParameter(value, childAdd, reader);
                        break;
                    case GROUP_NAME_ATTRIBUTE:
                        BaseLdapGroupSearchResource.GROUP_NAME_ATTRIBUTE.parseAndSetParameter(value, childAdd, reader);
                        break;
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

        boolean filterFound = false;
        ModelNode cacheAdd = null;
        ModelNode address = ldapAddress.clone().add(GROUP_SEARCH);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);

            final Element element = Element.forName(reader.getLocalName());
            if (filterFound) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case CACHE:
                    if (cacheAdd != null) {
                        throw unexpectedElement(reader);
                    }
                    cacheAdd = parseLdapCache(reader);
                    break;
                case GROUP_TO_PRINCIPAL:
                    filterFound = true;
                    parseGroupToPrincipal(reader, expectedNs, address, childAdd);
                    break;
                case PRINCIPAL_TO_GROUP:
                    filterFound = true;
                    parsePrincipalToGroup(reader, expectedNs, address, childAdd);
                    break;
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        if (filterFound == false) {
            throw missingOneOf(reader, EnumSet.of(Element.GROUP_TO_PRINCIPAL, Element.PRINCIPAL_TO_GROUP));
        }

        list.add(childAdd);
        if (cacheAdd != null) {
            correctCacheAddress(childAdd, cacheAdd);
            list.add(cacheAdd);
        }
    }

    private static void parseGroupToPrincipal(final XMLExtendedStreamReader reader, final Namespace expectedNs, final ModelNode parentAddress,
            final ModelNode addOp) throws XMLStreamException {
        boolean baseDnFound = false;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case BASE_DN: {
                        baseDnFound = true;
                        GroupToPrincipalResourceDefinition.BASE_DN.parseAndSetParameter(value, addOp, reader);
                        break;
                    }
                    case RECURSIVE: {
                        GroupToPrincipalResourceDefinition.RECURSIVE.parseAndSetParameter(value, addOp, reader);
                        break;
                    }
                    case SEARCH_BY:
                        GroupToPrincipalResourceDefinition.SEARCH_BY.parseAndSetParameter(value, addOp, reader);
                        break;
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

        if (baseDnFound == false) {
            throw missingRequired(reader, Collections.singleton(Attribute.BASE_DN));
        }

        boolean elementFound = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);

            final Element element = Element.forName(reader.getLocalName());
            if (elementFound) {
                throw unexpectedElement(reader);
            }
            elementFound = true;
            switch (element) {
                case MEMBERSHIP_FILTER:
                    parseMembershipFilter(reader, addOp);
                    break;
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        addOp.get(OP_ADDR).set(parentAddress.clone().add(GROUP_TO_PRINCIPAL));
    }

    private static void parseMembershipFilter(final XMLExtendedStreamReader reader,
            final ModelNode addOp) throws XMLStreamException {
        boolean principalAttribute = false;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case PRINCIPAL_ATTRIBUTE: {
                        principalAttribute = true;
                        GroupToPrincipalResourceDefinition.PRINCIPAL_ATTRIBUTE.parseAndSetParameter(value, addOp, reader);
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

        if (principalAttribute == false) {
            throw missingRequired(reader, Collections.singleton(Attribute.PRINCIPAL_ATTRIBUTE));
        }

        requireNoContent(reader);
    }

    private static void parsePrincipalToGroup(final XMLExtendedStreamReader reader, final Namespace expectedNs, final ModelNode parentAddress,
            final ModelNode addOp) throws XMLStreamException {

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case GROUP_ATTRIBUTE: {
                        PrincipalToGroupResourceDefinition.GROUP_ATTRIBUTE.parseAndSetParameter(value, addOp, reader);
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

        requireNoContent(reader);

        addOp.get(OP_ADDR).set(parentAddress.clone().add(PRINCIPAL_TO_GROUP));
    }

    private static void parsePropertiesAuthorization(final XMLExtendedStreamReader reader, final ModelNode realmAddress,
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

    private static void parsePlugIn_Authentication(final XMLExtendedStreamReader reader, final Namespace expectedNs,
            final ModelNode parentAddress, final List<ModelNode> list) throws XMLStreamException {
        ModelNode addr = parentAddress.clone().add(PLUG_IN);
        ModelNode plugIn = Util.getEmptyOperation(ADD, addr);
        list.add(plugIn);

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
                        PlugInAuthenticationResourceDefinition.NAME.parseAndSetParameter(value, plugIn, reader);
                        nameFound = true;
                        break;
                    case MECHANISM: {
                        PlugInAuthenticationResourceDefinition.MECHANISM.parseAndSetParameter(value, plugIn, reader);
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

        if (nameFound == false) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PROPERTIES: {
                    while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                        requireNamespace(reader, expectedNs);
                        final Element propertyElement = Element.forName(reader.getLocalName());
                        switch (propertyElement) {
                            case PROPERTY:
                                parseProperty(reader, addr, list);
                                break;
                            default:
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

    private static void parsePlugIn_Authorization(final XMLExtendedStreamReader reader, final Namespace expectedNs,
            final ModelNode parentAddress, final List<ModelNode> list) throws XMLStreamException {
        ModelNode addr = parentAddress.clone().add(PLUG_IN);
        ModelNode plugIn = Util.getEmptyOperation(ADD, addr);
        list.add(plugIn);

        requireSingleAttribute(reader, Attribute.NAME.getLocalName());
        // After double checking the name of the only attribute we can retrieve it.
        final String plugInName = reader.getAttributeValue(0);
        plugIn.get(NAME).set(plugInName);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PROPERTIES: {
                    while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                        requireNamespace(reader, expectedNs);
                        final Element propertyElement = Element.forName(reader.getLocalName());
                        switch (propertyElement) {
                            case PROPERTY:
                                parseProperty(reader, addr, list);
                                break;
                            default:
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

    private static void parseProperty(final XMLExtendedStreamReader reader, final ModelNode parentAddress, final List<ModelNode> list)
            throws XMLStreamException {

        final ModelNode add = new ModelNode();
        add.get(OP).set(ADD);
        list.add(add);

        boolean addressFound = false;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME:
                        add.get(OP_ADDR).set(parentAddress).add(PROPERTY, value);
                        addressFound = true;
                        break;
                    case VALUE: {
                        PropertyResourceDefinition.VALUE.parseAndSetParameter(value, add, reader);
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

        if (addressFound == false) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        requireNoContent(reader);
    }

    public static void parseAccessControlRoleMapping(final XMLExtendedStreamReader reader, final ModelNode accContAddr,
            final Namespace expectedNs, final List<ModelNode> list) throws XMLStreamException {
        requireNoAttributes(reader);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            if (element == Element.ROLE) {
                parseRole(reader, accContAddr, expectedNs, list);
            } else {
                throw unexpectedElement(reader);
            }
        }
    }

    private static void parseRole(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs,
            final List<ModelNode> list) throws XMLStreamException {
        final ModelNode add = new ModelNode();
        list.add(add);
        String name = null;

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME:
                        name = value;
                        break;
                    case INCLUDE_ALL: {
                        RoleMappingResourceDefinition.INCLUDE_ALL.parseAndSetParameter(value, add, reader);
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        ModelNode addr = address.clone().add(ROLE_MAPPING, name);
        add.get(OP_ADDR).set(addr);
        add.get(OP).set(ADD);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case INCLUDE: {
                    ModelNode includeAddr = addr.clone().add(INCLUDE);
                    parseIncludeExclude(reader, includeAddr, expectedNs, list);
                    break;
                }
                case EXCLUDE: {
                    ModelNode excludeAddr = addr.clone().add(EXCLUDE);
                    parseIncludeExclude(reader, excludeAddr, expectedNs, list);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private static void parseIncludeExclude(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs,
            final List<ModelNode> list) throws XMLStreamException {
        ParseUtils.requireNoAttributes(reader);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case GROUP: {
                    parsePrincipal(reader, address, GROUP, expectedNs, list);
                    break;
                }
                case USER: {
                    parsePrincipal(reader, address, USER, expectedNs, list);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private static void parsePrincipal(final XMLExtendedStreamReader reader, final ModelNode address, final String type,
            final Namespace expectedNs, final List<ModelNode> list) throws XMLStreamException {
        String alias = null;
        String realm = null;
        String name = null;

        ModelNode addOp = new ModelNode();
        addOp.get(OP).set(ADD);
        addOp.get(TYPE).set(type);

        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case ALIAS: {
                        alias = value;
                        break;
                    }
                    case NAME: {
                        name = value;
                        PrincipalResourceDefinition.NAME.parseAndSetParameter(value, addOp, reader);
                        break;
                    }
                    case REALM: {
                        realm = value;
                        PrincipalResourceDefinition.REALM.parseAndSetParameter(value, addOp, reader);
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

        if (name == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        String addrValue = alias == null ? generateAlias(type, name, realm) : alias;
        ModelNode addAddr = address.clone().add(addrValue);
        addOp.get(OP_ADDR).set(addAddr);
        list.add(addOp);

        ParseUtils.requireNoContent(reader);
    }

    private static String generateAlias(final String type, final String name, final String realm) {
        return type + "-" + name + (realm != null ? "@" + realm : "");
    }

    public static void parseAccessControlConstraints(final XMLExtendedStreamReader reader, final ModelNode accAuthzAddr, final Namespace expectedNs,
                                                     final List<ModelNode> list) throws XMLStreamException {
        ParseUtils.requireNoAttributes(reader);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case VAULT_EXPRESSION_SENSITIVITY: {
                    ModelNode vaultAddr = accAuthzAddr.clone().add(CONSTRAINT, VAULT_EXPRESSION);
                    parseClassificationType(reader, vaultAddr, expectedNs, list, true);
                    break;
                }
                case SENSITIVE_CLASSIFICATIONS: {
                    ModelNode sensAddr = accAuthzAddr.clone().add(CONSTRAINT, SENSITIVITY_CLASSIFICATION);
                    parseSensitiveClassifications(reader, sensAddr, expectedNs, list);
                    break;
                }
                case APPLICATION_CLASSIFICATIONS: {
                    ModelNode applAddr = accAuthzAddr.clone().add(CONSTRAINT, APPLICATION_CLASSIFICATION);
                    parseApplicationClassifications(reader, applAddr, expectedNs, list);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private static void parseSensitiveClassifications(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs,
            final List<ModelNode> list) throws XMLStreamException {

        ParseUtils.requireNoAttributes(reader);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case SENSITIVE_CLASSIFICATION: {
                    parseSensitivityClassification(reader, address, expectedNs, list);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private static void parseSensitivityClassification(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs,
            final List<ModelNode> list) throws XMLStreamException {
        parseClassificationType(reader, address, expectedNs, list, false);
//        String name = null;
//        final int count = reader.getAttributeCount();
//        for (int i = 0; i < count; i++) {
//            final String value = reader.getAttributeValue(i);
//            if (!isNoNamespaceAttribute(reader, i)) {
//                throw unexpectedAttribute(reader, i);
//            } else {
//                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
//                switch (attribute) {
//                    case NAME:
//                        name = value;
//                        break;
//                    default: {
//                        throw unexpectedAttribute(reader, i);
//                    }
//                }
//            }
//        }
//
//        if (name == null) {
//            throw ParseUtils.missingRequired(reader, Collections.singleton(NAME));
//        }
//
//        ModelNode newAddress = address.clone().add(SensitivityClassificationTypeResourceDefinition.PATH_ELEMENT.getKey(), name);
//        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
//            requireNamespace(reader, expectedNs);
//            final Element element = Element.forName(reader.getLocalName());
//            String name = null;
//
//            switch (element) {
//                case TYPE: {
//                    parseClassificationType(reader, newAddress, expectedNs, list, false);
//                    break;
//                }
//                default: {
//                    throw unexpectedElement(reader);
//                }
//            }
//        }
    }

    private static void parseClassificationType(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs,
            final List<ModelNode> list, boolean vault) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String name = null;
        String type = null;
        Map<String, ModelNode> values = new HashMap<String, ModelNode>();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = value;
                        break;
                    }
                    case TYPE: {
                        type = value;
                        break;
                    }
                    case REQUIRES_READ: {
                        values.put(SensitivityResourceDefinition.CONFIGURED_REQUIRES_READ.getName(),
                                SensitivityResourceDefinition.CONFIGURED_REQUIRES_READ.parse(value, reader));
                        break;
                    }
                    case REQUIRES_WRITE: {
                        values.put(SensitivityResourceDefinition.CONFIGURED_REQUIRES_WRITE.getName(),
                                SensitivityResourceDefinition.CONFIGURED_REQUIRES_WRITE.parse(value, reader));
                        break;
                    }
                    case REQUIRES_ADDRESSABLE: {
                        if (!vault) {
                            values.put(SensitivityResourceDefinition.CONFIGURED_REQUIRES_ADDRESSABLE.getName(),
                                SensitivityResourceDefinition.CONFIGURED_REQUIRES_ADDRESSABLE.parse(value, reader));
                            break;
                        }
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

        if (name == null && !vault) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        if (type == null && !vault) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.TYPE));
        }

        final ModelNode newAddress = vault ? address :
            address.clone()
            .add(SensitivityClassificationTypeResourceDefinition.PATH_ELEMENT.getKey(), type)
            .add(SensitivityResourceDefinition.PATH_ELEMENT.getKey(), name);

        for (Map.Entry<String, ModelNode> entry : values.entrySet()) {
            list.add(Util.getWriteAttributeOperation(newAddress, entry.getKey(), entry.getValue()));
        }
        ParseUtils.requireNoContent(reader);
    }

    private static void parseApplicationClassifications(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs,
            final List<ModelNode> list) throws XMLStreamException {

        ParseUtils.requireNoAttributes(reader);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case APPLICATION_CLASSIFICATION: {
                    parseApplicationClassification(reader, address, expectedNs, list);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private static void parseApplicationClassification(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs,
            final List<ModelNode> list) throws XMLStreamException {
        String name = null;
        String type = null;
        Boolean applicationValue = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME:
                        name = value;
                        break;
                    case TYPE:
                        type = value;
                        break;
                    case APPLICATION:
                        applicationValue = Boolean.valueOf(value);
                        break;
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }

        if (name == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(NAME));
        }
        if (type == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.TYPE));
        }
        if (applicationValue == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.APPLICATION));
        }

        ModelNode newAddress = address.clone()
                .add(ApplicationClassificationTypeResourceDefinition.PATH_ELEMENT.getKey(), type)
                .add(ApplicationClassificationConfigResourceDefinition.PATH_ELEMENT.getKey(), name);


        list.add(Util.getWriteAttributeOperation(newAddress, ApplicationClassificationConfigResourceDefinition.CONFIGURED_APPLICATION.getName(), applicationValue.toString()));
        ParseUtils.requireNoContent(reader);
    }

    public static void parseServerGroupScopedRoles(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list) throws XMLStreamException {

        ParseUtils.requireNoAttributes(reader);

        String scopedRoleType = ServerGroupScopedRoleResourceDefinition.PATH_ELEMENT.getKey();

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ROLE: {
                    parseScopedRole(reader, address, expectedNs, list, scopedRoleType, Element.SERVER_GROUP,
                            ServerGroupScopedRoleResourceDefinition.BASE_ROLE, ServerGroupScopedRoleResourceDefinition.SERVER_GROUPS, true);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

    }

    public static void parseHostScopedRoles(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list) throws XMLStreamException {

        ParseUtils.requireNoAttributes(reader);

        String scopedRoleType = HostScopedRolesResourceDefinition.PATH_ELEMENT.getKey();

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ROLE: {
                    parseScopedRole(reader, address, expectedNs, list, scopedRoleType, Element.HOST,
                            HostScopedRolesResourceDefinition.BASE_ROLE, HostScopedRolesResourceDefinition.HOSTS, false);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

    }

    private static void parseScopedRole(XMLExtendedStreamReader reader, ModelNode address, Namespace expectedNs,
                                 List<ModelNode> ops, String scopedRoleType, final Element listElement,
                                 SimpleAttributeDefinition baseRoleDefinition, ListAttributeDefinition listDefinition,
                                 boolean requireChildren) throws XMLStreamException {

        final ModelNode addOp = Util.createAddOperation();
        ops.add(addOp);
        final ModelNode ourAddress = addOp.get(OP_ADDR).set(address);
        final Set<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.BASE_ROLE);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    ourAddress.add(scopedRoleType, value);
                    break;
                case BASE_ROLE:
                    baseRoleDefinition.parseAndSetParameter(value, addOp, reader);
                    break;
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }

        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }

        boolean missingChildren = requireChildren;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            boolean named = false;
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            if (element == listElement) {
                missingChildren = false;
                final int groupCount = reader.getAttributeCount();
                for (int i = 0; i < groupCount; i++) {
                    final String value = reader.getAttributeValue(i);
                    if (!isNoNamespaceAttribute(reader, i)) {
                        throw unexpectedAttribute(reader, i);
                    }
                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                    required.remove(attribute);
                    if (attribute == Attribute.NAME) {
                        named = true;
                        listDefinition.parseAndAddParameterElement(value, addOp, reader);
                    } else {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            } else {
                throw unexpectedElement(reader);
            }

            if (!named) {
                throw missingRequired(reader, EnumSet.of(Attribute.NAME));
            }

            requireNoContent(reader);
        }

        if (missingChildren) {
            throw missingRequired(reader, EnumSet.of(listElement));
        }
    }

    public void writeManagement(final XMLExtendedStreamWriter writer, final ModelNode management, boolean allowInterfaces)
            throws XMLStreamException {
        boolean hasSecurityRealm = management.hasDefined(SECURITY_REALM);
        boolean hasConnection = management.hasDefined(LDAP_CONNECTION);
        boolean hasInterface = allowInterfaces && management.hasDefined(MANAGEMENT_INTERFACE);

        // TODO - These checks are going to become a source of bugs in certain cases - what we really need is a way to allow writing to continue and
        // if an element is empty by the time it is closed then undo the write of that element.

        ModelNode accessAuthorization = management.hasDefined(ACCESS) ? management.get(ACCESS, AUTHORIZATION) : null;
        boolean accessAuthorizationDefined = accessAuthorization != null && accessAuthorization.isDefined();
        boolean hasServerGroupRoles = accessAuthorizationDefined && accessAuthorization.hasDefined(SERVER_GROUP_SCOPED_ROLE);
        boolean hasHostRoles = accessAuthorizationDefined && (accessAuthorization.hasDefined(HOST_SCOPED_ROLE) || accessAuthorization.hasDefined(HOST_SCOPED_ROLES));
        boolean hasRoleMapping = accessAuthorizationDefined && accessAuthorization.hasDefined(ROLE_MAPPING);
        Map<String, Map<String, Set<String>>> configuredAccessConstraints = getConfiguredAccessConstraints(accessAuthorization);
        boolean hasProvider = accessAuthorizationDefined && accessAuthorization.hasDefined(AccessAuthorizationResourceDefinition.PROVIDER.getName());
        boolean hasCombinationPolicy = accessAuthorizationDefined && accessAuthorization.hasDefined(AccessAuthorizationResourceDefinition.PERMISSION_COMBINATION_POLICY.getName());
        ModelNode auditLog = management.hasDefined(ACCESS) ? management.get(ACCESS, AUDIT) : new ModelNode();

        if (!hasSecurityRealm && !hasConnection && !hasInterface && !hasServerGroupRoles
              && !hasHostRoles && !hasRoleMapping && configuredAccessConstraints.size() == 0
                && !hasProvider && !hasCombinationPolicy && !auditLog.isDefined()) {
            return;
        }

        writer.writeStartElement(Element.MANAGEMENT.getLocalName());
        if (hasSecurityRealm) {
            writeSecurityRealm(writer, management);
        }

        if (hasConnection) {
            writeOutboundConnections(writer, management);
        }

        if (auditLog.isDefined()) {
            delegate.writeAuditLog(writer, auditLog);
        }

        if (allowInterfaces && hasInterface) {
            writeManagementInterfaces(writer, management);
        }

        if (accessAuthorizationDefined) {
            delegate.writeAccessControl(writer, accessAuthorization);
        }

        writer.writeEndElement();
    }

    private static void writeAccessControl(final XMLExtendedStreamWriter writer, final ModelNode accessAuthorization) throws XMLStreamException {
        if (accessAuthorization == null || accessAuthorization.isDefined()==false) {
            return; // All subsequent checks are based on this being defined.
        }

        boolean hasServerGroupRoles =  accessAuthorization.hasDefined(SERVER_GROUP_SCOPED_ROLE);
        boolean hasHostRoles = accessAuthorization.hasDefined(HOST_SCOPED_ROLE) || accessAuthorization.hasDefined(HOST_SCOPED_ROLES);
        boolean hasRoleMapping = accessAuthorization.hasDefined(ROLE_MAPPING);
        Map<String, Map<String, Set<String>>> configuredAccessConstraints = getConfiguredAccessConstraints(accessAuthorization);
        boolean hasProvider = accessAuthorization.hasDefined(AccessAuthorizationResourceDefinition.PROVIDER.getName());
        boolean hasCombinationPolicy = accessAuthorization.hasDefined(AccessAuthorizationResourceDefinition.PERMISSION_COMBINATION_POLICY.getName());

        if (!hasProvider && !hasCombinationPolicy && !hasServerGroupRoles && !hasHostRoles
                && !hasRoleMapping && configuredAccessConstraints.size() == 0) {
            return;
        }

        writer.writeStartElement(Element.ACCESS_CONTROL.getLocalName());

        AccessAuthorizationResourceDefinition.PROVIDER.marshallAsAttribute(accessAuthorization, writer);
        AccessAuthorizationResourceDefinition.PERMISSION_COMBINATION_POLICY.marshallAsAttribute(accessAuthorization, writer);

        if (hasServerGroupRoles) {
            ModelNode serverGroupRoles = accessAuthorization.get(SERVER_GROUP_SCOPED_ROLE);
            if (serverGroupRoles.asInt() > 0) {
                writeServerGroupScopedRoles(writer, serverGroupRoles);
            }
        }

        if (hasHostRoles) {
            ModelNode serverGroupRoles = accessAuthorization.get(HOST_SCOPED_ROLE);
            if (serverGroupRoles.asInt() > 0) {
                writeHostScopedRoles(writer, serverGroupRoles);
            }
        }

        if (hasRoleMapping) {
            writeRoleMapping(writer, accessAuthorization);
        }

        if (configuredAccessConstraints.size() > 0) {
            writeAccessConstraints(writer, accessAuthorization, configuredAccessConstraints);
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
            if (realm.hasDefined(PLUG_IN)) {
                writePlugIns(writer, realm.get(PLUG_IN));
            }

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

    private void writePlugIns(XMLExtendedStreamWriter writer, ModelNode plugIns) throws XMLStreamException {
        writer.writeStartElement(Element.PLUG_INS.getLocalName());
        for (Property variable : plugIns.asPropertyList()) {
            writer.writeEmptyElement(Element.PLUG_IN.getLocalName());
            writer.writeAttribute(Attribute.MODULE.getLocalName(), variable.getName());
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
            boolean hasProvider = ssl.hasDefined(KEYSTORE_PROVIDER)
                    && (JKS.equals(ssl.require(KEYSTORE_PROVIDER).asString()) == false);
            if (hasProvider || ssl.hasDefined(KeystoreAttributes.KEYSTORE_PATH.getName())) {
                writer.writeEmptyElement(Element.KEYSTORE.getLocalName());
                KeystoreAttributes.KEYSTORE_PROVIDER.marshallAsAttribute(ssl, writer);
                KeystoreAttributes.KEYSTORE_PATH.marshallAsAttribute(ssl, writer);
                KeystoreAttributes.KEYSTORE_RELATIVE_TO.marshallAsAttribute(ssl, writer);
                KeystoreAttributes.KEYSTORE_PASSWORD.marshallAsAttribute(ssl, writer);
                KeystoreAttributes.ALIAS.marshallAsAttribute(ssl, writer);
                KeystoreAttributes.KEY_PASSWORD.marshallAsAttribute(ssl, writer);
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

    private void writeLdapCacheIfDefined(XMLExtendedStreamWriter writer, ModelNode parent) throws XMLStreamException {
        if (parent.hasDefined(CACHE)) {
            ModelNode cacheHolder = parent.require(CACHE);
            final ModelNode cache;
            final String type;

            if (cacheHolder.hasDefined(BY_ACCESS_TIME)) {
                cache = cacheHolder.require(BY_ACCESS_TIME);
                type = BY_ACCESS_TIME;
            } else if (cacheHolder.hasDefined(BY_SEARCH_TIME)) {
                cache = cacheHolder.require(BY_SEARCH_TIME);
                type = BY_SEARCH_TIME;
            } else {
                return;
            }

            writer.writeStartElement(Element.CACHE.getLocalName());
            if (type.equals(BY_SEARCH_TIME) == false) {
                writer.writeAttribute(Attribute.TYPE.getLocalName(), type);
            }
            LdapCacheResourceDefinition.EVICTION_TIME.marshallAsAttribute(cache, writer);
            LdapCacheResourceDefinition.CACHE_FAILURES.marshallAsAttribute(cache, writer);
            LdapCacheResourceDefinition.MAX_CACHE_SIZE.marshallAsAttribute(cache, writer);
            writer.writeEndElement();
        }
    }

    private void writeAuthentication(XMLExtendedStreamWriter writer, ModelNode realm) throws XMLStreamException {
        writer.writeStartElement(Element.AUTHENTICATION.getLocalName());
        ModelNode authentication = realm.require(AUTHENTICATION);

        if (authentication.hasDefined(TRUSTSTORE)) {
            ModelNode truststore = authentication.require(TRUSTSTORE);
            writer.writeEmptyElement(Element.TRUSTSTORE.getLocalName());
            KeystoreAttributes.KEYSTORE_PROVIDER.marshallAsAttribute(truststore, writer);
            KeystoreAttributes.KEYSTORE_PATH.marshallAsAttribute(truststore, writer);
            KeystoreAttributes.KEYSTORE_RELATIVE_TO.marshallAsAttribute(truststore, writer);
            KeystoreAttributes.KEYSTORE_PASSWORD.marshallAsAttribute(truststore, writer);
        }

        if (authentication.hasDefined(LOCAL)) {
            ModelNode local = authentication.require(LOCAL);
            writer.writeStartElement(Element.LOCAL.getLocalName());
            LocalAuthenticationResourceDefinition.DEFAULT_USER.marshallAsAttribute(local, writer);
            LocalAuthenticationResourceDefinition.ALLOWED_USERS.marshallAsAttribute(local, writer);
            writer.writeEndElement();
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
            LdapAuthenticationResourceDefinition.ALLOW_EMPTY_PASSWORDS.marshallAsAttribute(userLdap, writer);
            LdapAuthenticationResourceDefinition.USERNAME_LOAD.marshallAsAttribute(userLdap, writer);

            writeLdapCacheIfDefined(writer, userLdap);

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
        } else if (authentication.has(USERS)) {
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
        } else if (authentication.hasDefined(PLUG_IN)) {
            writePlugIn_Authentication(writer, authentication.get(PLUG_IN));
        }

        writer.writeEndElement();
    }

    private void writePlugIn_Authentication(XMLExtendedStreamWriter writer, ModelNode plugIn) throws XMLStreamException {
        writer.writeStartElement(Element.PLUG_IN.getLocalName());
        AbstractPlugInAuthResourceDefinition.NAME.marshallAsAttribute(plugIn, writer);
        PlugInAuthenticationResourceDefinition.MECHANISM.marshallAsAttribute(plugIn, writer);
        if (plugIn.hasDefined(PROPERTY)) {
            writer.writeStartElement(PROPERTIES);
            for (Property current : plugIn.get(PROPERTY).asPropertyList()) {
                writer.writeEmptyElement(PROPERTY);
                writer.writeAttribute(Attribute.NAME.getLocalName(), current.getName());
                PropertyResourceDefinition.VALUE.marshallAsAttribute(current.getValue(), writer);
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeAuthorization(XMLExtendedStreamWriter writer, ModelNode realm) throws XMLStreamException {
        writer.writeStartElement(Element.AUTHORIZATION.getLocalName());
        SecurityRealmResourceDefinition.MAP_GROUPS_TO_ROLES.marshallAsAttribute(realm, writer);
        ModelNode authorization = realm.require(AUTHORIZATION);
        if (authorization.hasDefined(PROPERTIES)) {
            ModelNode properties = authorization.require(PROPERTIES);
            writer.writeEmptyElement(Element.PROPERTIES.getLocalName());
            PropertiesAuthorizationResourceDefinition.PATH.marshallAsAttribute(properties, writer);
            PropertiesAuthorizationResourceDefinition.RELATIVE_TO.marshallAsAttribute(properties, writer);
        } else if (authorization.hasDefined(PLUG_IN)) {
            writePlugIn_Authorization(writer, authorization.get(PLUG_IN));
        } else if (authorization.hasDefined(LDAP)) {
            writeLdapAuthorization(writer,authorization.get(LDAP));
        }

        writer.writeEndElement();
    }

    private void writeLdapAuthorization(XMLExtendedStreamWriter writer, ModelNode ldapNode) throws XMLStreamException {
        writer.writeStartElement(Element.LDAP.getLocalName());
        LdapAuthorizationResourceDefinition.CONNECTION.marshallAsAttribute(ldapNode, writer);
        if (ldapNode.hasDefined(USERNAME_TO_DN)) {
            ModelNode usenameToDn = ldapNode.require(USERNAME_TO_DN);
            if (usenameToDn.hasDefined(USERNAME_IS_DN) || usenameToDn.hasDefined(USERNAME_FILTER)
                    || usenameToDn.hasDefined(ADVANCED_FILTER)) {
                writer.writeStartElement(Element.USERNAME_TO_DN.getLocalName());
                if (usenameToDn.hasDefined(USERNAME_IS_DN)) {
                    ModelNode usernameIsDn = usenameToDn.require(USERNAME_IS_DN);
                    UserIsDnResourceDefintion.FORCE.marshallAsAttribute(usernameIsDn, writer);
                    writeLdapCacheIfDefined(writer, usernameIsDn);
                    writer.writeEmptyElement(Element.USERNAME_IS_DN.getLocalName());
                } else if (usenameToDn.hasDefined(USERNAME_FILTER)) {
                    ModelNode usernameFilter = usenameToDn.require(USERNAME_FILTER);
                    UserSearchResourceDefintion.FORCE.marshallAsAttribute(usernameFilter, writer);
                    writeLdapCacheIfDefined(writer, usernameFilter);
                    writer.writeStartElement(Element.USERNAME_FILTER.getLocalName());
                    UserSearchResourceDefintion.BASE_DN.marshallAsAttribute(usernameFilter, writer);
                    UserSearchResourceDefintion.RECURSIVE.marshallAsAttribute(usernameFilter, writer);
                    UserSearchResourceDefintion.USER_DN_ATTRIBUTE.marshallAsAttribute(usernameFilter, writer);
                    UserSearchResourceDefintion.ATTRIBUTE.marshallAsAttribute(usernameFilter, writer);
                    writer.writeEndElement();
                } else {
                    ModelNode advancedFilter = usenameToDn.require(ADVANCED_FILTER);
                    AdvancedUserSearchResourceDefintion.FORCE.marshallAsAttribute(advancedFilter, writer);
                    writeLdapCacheIfDefined(writer, advancedFilter);
                    writer.writeStartElement(Element.ADVANCED_FILTER.getLocalName());
                    AdvancedUserSearchResourceDefintion.BASE_DN.marshallAsAttribute(advancedFilter, writer);
                    AdvancedUserSearchResourceDefintion.RECURSIVE.marshallAsAttribute(advancedFilter, writer);
                    AdvancedUserSearchResourceDefintion.USER_DN_ATTRIBUTE.marshallAsAttribute(advancedFilter, writer);
                    AdvancedUserSearchResourceDefintion.FILTER.marshallAsAttribute(advancedFilter, writer);
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
        }

        if (ldapNode.hasDefined(GROUP_SEARCH)) {
            ModelNode groupSearch = ldapNode.require(GROUP_SEARCH);

            if (groupSearch.hasDefined(GROUP_TO_PRINCIPAL) || groupSearch.hasDefined(PRINCIPAL_TO_GROUP)) {
                writer.writeStartElement(Element.GROUP_SEARCH.getLocalName());
                if (groupSearch.hasDefined(GROUP_TO_PRINCIPAL)) {
                    ModelNode groupToPrincipal = groupSearch.require(GROUP_TO_PRINCIPAL);
                    GroupToPrincipalResourceDefinition.GROUP_NAME.marshallAsAttribute(groupToPrincipal, writer);
                    GroupToPrincipalResourceDefinition.ITERATIVE.marshallAsAttribute(groupToPrincipal, writer);
                    GroupToPrincipalResourceDefinition.GROUP_DN_ATTRIBUTE.marshallAsAttribute(groupToPrincipal, writer);
                    GroupToPrincipalResourceDefinition.GROUP_NAME_ATTRIBUTE.marshallAsAttribute(groupToPrincipal, writer);
                    writeLdapCacheIfDefined(writer, groupToPrincipal);
                    writer.writeStartElement(Element.GROUP_TO_PRINCIPAL.getLocalName());
                    GroupToPrincipalResourceDefinition.SEARCH_BY.marshallAsAttribute(groupToPrincipal, writer);
                    GroupToPrincipalResourceDefinition.BASE_DN.marshallAsAttribute(groupToPrincipal, writer);
                    GroupToPrincipalResourceDefinition.RECURSIVE.marshallAsAttribute(groupToPrincipal, writer);
                    writer.writeStartElement(Element.MEMBERSHIP_FILTER.getLocalName());
                    GroupToPrincipalResourceDefinition.PRINCIPAL_ATTRIBUTE.marshallAsAttribute(groupToPrincipal, writer);
                    writer.writeEndElement();
                    writer.writeEndElement();
                } else {
                    ModelNode principalToGroup = groupSearch.require(PRINCIPAL_TO_GROUP);
                    PrincipalToGroupResourceDefinition.GROUP_NAME.marshallAsAttribute(principalToGroup, writer);
                    PrincipalToGroupResourceDefinition.ITERATIVE.marshallAsAttribute(principalToGroup, writer);
                    PrincipalToGroupResourceDefinition.GROUP_DN_ATTRIBUTE.marshallAsAttribute(principalToGroup, writer);
                    PrincipalToGroupResourceDefinition.GROUP_NAME_ATTRIBUTE.marshallAsAttribute(principalToGroup, writer);
                    writeLdapCacheIfDefined(writer, principalToGroup);
                    writer.writeStartElement(Element.PRINCIPAL_TO_GROUP.getLocalName());
                    PrincipalToGroupResourceDefinition.GROUP_ATTRIBUTE.marshallAsAttribute(principalToGroup, writer);
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
        }

        writer.writeEndElement();
    }

    private void writePlugIn_Authorization(XMLExtendedStreamWriter writer, ModelNode plugIn) throws XMLStreamException {
        writer.writeStartElement(Element.PLUG_IN.getLocalName());
        AbstractPlugInAuthResourceDefinition.NAME.marshallAsAttribute(plugIn, writer);
        if (plugIn.hasDefined(PROPERTY)) {
            writer.writeStartElement(PROPERTIES);
            for (Property current : plugIn.get(PROPERTY).asPropertyList()) {
                writer.writeEmptyElement(PROPERTY);
                writer.writeAttribute(Attribute.NAME.getLocalName(), current.getName());
                PropertyResourceDefinition.VALUE.marshallAsAttribute(current.getValue(), writer);
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeOutboundConnections(XMLExtendedStreamWriter writer, ModelNode management) throws XMLStreamException {

        writer.writeStartElement(Element.OUTBOUND_CONNECTIONS.getLocalName());

        for (Property variable : management.get(LDAP_CONNECTION).asPropertyList()) {
            ModelNode connection = variable.getValue();
            writer.writeStartElement(Element.LDAP.getLocalName());
            writer.writeAttribute(Attribute.NAME.getLocalName(), variable.getName());
            LdapConnectionResourceDefinition.URL.marshallAsAttribute(connection, writer);
            LdapConnectionResourceDefinition.SEARCH_DN.marshallAsAttribute(connection, writer);
            LdapConnectionResourceDefinition.SEARCH_CREDENTIAL.marshallAsAttribute(connection, writer);
            LdapConnectionResourceDefinition.SECURITY_REALM.marshallAsAttribute(connection, writer);
            LdapConnectionResourceDefinition.INITIAL_CONTEXT_FACTORY.marshallAsAttribute(connection, writer);
            if (connection.hasDefined(PROPERTY)) {
                List<Property> propertyList = connection.get(PROPERTY).asPropertyList();
                if (propertyList.size() > 0) {
                    writer.writeStartElement(PROPERTIES);
                    for (Property current : propertyList) {
                        writer.writeEmptyElement(PROPERTY);
                        writer.writeAttribute(Attribute.NAME.getLocalName(), current.getName());
                        LdapConnectionPropertyResourceDefinition.VALUE.marshallAsAttribute(current.getValue(), writer);
                    }
                    writer.writeEndElement();
                }
            }
            writer.writeEndElement();
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

    private static Map<String, Map<String, Set<String>>> getConfiguredAccessConstraints(ModelNode accessAuthorization) {
        Map<String, Map<String, Set<String>>> configuredConstraints = new HashMap<String, Map<String, Set<String>>>();
        if (accessAuthorization != null && accessAuthorization.hasDefined(CONSTRAINT)) {
            ModelNode constraint = accessAuthorization.get(CONSTRAINT);

            configuredConstraints.putAll(getVaultConstraints(constraint));
            configuredConstraints.putAll(getSensitivityClassificationConstraints(constraint));
            configuredConstraints.putAll(getApplicationClassificationConstraints(constraint));
        }

        return configuredConstraints;
    }

    private static Map<String, Map<String, Set<String>>> getVaultConstraints(final ModelNode constraint) {
        Map<String, Map<String, Set<String>>> configuredConstraints = new HashMap<String, Map<String, Set<String>>>();

        if (constraint.hasDefined(VAULT_EXPRESSION)) {
            ModelNode classification = constraint.require(VAULT_EXPRESSION);
            if (classification.hasDefined(SensitivityResourceDefinition.CONFIGURED_REQUIRES_WRITE.getName())
                    || classification.hasDefined(SensitivityResourceDefinition.CONFIGURED_REQUIRES_READ.getName())) {
                configuredConstraints.put(SensitivityResourceDefinition.VAULT_ELEMENT.getKey(),
                        Collections.<String, Set<String>> emptyMap());
            }
        }

        return configuredConstraints;
    }

    private static Map<String, Map<String, Set<String>>> getSensitivityClassificationConstraints(final ModelNode constraint) {
        Map<String, Map<String, Set<String>>> configuredConstraints = new HashMap<String, Map<String, Set<String>>>();

        if (constraint.hasDefined(SENSITIVITY_CLASSIFICATION)) {
            ModelNode sensitivityParent = constraint.require(SENSITIVITY_CLASSIFICATION);

            if (sensitivityParent.hasDefined(TYPE)) {
                for (Property typeProperty : sensitivityParent.get(TYPE).asPropertyList()) {
                    if (typeProperty.getValue().hasDefined(CLASSIFICATION)) {
                        for (Property sensitivityProperty : typeProperty.getValue().get(CLASSIFICATION).asPropertyList()) {
                            ModelNode classification = sensitivityProperty.getValue();
                            if (classification.hasDefined(SensitivityResourceDefinition.CONFIGURED_REQUIRES_ADDRESSABLE.getName())
                                    || classification.hasDefined(SensitivityResourceDefinition.CONFIGURED_REQUIRES_WRITE
                                            .getName())
                                    || classification.hasDefined(SensitivityResourceDefinition.CONFIGURED_REQUIRES_READ
                                            .getName())) {
                                Map<String, Set<String>> constraintMap = configuredConstraints.get(SENSITIVITY_CLASSIFICATION);
                                if (constraintMap == null) {
                                    constraintMap = new TreeMap<String, Set<String>>();
                                    configuredConstraints.put(SENSITIVITY_CLASSIFICATION, constraintMap);
                                }
                                Set<String> types = constraintMap.get(typeProperty.getName());
                                if (types == null) {
                                    types = new TreeSet<String>();
                                    constraintMap.put(typeProperty.getName(), types);
                                }
                                types.add(sensitivityProperty.getName());
                            }
                        }
                    }
                }
            }
        }

        return configuredConstraints;
    }

    private static Map<String, Map<String, Set<String>>> getApplicationClassificationConstraints(final ModelNode constraint) {
        Map<String, Map<String, Set<String>>> configuredConstraints = new HashMap<String, Map<String, Set<String>>>();

        if (constraint.hasDefined(APPLICATION_CLASSIFICATION)) {
            ModelNode appTypeParent = constraint.require(APPLICATION_CLASSIFICATION);

            if (appTypeParent.hasDefined(TYPE)) {
                for (Property typeProperty : appTypeParent.get(TYPE).asPropertyList()) {
                    if (typeProperty.getValue().hasDefined(CLASSIFICATION)) {
                        for (Property applicationProperty : typeProperty.getValue().get(CLASSIFICATION).asPropertyList()) {
                            ModelNode applicationType = applicationProperty.getValue();
                            if (applicationType.hasDefined(ApplicationClassificationConfigResourceDefinition.CONFIGURED_APPLICATION.getName())) {
                                Map<String, Set<String>> constraintMap = configuredConstraints.get(APPLICATION_CLASSIFICATION);
                                if (constraintMap == null) {
                                    constraintMap = new TreeMap<String, Set<String>>();
                                    configuredConstraints.put(APPLICATION_CLASSIFICATION, constraintMap);
                                }
                                Set<String> types = constraintMap.get(typeProperty.getName());
                                if (types == null) {
                                    types = new TreeSet<String>();
                                    constraintMap.put(typeProperty.getName(), types);
                                }
                                types.add(applicationProperty.getName());
                            }
                        }
                    }
                }
            }
        }

        return configuredConstraints;
    }

    private static void writeRoleMapping(XMLExtendedStreamWriter writer, ModelNode accessAuthorization)
            throws XMLStreamException {
        writer.writeStartElement(Element.ROLE_MAPPING.getLocalName());


        if (accessAuthorization.hasDefined(ROLE_MAPPING)) {
            ModelNode roleMappings = accessAuthorization.get(ROLE_MAPPING);

            for (Property variable : roleMappings.asPropertyList()) {
                writer.writeStartElement(Element.ROLE.getLocalName());
                writeAttribute(writer, Attribute.NAME, variable.getName());
                ModelNode role = variable.getValue();
                RoleMappingResourceDefinition.INCLUDE_ALL.marshallAsAttribute(role, writer);
                if (role.hasDefined(INCLUDE)) {
                    writeIncludeExclude(writer, Element.INCLUDE.getLocalName(), role.get(INCLUDE));
                }

                if (role.hasDefined(EXCLUDE)) {
                    writeIncludeExclude(writer, Element.EXCLUDE.getLocalName(), role.get(EXCLUDE));
                }

                writer.writeEndElement();
            }
        }

        writer.writeEndElement();
    }

    private static void writeIncludeExclude(XMLExtendedStreamWriter writer, String elementName, ModelNode includeExclude)
            throws XMLStreamException {
        List<Property> list = includeExclude.asPropertyList();
        if (list.isEmpty()) {
            return;
        }

        writer.writeStartElement(elementName);
        for (Property current : list) {
            // The names where only arbitrary to allow unique referencing.
            writePrincipal(writer, current.getName(), current.getValue());
        }

        writer.writeEndElement();
    }

    private static void writePrincipal(XMLExtendedStreamWriter writer, String alias, ModelNode principal) throws XMLStreamException {
        String elementName = principal.require(TYPE).asString().equalsIgnoreCase(GROUP) ? Element.GROUP.getLocalName() : Element.USER.getLocalName();
        writer.writeStartElement(elementName);

        String realm = principal.get(REALM).isDefined() ? principal.require(REALM).asString() : null;
        String name = principal.require(NAME).asString();

        String expectedAlias = generateAlias(elementName, name, realm);
        if (alias.equals(expectedAlias)==false) {
            writeAttribute(writer, Attribute.ALIAS, alias);
        }

        PrincipalResourceDefinition.REALM.marshallAsAttribute(principal, writer);

        PrincipalResourceDefinition.NAME.marshallAsAttribute(principal, writer);

        writer.writeEndElement();
    }

    private static void writeAccessConstraints(XMLExtendedStreamWriter writer, ModelNode accessAuthorization, Map<String, Map<String, Set<String>>> configuredConstraints) throws XMLStreamException {
        writer.writeStartElement(Element.CONSTRAINTS.getLocalName());

        if (configuredConstraints.containsKey(SensitivityResourceDefinition.VAULT_ELEMENT.getKey())){
            writer.writeEmptyElement(Element.VAULT_EXPRESSION_SENSITIVITY.getLocalName());
            ModelNode model = accessAuthorization.get(SensitivityResourceDefinition.VAULT_ELEMENT.getKey(),
                    SensitivityResourceDefinition.VAULT_ELEMENT.getValue());
            SensitivityResourceDefinition.CONFIGURED_REQUIRES_READ.marshallAsAttribute(model, writer);
            SensitivityResourceDefinition.CONFIGURED_REQUIRES_WRITE.marshallAsAttribute(model, writer);
        }

        if (configuredConstraints.containsKey(SENSITIVITY_CLASSIFICATION)) {
            writer.writeStartElement(Element.SENSITIVE_CLASSIFICATIONS.getLocalName());
            Map<String, Set<String>> constraints = configuredConstraints.get(SENSITIVITY_CLASSIFICATION);
            for (Map.Entry<String, Set<String>> entry : constraints.entrySet()) {
                for (String classification : entry.getValue()) {
                    writer.writeEmptyElement(Element.SENSITIVE_CLASSIFICATION.getLocalName());
                    ModelNode model = accessAuthorization.get(CONSTRAINT, SENSITIVITY_CLASSIFICATION, TYPE, entry.getKey(), CLASSIFICATION, classification);
                    writeAttribute(writer, Attribute.TYPE, entry.getKey());
                    writeAttribute(writer, Attribute.NAME, classification);
                    SensitivityResourceDefinition.CONFIGURED_REQUIRES_ADDRESSABLE.marshallAsAttribute(model, writer);
                    SensitivityResourceDefinition.CONFIGURED_REQUIRES_READ.marshallAsAttribute(model, writer);
                    SensitivityResourceDefinition.CONFIGURED_REQUIRES_WRITE.marshallAsAttribute(model, writer);
                }
            }
            writer.writeEndElement();
        }
        if (configuredConstraints.containsKey(APPLICATION_CLASSIFICATION)) {
            writer.writeStartElement(Element.APPLICATION_CLASSIFICATIONS.getLocalName());
            Map<String, Set<String>> constraints = configuredConstraints.get(APPLICATION_CLASSIFICATION);
            for (Map.Entry<String, Set<String>> entry : constraints.entrySet()) {

                for (String classification : entry.getValue()) {
                    writer.writeEmptyElement(Element.APPLICATION_CLASSIFICATION.getLocalName());
                    ModelNode model = accessAuthorization.get(CONSTRAINT, APPLICATION_CLASSIFICATION, TYPE, entry.getKey(), CLASSIFICATION, classification);
                    writeAttribute(writer, Attribute.TYPE, entry.getKey());
                    writeAttribute(writer, Attribute.NAME, classification);
                    ApplicationClassificationConfigResourceDefinition.CONFIGURED_APPLICATION.marshallAsAttribute(model, writer);
                }
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private static void writeServerGroupScopedRoles(XMLExtendedStreamWriter writer, ModelNode scopedRoles) throws XMLStreamException {
        writer.writeStartElement(Element.SERVER_GROUP_SCOPED_ROLES.getLocalName());

        for (Property property : scopedRoles.asPropertyList()) {
            writer.writeStartElement(Element.ROLE.getLocalName());
            writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
            ModelNode value = property.getValue();
            ServerGroupScopedRoleResourceDefinition.BASE_ROLE.marshallAsAttribute(value, writer);
            ServerGroupScopedRoleResourceDefinition.SERVER_GROUPS.marshallAsElement(value, writer);
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private static void writeHostScopedRoles(XMLExtendedStreamWriter writer, ModelNode scopedRoles) throws XMLStreamException {
        writer.writeStartElement(Element.HOST_SCOPED_ROLES.getLocalName());

        for (Property property : scopedRoles.asPropertyList()) {
            writer.writeStartElement(Element.ROLE.getLocalName());
            writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
            ModelNode value = property.getValue();
            HostScopedRolesResourceDefinition.BASE_ROLE.marshallAsAttribute(value, writer);
            HostScopedRolesResourceDefinition.HOSTS.marshallAsElement(value, writer);
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private static void writeAttribute(XMLExtendedStreamWriter writer, Attribute attribute, String value)
            throws XMLStreamException {
        writer.writeAttribute(attribute.getLocalName(), value);
    }
}
