/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * A server group within a {@link Domain}.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServerGroupElement extends AbstractModelElement<ServerGroupElement> {

    private static final long serialVersionUID = 3780369374145922407L;

    private final String name;
    private final Map<String, ServerGroupDeploymentElement> deploymentMappings = new TreeMap<String, ServerGroupDeploymentElement>();

    public ServerGroupElement(final String name) {
        this.name = name;
    }

    /** {@inheritDoc} */
    public long elementHash() {
        long cksum = name.hashCode() & 0xffffffffL;
        for (ServerGroupDeploymentElement deployment : deploymentMappings.values()) {
            cksum = Long.rotateLeft(cksum, 1) ^ deployment.elementHash();
        }
        return cksum;
    }

    /** {@inheritDoc} */
    protected void appendDifference(final Collection<AbstractModelUpdate<ServerGroupElement>> target, final ServerGroupElement other) {
    }

    /** {@inheritDoc} */
    protected Class<ServerGroupElement> getElementClass() {
        return ServerGroupElement.class;
    }

    /** {@inheritDoc} */
    public boolean isSameElement(final ServerGroupElement other) {
        return (name.equals(other.name));
    }

    /** {@inheritDoc} */
    public void writeContent(final XMLStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), name);
        // todo write content
        streamWriter.writeEndElement();
    }
}
