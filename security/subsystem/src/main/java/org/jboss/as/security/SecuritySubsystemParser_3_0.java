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
import static org.jboss.as.security.Constants.ELYTRON_REALM;

import java.util.EnumSet;
import java.util.List;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.security.elytron.ElytronRealmResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * This class implements a parser for the 3.0 version of legacy security subsystem. It extends the {@link SecuritySubsystemParser}
 * and adds support for the {@code elytron-integration} section of the schema.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class SecuritySubsystemParser_3_0 extends SecuritySubsystemParser {

    public static final SecuritySubsystemParser_3_0 INSTANCE = new SecuritySubsystemParser_3_0();

    protected SecuritySubsystemParser_3_0() {
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
                        case ElYTRON_REALM: {
                            parseElytronRealm(reader, operations, subsystemPath);
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

    protected void parseElytronRealm(final XMLExtendedStreamReader reader, final List<ModelNode> operations,
                                     final PathAddress subsystemPath) throws XMLStreamException {

        final ModelNode elytronRealmAddOperation = Util.createAddOperation();
        PathElement elytronRealmPath = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.LEGACY_DOMAIN_NAME);
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
                case LEGACY_DOMAIN_NAME: {
                    ElytronRealmResourceDefinition.LEGACY_DOMAIN_NAME.parseAndSetParameter(value, elytronRealmAddOperation, reader);
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
}