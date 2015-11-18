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

import static org.jboss.as.controller.parsing.ParseUtils.duplicateNamedElement;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequiredElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_SECURITY_DOMAIN;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.SECURITY_DOMAINS;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

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
            case SECURITY_DOMAINS: {
                parseSecurityDomains(reader, ejb3SubsystemAddOperation);
                break;
            }
            case DEFAULT_SECURITY_DOMAIN: {
                if ((ejb3SubsystemAddOperation.hasDefined(SECURITY_DOMAINS)) && (ejb3SubsystemAddOperation.hasDefined(DEFAULT_SECURITY_DOMAIN))) {
                    // Already defined using the security-domains default attribute
                    throw unexpectedElement(reader);
                }
                parseDefaultSecurityDomain(reader, ejb3SubsystemAddOperation);
                break;
            }
            default: {
                super.readElement(reader, element, operations, ejb3SubsystemAddOperation);
            }
        }
    }

    private void parseSecurityDomains(final XMLExtendedStreamReader reader, final ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String attributeValue = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DEFAULT: {
                    if (ejb3SubsystemAddOperation.hasDefined(DEFAULT_SECURITY_DOMAIN)) {
                        // Already defined using the default-security-domain element
                        throw unexpectedAttribute(reader, i);
                    }
                    EJB3SubsystemRootResourceDefinition.DEFAULT_SECURITY_DOMAIN.parseAndSetParameter(attributeValue, ejb3SubsystemAddOperation, reader);
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }
        final Set<String> securityDomainNames = new HashSet<String>();
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case SECURITY_DOMAIN: {
                    parseSecurityDomain(reader, ejb3SubsystemAddOperation, securityDomainNames);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if (securityDomainNames.isEmpty()) {
            throw missingRequiredElement(reader, Collections.singleton(EJB3SubsystemXMLElement.SECURITY_DOMAIN.getLocalName()));
        }
    }

    private void parseSecurityDomain(final XMLExtendedStreamReader reader, final ModelNode ejb3SubsystemAddOperation,
                                     final Set<String> securityDomainNames) throws XMLStreamException {
        String securityDomainName = null;
        ModelNode securityDomain = new ModelNode();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String attributeValue = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                    securityDomainName = attributeValue;
                    if (! securityDomainNames.add(securityDomainName)) {
                        throw duplicateNamedElement(reader, securityDomainName);
                    }
                    EJB3SubsystemRootResourceDefinition.SECURITY_DOMAIN_NAME.parseAndSetParameter(securityDomainName, securityDomain, reader);
                    break;
                case ALIAS:
                    EJB3SubsystemRootResourceDefinition.SECURITY_DOMAIN_ALIAS.parseAndSetParameter(attributeValue, securityDomain, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (securityDomainName == null) {
            throw missingRequired(reader, Collections.singleton(EJB3SubsystemXMLAttribute.NAME.getLocalName()));
        }
        requireNoContent(reader);
        ejb3SubsystemAddOperation.get(SECURITY_DOMAINS).add(securityDomain);
    }
}
