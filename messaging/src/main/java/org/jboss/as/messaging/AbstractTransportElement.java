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

package org.jboss.as.messaging;

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.hornetq.api.core.TransportConfiguration;
import org.jboss.as.model.AbstractModelElement;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Emanuel Muckenhuber
 */
public abstract class AbstractTransportElement<E extends AbstractTransportElement<E>> extends AbstractModelElement<E> {

    private static final long serialVersionUID = -2921301251096974402L;
    private final Element element;
    private final String name;
    private String socketBindingRef;
    private Map<String, Object> params = new HashMap<String, Object>();

    protected AbstractTransportElement(Element element, String name) {
        this.element = element;
        this.name = name;
    }

    public abstract String getFactoryClassName();

    Element getElement() {
        return element;
    }

    public String getName() {
        return name;
    }

    public String getSocketBindingRef() {
        return socketBindingRef;
    }

    public void setSocketBindingRef(String socketBindingRef) {
        this.socketBindingRef = socketBindingRef;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(final Map<String, Object> params) {
        this.params = params;
    }

    protected void processHQConfig(final TransportConfiguration transport) {
        if(socketBindingRef != null) {
            transport.getParams().put("socket-ref", socketBindingRef);
        }
    }

    /** {@inheritDoc} */
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        if(name != null) streamWriter.writeAttribute(Attribute.NAME.getLocalName(), name);
        if(socketBindingRef != null) streamWriter.writeAttribute(Attribute.SOCKET_BINDING.getLocalName(), socketBindingRef);
        if (params != null && ! params.isEmpty()) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                streamWriter.writeEmptyElement(Element.PARAM.getLocalName());
                streamWriter.writeAttribute(Attribute.KEY.getLocalName(), entry.getKey());
                streamWriter.writeAttribute(Attribute.VALUE.getLocalName(), entry.getValue().toString());
            }
        }
        streamWriter.writeEndElement();
    }

}
