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

import java.util.Collection;
import java.util.Collections;

import javax.xml.stream.XMLStreamException;

import org.jboss.msc.service.Location;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A model extension element.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ExtensionElement extends AbstractModelElement<ExtensionElement> {

    private static final long serialVersionUID = -90177370272205647L;

    private final String prefix;
    private final String module;

    /**
     * Construct a new instance.
     *
     * @param location the declaration location of this element
     * @param prefix the extension prefix, if any
     * @param module the module identifier of the subsystem
     */
    public ExtensionElement(final Location location, final String prefix, final String module) {
        super(location);
        this.prefix = prefix;
        this.module = module;
    }
    
    public ExtensionElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        // Handle attributes
        String prefix = null;
        String module = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case PREFIX: {
                        prefix = value;
                        break;
                    }
                    case MODULE: {
                        module = value;
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (prefix == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.PREFIX));
        }
        if (module == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.MODULE));
        }
        this.prefix = prefix;
        this.module = module;
        // Handle elements
        requireNoContent(reader);
    }

    public String getPrefix() {
        return prefix;
    }

    public String getModule() {
        return module;
    }

    /** {@inheritDoc} */
    public long elementHash() {
        final String module = this.module;
        final String prefix = this.prefix;
        long hc = (prefix == null ? 0 : prefix.hashCode() & 0xFFFFFFFFL << 32L) | module.hashCode() & 0xFFFFFFFF;
        return hc;
    }

    /** {@inheritDoc} */
    protected void appendDifference(final Collection<AbstractModelUpdate<ExtensionElement>> target, final ExtensionElement other) {
        // no mutable state
    }

    /** {@inheritDoc} */
    protected Class<ExtensionElement> getElementClass() {
        return ExtensionElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        final String prefix = this.prefix;
        if (prefix != null) streamWriter.writeAttribute(Attribute.PREFIX.getLocalName(), prefix);
        streamWriter.writeAttribute(Attribute.MODULE.getLocalName(), module);
        streamWriter.writeEndElement();
    }
}
