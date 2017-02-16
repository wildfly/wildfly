/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URI;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequiredElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.APPLICATION_SECURITY_DOMAIN;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.IDENTITY;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.SERVICE;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parser for ejb3:5.0 namespace.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class EJB3Subsystem50Parser extends EJB3Subsystem40Parser {

    EJB3Subsystem50Parser() {
    }

    @Override
    protected EJB3SubsystemNamespace getExpectedNamespace() {
        return EJB3SubsystemNamespace.EJB3_5_0;
    }

    @Override
    protected void readElement(final XMLExtendedStreamReader reader, final EJB3SubsystemXMLElement element, final List<ModelNode> operations, final ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        switch (element) {
            case APPLICATION_SECURITY_DOMAINS: {
                parseApplicationSecurityDomains(reader, operations);
                break;
            }
            case IDENTITY: {
                parseIdentity(reader, operations);
                break;
            }
            case ALLOW_EJB_NAME_REGEX: {
                parseAllowEjbNameRegex(reader, ejb3SubsystemAddOperation);
                break;
            }
            case ENABLE_GRACEFUL_TXN_SHUTDOWN: {
                parseEnableGracefulTxnShutdown(reader, ejb3SubsystemAddOperation);
                break;
            }
            default: {
                super.readElement(reader, element, operations, ejb3SubsystemAddOperation);
            }
        }
    }

    private void parseApplicationSecurityDomains(final XMLExtendedStreamReader reader, final List<ModelNode> operations) throws XMLStreamException {
        requireNoAttributes(reader);
        boolean applicationSecurityDomainFound = false;
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case APPLICATION_SECURITY_DOMAIN: {
                    parseApplicationSecurityDomain(reader, operations);
                    applicationSecurityDomainFound = true;
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if (! applicationSecurityDomainFound) {
            throw missingRequiredElement(reader, Collections.singleton(EJB3SubsystemXMLElement.APPLICATION_SECURITY_DOMAIN.getLocalName()));
        }
    }

    private void parseApplicationSecurityDomain(final XMLExtendedStreamReader reader, final List<ModelNode> operations) throws XMLStreamException {
        String applicationSecurityDomain = null;
        ModelNode operation = Util.createAddOperation();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String attributeValue = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                    applicationSecurityDomain = attributeValue;
                    break;
                case SECURITY_DOMAIN:
                    ApplicationSecurityDomainDefinition.SECURITY_DOMAIN.parseAndSetParameter(attributeValue, operation, reader);
                    break;
                case ENABLE_JACC:
                    ApplicationSecurityDomainDefinition.ENABLE_JACC.parseAndSetParameter(attributeValue, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (applicationSecurityDomain == null) {
            throw missingRequired(reader, Collections.singleton(EJB3SubsystemXMLAttribute.NAME.getLocalName()));
        }
        requireNoContent(reader);
        final PathAddress address = this.getEJB3SubsystemAddress().append(PathElement.pathElement(APPLICATION_SECURITY_DOMAIN, applicationSecurityDomain));
        operation.get(OP_ADDR).set(address.toModelNode());
        operations.add(operation);
    }

    private void parseAllowEjbNameRegex(XMLExtendedStreamReader reader, ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        final EnumSet<EJB3SubsystemXMLAttribute> missingRequiredAttributes = EnumSet.of(EJB3SubsystemXMLAttribute.VALUE);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case VALUE:
                    EJB3SubsystemRootResourceDefinition.ALLOW_EJB_NAME_REGEX.parseAndSetParameter(value, ejb3SubsystemAddOperation, reader);
                    // found the mandatory attribute
                    missingRequiredAttributes.remove(EJB3SubsystemXMLAttribute.VALUE);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);
        if (!missingRequiredAttributes.isEmpty()) {
            throw missingRequired(reader, missingRequiredAttributes);
        }
    }

    private void parseIdentity(final XMLExtendedStreamReader reader, final List<ModelNode> operations) throws XMLStreamException {
        final PathAddress address = this.getEJB3SubsystemAddress().append(SERVICE, IDENTITY);
        ModelNode addIdentity = Util.createAddOperation(address);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case OUTFLOW_SECURITY_DOMAINS: {
                    for (String outflowDomain : reader.getListAttributeValue(i)) {
                        IdentityResourceDefinition.OUTFLOW_SECURITY_DOMAINS.parseAndAddParameterElement(outflowDomain, addIdentity, reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }

        }
        requireNoContent(reader);
        operations.add(addIdentity);
    }

    private void parseEnableGracefulTxnShutdown(XMLExtendedStreamReader reader, ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        final EnumSet<EJB3SubsystemXMLAttribute> missingRequiredAttributes = EnumSet.of(EJB3SubsystemXMLAttribute.VALUE);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case VALUE:
                    EJB3SubsystemRootResourceDefinition.ENABLE_GRACEFUL_TXN_SHUTDOWN.parseAndSetParameter(value, ejb3SubsystemAddOperation, reader);
                    // found the mandatory attribute
                    missingRequiredAttributes.remove(EJB3SubsystemXMLAttribute.VALUE);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);
        if (!missingRequiredAttributes.isEmpty()) {
            throw missingRequired(reader, missingRequiredAttributes);
        }
    }

    protected void parseProfile(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String profileName = null;

        final EJBClientContext.Builder builder = new EJBClientContext.Builder();

        final ModelNode operation = Util.createAddOperation();

        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                    profileName = value;
                    break;
                case EXCLUDE_LOCAL_RECEIVER:
                    RemotingProfileResourceDefinition.EXCLUDE_LOCAL_RECEIVER.parseAndSetParameter(value, operation, reader);
                    break;
                case LOCAL_RECEIVER_PASS_BY_VALUE:
                    RemotingProfileResourceDefinition.LOCAL_RECEIVER_PASS_BY_VALUE.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (profileName == null) {
            throw missingRequired(reader, Collections.singleton(EJB3SubsystemXMLAttribute.NAME.getLocalName()));
        }


        final PathAddress address = SUBSYSTEM_PATH.append(EJB3SubsystemModel.REMOTING_PROFILE, profileName);
        operation.get(OP_ADDR).set(address.toModelNode());
        operations.add(operation);

        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case STATIC_EJB_DISCOVERY:
                    final ModelNode staticEjb = parseStaticEjbDiscoveryType(reader);
                    operation.get(StaticEJBDiscoveryDefinition.STATIC_EJB_DISCOVERY).set(staticEjb);
                    break;
                case REMOTING_EJB_RECEIVER: {
                    parseRemotingReceiver(reader, address, operations);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private ModelNode parseStaticEjbDiscoveryType(final XMLExtendedStreamReader reader) throws XMLStreamException {
        ModelNode staticDiscovery = new ModelNode();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case MODULE: {
                    final ModelNode ejb = new ModelNode();
                    final int count = reader.getAttributeCount();
                    String uri = null;
                    String module = null;
                    String app = null;
                    String distinct = null;
                    for (int i = 0; i < count; i++) {
                        requireNoNamespaceAttribute(reader, i);
                        final String value = reader.getAttributeValue(i);
                        final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
                        switch (attribute) {
                            case URI:
                                if (uri != null) {
                                    throw unexpectedAttribute(reader, i);
                                }
                                uri = value;
                                StaticEJBDiscoveryDefinition.URI_AD.parseAndSetParameter(uri, ejb, reader);
                                break;
                            case MODULE_NAME:
                                if (module != null) {
                                    throw unexpectedAttribute(reader, i);
                                }
                                module = value;
                                StaticEJBDiscoveryDefinition.MODULE_AD.parseAndSetParameter(module, ejb, reader);
                                break;
                            case APP_NAME:
                                if (app != null) {
                                    throw unexpectedAttribute(reader, i);
                                }
                                app = value;
                                StaticEJBDiscoveryDefinition.APP_AD.parseAndSetParameter(app, ejb, reader);
                                break;
                            case DISTINCT_NAME:
                                if (distinct != null) {
                                    throw unexpectedAttribute(reader, i);
                                }
                                distinct = value;
                                StaticEJBDiscoveryDefinition.DISTINCT_AD.parseAndSetParameter(distinct, ejb, reader);
                                break;
                            default:
                                throw unexpectedAttribute(reader, i);
                        }
                    }
                    if (module == null) {
                        throw missingRequired(reader, Collections.singleton(EJB3SubsystemXMLAttribute.MODULE_NAME.getLocalName()));
                    }
                    if (uri == null) {
                        throw missingRequired(reader, Collections.singleton(URI));
                    }

                    staticDiscovery.add(ejb);

                    requireNoContent(reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return staticDiscovery;
    }

}
