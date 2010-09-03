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
import org.jboss.as.model.socket.SocketBindingGroupRefElement;
import org.jboss.msc.service.Location;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;
import java.util.Collection;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * An individual server on a {@link Host}.
 * 
 * @author Brian Stansberry
 */
public final class LocalDomainControllerElement extends AbstractModelElement<LocalDomainControllerElement> {

    private static final long serialVersionUID = 7667892965813702351L;

    public static final String DEFAULT_NAME = "DomainController";
    
    private final String name;
    private final String serverGroup;
    private final NavigableMap<String, ServerInterfaceElement> interfaces = new TreeMap<String, ServerInterfaceElement>();
    private SocketBindingGroupRefElement bindingGroup;
    private JvmElement jvm;
    /**
     * Construct a new instance.
     *
     * @param location the declaration location of the host element
     */
    public LocalDomainControllerElement(final Location location, final String name, final String serverGroup) {
        super(location);
        this.name = name;
        this.serverGroup = serverGroup;
    }

    /**
     * Construct a new instance.
     *
     * @param reader the reader from which to build this element
     * @throws XMLStreamException if an error occurs
     */
    public LocalDomainControllerElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        // Handle attributes
        String name = null;
        String group = null;
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
//                    case GROUP: {
//                        group = value;
//                        break;
//                    }
                    default: throw unexpectedAttribute(reader, i);
                }
            }
        }
//        if (group == null) {
//            throw missingRequired(reader, Collections.singleton(Attribute.GROUP));
//        }
        this.name = name == null ? DEFAULT_NAME : name;
        this.serverGroup = group;
        
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
                        case SOCKET_BINDING_GROUP: {
                            if (bindingGroup != null) {
                                throw new XMLStreamException(element.getLocalName() + " already defined", reader.getLocation());
                            }
                            bindingGroup = new SocketBindingGroupRefElement(reader);
                            break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                    break;
                }
                default: throw unexpectedElement(reader);
            }
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
     * Gets the name of the server's server group.
     * 
     * @return the server group name. Will not be <code>null</code>
     */
    public String getServerGroup() {
        return serverGroup;
    }

    /** {@inheritDoc} */
    public long elementHash() {
        long cksum = name.hashCode() & 0xffffffffL;
        cksum = Long.rotateLeft(cksum, 1) ^ serverGroup.hashCode() & 0xffffffffL;
        synchronized (interfaces) {
            cksum = calculateElementHashOf(interfaces.values(), cksum);
        }
        if (jvm != null) {
            cksum = Long.rotateLeft(cksum, 1) ^ jvm.elementHash();
        }
        return cksum;
    }

    /** {@inheritDoc} */
    protected void appendDifference(final Collection<AbstractModelUpdate<LocalDomainControllerElement>> target, final LocalDomainControllerElement other) {
        // FIXME implement appendDifference
        throw new UnsupportedOperationException("implement me");
    }

    /** {@inheritDoc} */
    protected Class<LocalDomainControllerElement> getElementClass() {
        return LocalDomainControllerElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), name);
        streamWriter.writeAttribute(Attribute.GROUP.getLocalName(), serverGroup);

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

        if (bindingGroup != null) {
            streamWriter.writeStartElement(Element.SOCKET_BINDING_GROUP.getLocalName());
            bindingGroup.writeContent(streamWriter);
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

    public JvmElement getJvm() {
        return jvm;
    }
}
