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

import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

/**
 * A configuration element for a remote domain controller.
 *
 * @author John E. Bailey
 */
public class RemoteDomainControllerElement extends AbstractModelElement<RemoteDomainControllerElement> {
    private static final long serialVersionUID = -2704285433730705139L;

    private String host;
    private int port;

    public RemoteDomainControllerElement(final String host, final int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    protected Class<RemoteDomainControllerElement> getElementClass() {
        return RemoteDomainControllerElement.class;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.HOST.getLocalName(), host);
        streamWriter.writeAttribute(Attribute.PORT.getLocalName(), Integer.toString(port));
        streamWriter.writeEndElement();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
