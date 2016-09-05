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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
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
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parser for ejb3:5.0 namespace.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class EJB3Subsystem50Parser extends EJB3Subsystem40Parser {

    public static final EJB3Subsystem50Parser INSTANCE = new EJB3Subsystem50Parser();

    protected EJB3Subsystem50Parser() {
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
}
