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

/**
 *
 */
package org.jboss.as.model.socket;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.Attribute;
import org.jboss.as.model.Element;
import org.jboss.as.model.Namespace;
import org.jboss.as.model.ParseUtils;
import org.jboss.as.services.net.NetworkInterfaceService;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Based superclass for a named interface definition.
 *
 * @author Brian Stansberry
 */
public abstract class AbstractInterfaceElement<E extends AbstractInterfaceElement<E>> extends AbstractModelElement<E> implements ServiceActivator {

    private static final long serialVersionUID = -5256526713311518506L;
    private static final Logger log = Logger.getLogger("org.jboss.as.socket");

    private final String name;
    private boolean anyLocalV4;
    private boolean anyLocalV6;
    private boolean anyLocal;
    private final NavigableMap<Element, AbstractInterfaceCriteriaElement<?>> interfaceCriteria =
            new TreeMap<Element, AbstractInterfaceCriteriaElement<?>>();

    /**
     * Creates a new AbstractInterfaceElement by parsing an xml stream
     *
     * @param reader stream reader used to the xml
     * @param criteriaRequired <code>true</code> if the element content must
     *         include criteria to identify the IP address to use for the
     *         interface; <code>false</code> if that is not required
     *
     * @throws XMLStreamException if an error occurs
     */
    protected AbstractInterfaceElement(XMLExtendedStreamReader reader, boolean criteriaRequired) throws XMLStreamException {
        super();
        // Handle attributes
        String name = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = value;
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (name == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        this.name = name;
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case ANY_ADDRESS: {
                            validateAnyLocalAllowed(element, reader);
                            ParseUtils.requireNoAttributes(reader);
                            ParseUtils.requireNoContent(reader);
                            anyLocal = true;
                            break;
                        }
                        case ANY_IPV4_ADDRESS: {
                            validateAnyLocalAllowed(element, reader);
                            ParseUtils.requireNoAttributes(reader);
                            ParseUtils.requireNoContent(reader);
                            anyLocalV4 = true;
                            break;
                        }
                        case ANY_IPV6_ADDRESS: {
                            validateAnyLocalAllowed(element, reader);
                            ParseUtils.requireNoAttributes(reader);
                            ParseUtils.requireNoContent(reader);
                            anyLocalV6 = true;
                            break;
                        }
                        case ANY:
                        case NOT:{
                            validateNotAnyLocal(element, reader);
                            CompoundCriteriaElement criteria = new CompoundCriteriaElement(reader, element == Element.ANY);
                            interfaceCriteria.put(criteria.getElement(), criteria);
                            break;
                        }
                        default: {
                            validateNotAnyLocal(element, reader);
                            AbstractInterfaceCriteriaElement<?> criteria = ParsingUtil.parseSimpleInterfaceCriteria(reader, element);
                            interfaceCriteria.put(criteria.getElement(), criteria);
                            break;
                        }
                    }
                    break;
                }
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
        if (criteriaRequired && !anyLocal && !anyLocalV4 && !anyLocalV6 && interfaceCriteria.isEmpty()) {
            throw new XMLStreamException("Either an inet-address element or some other interface criteria element is required", reader.getLocation());
        }
    }

    protected AbstractInterfaceElement(AbstractInterfaceElement<?> toCopy) {
        if (toCopy == null)
            throw new IllegalArgumentException("toCopy is null");
        this.name = toCopy.name;
        this.anyLocal = toCopy.anyLocal;
        this.anyLocalV4 = toCopy.anyLocalV4;
        this.anyLocalV6 = toCopy.anyLocalV6;
        interfaceCriteria.putAll(toCopy.interfaceCriteria);
    }

    /**
     * Gets the name of the interface
     *
     * @return the interface name. Will not be <code>null</code>
     */
    public String getName() {
        return name;
    }

    public InterfaceCriteria getInterfaceCriteria() {
        return new OverallInterfaceCriteria();
    }

    public boolean isAnyLocalAddress() {
        return anyLocal;
    }

    public boolean isAnyLocalV4Address() {
        return anyLocalV4;
    }

    public boolean isAnyLocalV6Address() {
        return anyLocalV6;
    }

    /**
     * Gets whether this element is configured with necessary information needed
     * to determine an IP address for the interface; either via a directly
     * specified {@link #getAddress() address} or via at least one address
     * selection criteria.
     *
     * @return <code>true</code> if the necessary information is available, <code>false</code>
     *         otherwise
     */
    public boolean isFullySpecified() {
        return anyLocal || anyLocalV4 || anyLocalV6 || interfaceCriteria.size() > 0;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), name);
        if (anyLocal) {
            streamWriter.writeStartElement(Element.ANY_ADDRESS.getLocalName());
            streamWriter.writeEndElement();
        }
        else if (anyLocalV4) {
            streamWriter.writeStartElement(Element.ANY_IPV4_ADDRESS.getLocalName());
            streamWriter.writeEndElement();

        }
        else if (anyLocalV6) {
            streamWriter.writeStartElement(Element.ANY_IPV6_ADDRESS.getLocalName());
            streamWriter.writeEndElement();
        }
        else if (! interfaceCriteria.isEmpty()) {
            for (AbstractInterfaceCriteriaElement<?> criteria : interfaceCriteria.values()) {
                streamWriter.writeStartElement(criteria.getElement().getLocalName());
                criteria.writeContent(streamWriter);
            }
        }

        streamWriter.writeEndElement();
    }

    @Override
    public void activate(ServiceActivatorContext context) {
        log.info("Activating interface element:" + name);
        context.getBatchBuilder().addService(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(getName()),
                new NetworkInterfaceService(this)).setInitialMode(Mode.ON_DEMAND);
    }

    List<AbstractInterfaceCriteriaElement<?>> getCriteriaElements() {
        synchronized (interfaceCriteria) {
            return new ArrayList<AbstractInterfaceCriteriaElement<?>>(interfaceCriteria.values());
        }
    }

    private void validateAnyLocalAllowed(Element element, XMLExtendedStreamReader reader) throws XMLStreamException {
        if (interfaceCriteria.size() > 0)
            throw new XMLStreamException(element + " cannot be combined with " + interfaceCriteria.keySet().iterator().next(), reader.getLocation());
        validateNotAnyLocal(element, reader);
    }

    private void validateNotAnyLocal(Element element, XMLExtendedStreamReader reader) throws XMLStreamException {
        if (anyLocal)
            throw new XMLStreamException(element + " cannot be combined with " + Element.ANY_ADDRESS.getLocalName(), reader.getLocation());
        if (anyLocalV4)
            throw new XMLStreamException(element + " cannot be combined with " + Element.ANY_IPV4_ADDRESS.getLocalName(), reader.getLocation());
        if (anyLocalV6)
            throw new XMLStreamException(element + " cannot be combined with " + Element.ANY_IPV6_ADDRESS.getLocalName(), reader.getLocation());
    }

    private class OverallInterfaceCriteria implements InterfaceCriteria {

        private static final long serialVersionUID = 2784447904647077246L;

        @Override
        public boolean isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {

            for (AbstractInterfaceCriteriaElement<?> criteria : interfaceCriteria.values()) {
                if (! criteria.getInterfaceCriteria().isAcceptable(networkInterface, address))
                    return false;
            }
            return true;
        }

    }

}
