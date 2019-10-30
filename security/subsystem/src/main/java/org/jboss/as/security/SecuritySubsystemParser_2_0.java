/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.invalidAttributeValue;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.security.Constants.ELYTRON_KEY_MANAGER;
import static org.jboss.as.security.Constants.ELYTRON_KEY_STORE;
import static org.jboss.as.security.Constants.ELYTRON_REALM;
import static org.jboss.as.security.Constants.ELYTRON_TRUST_MANAGER;
import static org.jboss.as.security.Constants.ELYTRON_TRUST_STORE;
import static org.jboss.as.security.elytron.ElytronIntegrationResourceDefinitions.APPLY_ROLE_MAPPERS;
import static org.jboss.as.security.elytron.ElytronIntegrationResourceDefinitions.LEGACY_JAAS_CONFIG;
import static org.jboss.as.security.elytron.ElytronIntegrationResourceDefinitions.LEGACY_JSSE_CONFIG;

import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * This class implements a parser for the 2.0 version of legacy security subsystem. It extends the {@link SecuritySubsystemParser}
 * and adds support for the {@code elytron-integration} section of the schema.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class SecuritySubsystemParser_2_0 extends SecuritySubsystemParser {

    protected SecuritySubsystemParser_2_0() {
    }

    @Override
    protected void readElement(final XMLExtendedStreamReader reader, final Element element, final List<ModelNode> operations,
                               final PathAddress subsystemPath, final ModelNode subsystemNode) throws XMLStreamException {

        switch(element) {
            case ELYTRON_INTEGRATION: {
                requireNoAttributes(reader);
                while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                    final Element innerElement = Element.forName(reader.getLocalName());
                    switch (innerElement) {
                        case SECURITY_REALMS: {
                            parseSecurityRealms(reader, operations, subsystemPath);
                            break;
                        }
                        case TLS: {
                            parseTLS(reader, operations, subsystemPath);
                            break;
                        }
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
                }
                break;
            }
            default: {
                super.readElement(reader, element, operations, subsystemPath, subsystemNode);
            }
        }
    }

    protected void parseSecurityRealms(final XMLExtendedStreamReader reader, final List<ModelNode> operations,
                                       final PathAddress subsystemPath) throws XMLStreamException {

        requireNoAttributes(reader);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ELYTRON_REALM: {
                    parseElytronRealm(reader, operations, subsystemPath);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    protected void parseElytronRealm(final XMLExtendedStreamReader reader, final List<ModelNode> operations,
                                     final PathAddress subsystemPath) throws XMLStreamException {

        final ModelNode elytronRealmAddOperation = Util.createAddOperation();
        PathElement elytronRealmPath = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.LEGACY_JAAS_CONFIG);
        final int count = reader.getAttributeCount();

        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME: {
                    if (value == null || value.length() == 0) {
                        throw invalidAttributeValue(reader, i);
                    }
                    elytronRealmPath = PathElement.pathElement(ELYTRON_REALM, value);
                    break;
                }
                case LEGACY_JAAS_CONFIG: {
                    LEGACY_JAAS_CONFIG.parseAndSetParameter(value, elytronRealmAddOperation, reader);
                    break;
                }
                case APPLY_ROLE_MAPPERS: {
                    APPLY_ROLE_MAPPERS.parseAndSetParameter(value, elytronRealmAddOperation, reader);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (required.size() > 0) {
            throw missingRequired(reader, required);
        }

        elytronRealmAddOperation.get(OP_ADDR).set(subsystemPath.append(elytronRealmPath).toModelNode());
        operations.add(elytronRealmAddOperation);
        requireNoContent(reader);
    }

    protected void parseTLS(final XMLExtendedStreamReader reader, final List<ModelNode> operations,
                            final PathAddress subsystemPath) throws XMLStreamException {

        requireNoAttributes(reader);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ELYTRON_KEY_STORE: {
                    parseTLSEntity(reader, operations, subsystemPath, ELYTRON_KEY_STORE);
                    break;
                }
                case ELYTRON_TRUST_STORE: {
                    parseTLSEntity(reader, operations, subsystemPath, ELYTRON_TRUST_STORE);
                    break;
                }
                case ELYTRON_KEY_MANAGER: {
                    parseTLSEntity(reader, operations, subsystemPath, ELYTRON_KEY_MANAGER);
                    break;
                }
                case ELYTRON_TRUST_MANAGER: {
                    parseTLSEntity(reader, operations, subsystemPath, ELYTRON_TRUST_MANAGER);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    protected void parseTLSEntity(final XMLExtendedStreamReader reader, final List<ModelNode> operations,
                                  final PathAddress subsystemPath, final String tlsEntityName) throws XMLStreamException {

        final ModelNode elytronTLSEntityAddOperation = Util.createAddOperation();
        PathElement elytronTLSEntityPath = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.LEGACY_JSSE_CONFIG);
        final int count = reader.getAttributeCount();

        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME: {
                    if (value == null || value.length() == 0) {
                        throw invalidAttributeValue(reader, i);
                    }
                    elytronTLSEntityPath = PathElement.pathElement(tlsEntityName, value);
                    break;
                }
                case LEGACY_JSSE_CONFIG: {
                    LEGACY_JSSE_CONFIG.parseAndSetParameter(value, elytronTLSEntityAddOperation, reader);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (required.size() > 0) {
            throw missingRequired(reader, required);
        }

        elytronTLSEntityAddOperation.get(OP_ADDR).set(subsystemPath.append(elytronTLSEntityPath).toModelNode());
        operations.add(elytronTLSEntityAddOperation);
        requireNoContent(reader);
    }
}