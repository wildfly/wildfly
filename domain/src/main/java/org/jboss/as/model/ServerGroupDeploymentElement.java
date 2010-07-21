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

import java.util.Collection;
import java.util.Collections;

import org.jboss.msc.service.Location;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

/**
 * A deployment which is mapped into a {@link ServerGroupElement}.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServerGroupDeploymentElement extends AbstractModelElement<ServerGroupDeploymentElement> {
    private static final long serialVersionUID = -7282640684801436543L;

    private final String deploymentName;
    private final byte[] deploymentHash;
    // todo: deployment overrides

    /**
     * Construct a new instance.
     *
     * @param location the declaration location of this element
     * @param deploymentName the name of the deployment unit
     * @param deploymentHash the hash of the deployment unit
     */
    public ServerGroupDeploymentElement(final Location location, final String deploymentName, final byte[] deploymentHash) {
        super(location);
        if (deploymentName == null) {
            throw new IllegalArgumentException("deploymentName is null");
        }
        if (deploymentHash == null) {
            throw new IllegalArgumentException("deploymentHash is null");
        }
        if (deploymentHash.length != 20) {
            throw new IllegalArgumentException("deploymentHash is not a valid length");
        }
        this.deploymentName = deploymentName;
        this.deploymentHash = deploymentHash;
    }
    
    public ServerGroupDeploymentElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        // Handle attributes
        String fileName = null;
        String sha1Hash = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        fileName = value;
                        break;
                    }
                    case SHA1: {
                        sha1Hash = value;
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (fileName == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        if (sha1Hash == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.SHA1));
        }
        this.deploymentName = fileName;
        this.deploymentHash = sha1Hash.getBytes();
        // Handle elements
        requireNoContent(reader);
    }

    /** {@inheritDoc} */
    public long elementHash() {
        final byte[] hash = deploymentHash;
        return deploymentName.hashCode() & 0xFFFFFFFFL ^ calculateElementHashOf(hash);
    }

    /** {@inheritDoc} */
    protected void appendDifference(final Collection<AbstractModelUpdate<ServerGroupDeploymentElement>> target, final ServerGroupDeploymentElement other) {
    }

    /** {@inheritDoc} */
    protected Class<ServerGroupDeploymentElement> getElementClass() {
        return ServerGroupDeploymentElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
    }
}
