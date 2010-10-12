/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector;

import java.util.EnumSet;
import org.jboss.as.ExtensionContext;
import org.jboss.as.model.ParseResult;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamException;

import static org.jboss.as.model.ParseUtils.*;
import static javax.xml.stream.XMLStreamConstants.*;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ConnectorSubsystemElementParser implements
        XMLElementReader<ParseResult<ExtensionContext.SubsystemConfiguration<ConnectorSubsystemElement>>> {

    public void readElement(final XMLExtendedStreamReader reader,
            final ParseResult<ExtensionContext.SubsystemConfiguration<ConnectorSubsystemElement>> result)
            throws XMLStreamException {
        final ConnectorSubsystemAdd add = new ConnectorSubsystemAdd();

        final EnumSet<Element> visited = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case CONNECTOR_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (!visited.add(element)) {
                        throw unexpectedElement(reader);
                    }
                    switch (element) {
                        case ARCHIVE_VALIDATION: {
                            final int cnt = reader.getAttributeCount();
                            for (int i = 0; i < cnt; i++) {
                                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                                switch (attribute) {
                                    case ENABLED: {
                                        add.setArchiveValidation(Boolean.parseBoolean(reader.getAttributeValue(i)));
                                        break;
                                    }
                                    case FAIL_ON_ERROR: {
                                        add.setArchiveValidationFailOnError(Boolean.parseBoolean(reader.getAttributeValue(i)));
                                        break;
                                    }
                                    case FAIL_ON_WARN: {
                                        add.setArchiveValidationFailOnWarn(Boolean.parseBoolean(reader.getAttributeValue(i)));
                                        break;
                                    }
                                    default: {
                                        throw unexpectedAttribute(reader, i);
                                    }
                                }
                            }
                            requireNoContent(reader);
                            break;
                        }
                        case BEAN_VALIDATION: {
                            final boolean enabled = readBooleanAttributeElement(reader, Attribute.ENABLED.getLocalName());
                            add.setBeanValidation(enabled);
                            requireNoContent(reader);
                            break;
                        }
                        default:
                            throw unexpectedElement(reader);
                    }
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }

        result.setResult(new ExtensionContext.SubsystemConfiguration<ConnectorSubsystemElement>(add));
    }
}
