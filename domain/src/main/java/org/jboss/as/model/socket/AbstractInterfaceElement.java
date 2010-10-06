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
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.Attribute;
import org.jboss.as.model.Element;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Based superclass for a named interface definition.
 *
 * @author Brian Stansberry
 */
public abstract class AbstractInterfaceElement<E extends AbstractInterfaceElement<E>> extends AbstractModelElement<E> {

    private static final long serialVersionUID = -5256526713311518506L;

    private final String name;
    private boolean anyLocalV4;
    private boolean anyLocalV6;
    private boolean anyLocal;
    private final NavigableMap<Element, AbstractInterfaceCriteriaElement<?>> interfaceCriteria =
            new TreeMap<Element, AbstractInterfaceCriteriaElement<?>>();


    protected AbstractInterfaceElement(final String name) {
        this.name = name;
    }

    protected AbstractInterfaceElement(String name, List<AbstractInterfaceCriteriaElement<?>> criteriaList) {
        if (name == null)
            throw new IllegalArgumentException("name is null");
        if (criteriaList == null)
            throw new IllegalArgumentException("criteria is null");
        this.name = name;
        for (AbstractInterfaceCriteriaElement<?> criteria : criteriaList) {
            interfaceCriteria.put(criteria.getElement(), criteria);
        }
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

    void setAnyLocal(boolean anyLocal) {
        this.anyLocal = anyLocal;
    }

    public boolean isAnyLocalV4Address() {
        return anyLocalV4;
    }

    void setAnyLocalV4(boolean anyLocalV4) {
        this.anyLocalV4 = anyLocalV4;
    }

    public boolean isAnyLocalV6Address() {
        return anyLocalV6;
    }

    void setAnyLocalV6(boolean anyLocalV6) {
        this.anyLocalV6 = anyLocalV6;
    }

    protected void addCriteria(AbstractInterfaceCriteriaElement<?> criteria) {
        this.interfaceCriteria.put(criteria.getElement(), criteria);
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

    List<AbstractInterfaceCriteriaElement<?>> getCriteriaElements() {
        synchronized (interfaceCriteria) {
            return new ArrayList<AbstractInterfaceCriteriaElement<?>>(interfaceCriteria.values());
        }
    }

    protected void validateAnyLocalAllowed(Element element) throws IllegalStateException {
        if (interfaceCriteria.size() > 0) {
            throw new IllegalStateException(element + " cannot be combined with " + interfaceCriteria.keySet().iterator().next());
        }
        validateNotAnyLocal(element);
    }

    private void validateNotAnyLocal(Element element) throws IllegalStateException {
        if (anyLocal)
            throw new IllegalStateException(element + " cannot be combined with " + Element.ANY_ADDRESS.getLocalName());
        if (anyLocalV4)
            throw new IllegalStateException(element + " cannot be combined with " + Element.ANY_IPV4_ADDRESS.getLocalName());
        if (anyLocalV6)
            throw new IllegalStateException(element + " cannot be combined with " + Element.ANY_IPV6_ADDRESS.getLocalName());
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
