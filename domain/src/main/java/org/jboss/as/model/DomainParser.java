/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.model;

import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLMapper;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

/**
 * A parser which can be sent in to {@link XMLMapper#registerRootElement(QName, XMLElementReader)}
 * for {@code &lt;domain&gt;} root elements.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class DomainParser implements XMLElementReader<ParseResult<Domain>>, XMLStreamConstants {

    public void readElement(final XMLExtendedStreamReader reader, final ParseResult<Domain> value) throws XMLStreamException {
        // read attributes first
        // no required attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            if (reader.getAttributeNamespace(i) != null) {
                reader.handleAttribute(value, i);
            } else {
                switch (Attribute.forName(reader.getAttributeLocalName(i))) {
                    default: throw unexpectedAttribute(reader, i);
                }
            }
        }
        // construct our new domain!
        final Domain domain = new Domain();
        // next, elements
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    // should mean we're done, so ignore it.
                    break;
                }
                case START_ELEMENT: {
                    if (Domain.NAMESPACES.contains(reader.getNamespaceURI())) {
                        switch (Element.forName(reader.getLocalName())) {
                            case SERVER_GROUPS: {
                                readServerGroupsElement(reader, domain);
                                break;
                            }
                            default: throw unexpectedElement(reader);
                        }
                    } else {
                        // handle foreign root elements
                        reader.handleAny(domain);
                    }
                    break;
                }
                default: throw new IllegalStateException();
            }
        }

        // return the result
        value.setResult(domain);
    }

    private void readServerGroupsElement(final XMLExtendedStreamReader reader, final Domain domain) throws XMLStreamException {
        // read attributes first
        // no required attributes
        // todo - simple lists as model elements?
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            if (reader.getAttributeNamespace(i) != null) {
                reader.handleAttribute(domain, i);
            } else {
                switch (Attribute.forName(reader.getAttributeLocalName(i))) {
                    default: throw unexpectedAttribute(reader, i);
                }
            }
        }

    }

    private static XMLStreamException unexpectedElement(final XMLExtendedStreamReader reader) {
        return new XMLStreamException("Unexpected element '" + reader.getName() + "' encountered", reader.getLocation());
    }

    private static XMLStreamException unexpectedAttribute(final XMLExtendedStreamReader reader, final int i) {
        return new XMLStreamException("Unexpected attribute '" + reader.getAttributeName(i) + "' encountered", reader.getLocation());
    }
}
