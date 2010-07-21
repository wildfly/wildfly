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

public final class DeploymentUnitElement extends AbstractModelElement<DeploymentUnitElement> {

    private static final long serialVersionUID = 5335163070198512362L;

    private final DeploymentUnitKey key;
    private final boolean allowed;
    private final boolean start;

    public DeploymentUnitElement(final Location location, final String fileName, 
            final byte[] sha1Hash, final boolean allowed, final boolean start) {
        super(location);
        this.key = new DeploymentUnitKey(fileName, sha1Hash);
        this.allowed = allowed;
        this.start = start;
    }
    
    public DeploymentUnitElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        // Handle attributes
        String fileName = null;
        String sha1Hash = null;
        String allowed = null;
        String start = null;
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
                    case ALLOWED: {
                        allowed = value;
                        break;
                    }
                    case START: {
                        start = value;
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
        this.key = new DeploymentUnitKey(fileName, sha1Hash.getBytes());
        this.allowed = allowed == null ? true : Boolean.valueOf(allowed);
        this.start = start == null ? true : Boolean.valueOf(start);
        
        // Handle elements
        requireNoContent(reader);
    }
    
    public DeploymentUnitKey getKey() {
        return key;
    }

    public String getName() {
        return key.getName();
    }

    public byte[] getSha1Hash() {
        return key.getSha1Hash();
    }

    public boolean isAllowed() {
        return allowed;
    }

    public boolean isStart() {
        return start;
    }

    public long elementHash() {
        return key.elementHash();
    }

    protected void appendDifference(final Collection<AbstractModelUpdate<DeploymentUnitElement>> target, final DeploymentUnitElement other) {
    }

    protected Class<DeploymentUnitElement> getElementClass() {
        return DeploymentUnitElement.class;
    }

    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        key.writeContent(streamWriter);
        streamWriter.writeEndElement();
    }
}
