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

package org.jboss.as.jmx;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Emanuel Muckenhuber
 */
public class JMXConnectorElement extends AbstractModelElement<JMXConnectorElement> {

    private static final long serialVersionUID = 1504432525621055179L;
    private String serverBinding;
    private String registryBinding;

    public JMXConnectorElement(final String serverBinding, final String registryBinding) {
        if(serverBinding == null) {
            throw new IllegalArgumentException("null connector binding");
        }
        if(registryBinding == null) {
            throw new IllegalArgumentException("null registry binding");
        }
        this.serverBinding = serverBinding;
        this.registryBinding = registryBinding;
    }

    public String getServerBinding() {
        return serverBinding;
    }

    public String getRegistryBinding() {
        return registryBinding;
    }

    /** {@inheritDoc} */
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        writeAttributes(streamWriter);
        streamWriter.writeEndElement();
    }

    /**
     * Helper method writing the attributes, without closing the element. Which can be used
     * to write an empty element tag.
     *
     * @param streamWriter
     * @throws XMLStreamException
     */
    void writeAttributes(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.REGISTRY_BINDING.getLocalName(), registryBinding);
        streamWriter.writeAttribute(Attribute.SERVER_BINDING.getLocalName(), serverBinding);
    }

    /** {@inheritDoc} */
    protected Class<JMXConnectorElement> getElementClass() {
        return JMXConnectorElement.class;
    }

}
