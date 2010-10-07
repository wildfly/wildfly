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

import javax.xml.stream.XMLStreamException;

import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Configuration for a server manager management socket on a {@link org.jboss.as.model.HostModel}.
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
    @Override
    protected Class<ManagementElement> getElementClass() {
        return ManagementElement.class;
    }

    /** {@inheritDoc} */
    @Override
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.INTERFACE.getLocalName(), interfaceName);
        streamWriter.writeAttribute(Attribute.PORT.getLocalName(), Integer.toString(port));
        streamWriter.writeAttribute(Attribute.MAX_THREADS.getLocalName(), Integer.toString(maxThreads));
        streamWriter.writeEndElement();
    }

    void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }
}
