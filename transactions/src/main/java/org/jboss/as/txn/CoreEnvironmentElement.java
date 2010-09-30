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

package org.jboss.as.txn;

import org.jboss.as.model.*;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

/**
 * The model element for the core environment element.
 *
 * @author John E. Bailey
 */
public class CoreEnvironmentElement extends AbstractModelElement<CoreEnvironmentElement> {
    private static final long serialVersionUID = 968449300391525909L;
    private String nodeIdentifier = "1";
    private String bindingRef;
    private int maxPorts = 10;

    protected CoreEnvironmentElement() {
        //
    }

    @Override
    protected Class<CoreEnvironmentElement> getElementClass() {
        return CoreEnvironmentElement.class;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.BINDING.getLocalName(), bindingRef);
        streamWriter.writeAttribute(Attribute.SOCKET_PROCESS_ID_MAX_PORTS.getLocalName(), Integer.toString(maxPorts));
        streamWriter.writeAttribute(Attribute.NODE_IDENTIFIER.getLocalName(), nodeIdentifier);
        streamWriter.writeEndElement();
    }

    public String getBindingRef() {
        return bindingRef;
    }


    public void setBindingRef(String bindingRef) {
        this.bindingRef = bindingRef;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    public void setNodeIdentifier(String nodeIdentifier) {
        this.nodeIdentifier = nodeIdentifier;
    }

    public int getMaxPorts() {
        return maxPorts;
    }

    public void setMaxPorts(int maxPorts) {
        this.maxPorts = maxPorts;
    }
}
