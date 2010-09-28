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

import java.util.Collections;
import javax.xml.stream.XMLStreamException;

import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Configuration for a server manager management port on a {@link org.jboss.as.model.HostModel}.
 *
 * @author John Bailey
 */
public final class ManagementElement extends AbstractModelElement<ManagementElement> {
    private static final long serialVersionUID = 8470861221364095661L;

    private final String interfaceName;
    private final int port;
    private int maxThreads = 20;

    /**
     * Construct a new instance.
     *
     */
    public ManagementElement(final String interfaceName, final int port) {
        this.interfaceName = interfaceName;
        this.port = port;
    }

    /**
     * Construct a new instance.
     *
     * @param reader the reader from which to build this element
     * @throws javax.xml.stream.XMLStreamException if an error occurs
     */
    public ManagementElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super();
        // Handle attributes
        String interfaceName = null;
        int port = -1;

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case INTERFACE: {
                        interfaceName = value;
                        break;
                    }
                    case PORT: {
                        port = Integer.parseInt(value);
                        break;
                    }
                    case MAX_THREADS: {
                        maxThreads = Integer.parseInt(value);
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if(interfaceName == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.INTERFACE.getLocalName()));
        }
        this.interfaceName = interfaceName;
        this.port = port;

        reader.discardRemainder();
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public int getPort() {
        return port;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    /** {@inheritDoc} */
    private long elementHash() {
        long cksum = interfaceName.hashCode() & 0xffffffffL;
        return cksum;
    }

    /** {@inheritDoc} */
    protected Class<ManagementElement> getElementClass() {
        return ManagementElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.INTERFACE.getLocalName(), interfaceName);
        streamWriter.writeAttribute(Attribute.PORT.getLocalName(), Integer.toString(port));
        streamWriter.writeAttribute(Attribute.MAX_THREADS.getLocalName(), Integer.toString(maxThreads));
        streamWriter.writeEndElement();
    }
}
