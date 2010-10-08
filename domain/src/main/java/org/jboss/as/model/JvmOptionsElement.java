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

package org.jboss.as.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * An element representing a list of jvm options.
 *
 * @author <a href="mailto:kkhan@redhat.com">Kabir Khan</a>
 */
public final class JvmOptionsElement extends AbstractModelElement<JvmOptionsElement> {

    private static final long serialVersionUID = 1614693052895734582L;

    private final List<String> options = new ArrayList<String>();

    /**
     * Construct a new instance.
     *
     */
    public JvmOptionsElement() {
    }

    /**
     * Construct a new instance.
     *
     * @param reader the reader from which to construct this element.
     */
    public JvmOptionsElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // Handle attributes
        ParseUtils.requireNoAttributes(reader);
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (element == Element.OPTION) {
                        // Handle attributes
                        String value = null;
                        int count = reader.getAttributeCount();
                        for (int i = 0; i < count; i++) {
                            final String attrValue = reader.getAttributeValue(i);
                            if (reader.getAttributeNamespace(i) != null) {
                                throw ParseUtils.unexpectedAttribute(reader, i);
                            }
                            else {
                                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                                switch (attribute) {
                                    case VALUE: {
                                        value = attrValue;
                                        break;
                                    }
                                    default:
                                        throw ParseUtils.unexpectedAttribute(reader, i);
                                }
                            }
                        }
                        addOption(value);

                        // Handle elements
                        ParseUtils.requireNoContent(reader);
                    } else {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
        if (options.size() == 0) {
            throw ParseUtils.missingRequiredElement(reader, Collections.singleton(Element.OPTION));
        }
    }

    public JvmOptionsElement(final Element propertyType, boolean allowNullValue, JvmOptionsElement ... toCombine) {
        if (toCombine != null) {
            for (JvmOptionsElement pe : toCombine) {
                if (pe == null)
                    continue;
                for (String value : pe.getOptions()) {
                    addOption(value);
                }
            }
        }
    }

    /** {@inheritDoc} */
    public long elementHash() {
        synchronized (options) {
            return options.hashCode();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Class<JvmOptionsElement> getElementClass() {
        return JvmOptionsElement.class;
    }

    /** {@inheritDoc} */
    @Override
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeStartElement(Element.JVM_OPTIONS.getLocalName());
        synchronized (options) {
            for (String option : options) {
                streamWriter.writeEmptyElement(Element.OPTION.toString());
                streamWriter.writeAttribute(Attribute.VALUE.getLocalName(), option);
            }
        }
        streamWriter.writeEndElement();
    }

    /**
     * Adds an option to the Jvm options
     *
     * @param value the option to add
     */
    void addOption(final String value) {
        synchronized (options) {
            if (value == null) {
                throw new IllegalArgumentException("Value for jvm option is null");
            }
            options.add(value);
        }
    }

    public int size() {
        return options.size();
    }

    /**
     * Get a copy of the options.
     *
     * @return the copy of the options
     */
    public List<String> getOptions() {
        return new ArrayList<String>(options);
    }

}
