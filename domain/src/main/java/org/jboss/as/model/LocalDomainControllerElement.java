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

import org.jboss.as.model.socket.InterfaceElement;
import org.jboss.as.model.socket.ServerInterfaceElement;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

import java.util.Collections;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

/**
 * An locally configured domain controller on a {@link HostModel}.
 *
 * @author Brian Stansberry
 */
public final class LocalDomainControllerElement extends AbstractModelElement<LocalDomainControllerElement> {

    private static final long serialVersionUID = 7667892965813702351L;

    public static final String DEFAULT_NAME = "DomainController";

    private final String name;
    private final String interfaceName;
    private final int port;
    private int maxThreads = 10;
    private final NavigableMap<String, ServerInterfaceElement> interfaces = new TreeMap<String, ServerInterfaceElement>();
    private JvmElement jvm;


    /**
     * Construct a new instance.
     *
     */
    public LocalDomainControllerElement(final String name, final String interfaceName, final int port) {
        this.name = name;
        this.interfaceName = interfaceName;
        this.port = port;
    }

    /**
     * Construct a new instance.
     *
     * @param reader the reader from which to build this element
     * @throws XMLStreamException if an error occurs
     */
    public LocalDomainControllerElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super();
        // Handle attributes
        String name = null;
        String interfaceName = null;
        int port = -1;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = value;
                        break;
                    }
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
                    default: throw unexpectedAttribute(reader, i);
                }
            }
        }
        this.name = name == null ? DEFAULT_NAME : name;
        if(interfaceName == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.INTERFACE.getLocalName()));
        }
        this.interfaceName = interfaceName;
        this.port = port;

        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case INTERFACE_SPECS: {
                            parseInterfaces(reader);
                            break;
                        }
                        case JVM: {
                            if (jvm != null) {
                                throw new XMLStreamException(element.getLocalName() + " already defined", reader.getLocation());
                            }
                            jvm = new JvmElement(reader);
                            break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                    break;
                }
                default: throw unexpectedElement(reader);
            }
        }
        if (jvm == null) {
            throw missingRequiredElement(reader, Collections.singleton(Element.JVM.getLocalName()));
        }
    }

    /**
     * Gets the name of the server.
     *
     * @return the name. Will not be <code>null</code>
     */
    public String getName() {
        return name;
    }

    /**
     * Get the JVM configuration.
     *
     * @return The JVM configuration. Will not be <code>null</code>
     */
    public JvmElement getJvm() {
        return jvm;
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

    public Set<ServerInterfaceElement> getInterfaces() {
        synchronized (interfaces) {
            return new HashSet<ServerInterfaceElement>(interfaces.values());
        }
    }

    /** {@inheritDoc} */
    public long elementHash() {
        long cksum = name.hashCode() & 0xffffffffL;
        synchronized (interfaces) {
            cksum = calculateElementHashOf(interfaces.values(), cksum);
        }
        if (jvm != null) {
            cksum = Long.rotateLeft(cksum, 1) ^ jvm.elementHash();
        }
        return cksum;
    }

    /** {@inheritDoc} */
    protected Class<LocalDomainControllerElement> getElementClass() {
        return LocalDomainControllerElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), name);

        synchronized (interfaces) {
            if (! interfaces.isEmpty()) {
                streamWriter.writeStartElement(Element.INTERFACE_SPECS.getLocalName());
                for (InterfaceElement element : interfaces.values()) {
                    streamWriter.writeStartElement(Element.INTERFACE.getLocalName());
                    element.writeContent(streamWriter);
                }
                streamWriter.writeEndElement();
            }
        }

        if (jvm != null) {
            streamWriter.writeStartElement(Element.JVM.getLocalName());
            jvm.writeContent(streamWriter);
        }
        streamWriter.writeEndElement();
    }

    private void parseInterfaces(XMLExtendedStreamReader reader) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case INTERFACE: {
                            final ServerInterfaceElement interfaceEl = new ServerInterfaceElement(reader);
                            if (interfaces.containsKey(interfaceEl.getName())) {
                                throw new XMLStreamException("Interface " + interfaceEl.getName() + " already declared", reader.getLocation());
                            }
                            interfaces.put(interfaceEl.getName(), interfaceEl);
                            break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                }
                default: throw unexpectedElement(reader);
            }
        }
    }
}
