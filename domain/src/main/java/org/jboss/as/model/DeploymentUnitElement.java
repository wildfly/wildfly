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

import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceName;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;
import java.util.Collection;
import java.util.Collections;

/**
 * A deployment that is known to the domain.
 *
 * @author Brian Stansberry
 * @author John E. Bailey
 */
public final class DeploymentUnitElement extends AbstractModelElement<DeploymentUnitElement> implements ServiceActivator {

    private static final long serialVersionUID = 5335163070198512362L;
    private static final ServiceName MOUNT_SERVICE_NAME = ServiceName.JBOSS.append("mounts");

    private final DeploymentUnitKey key;
    private boolean allowed;
    private boolean start;

    public DeploymentUnitElement(final String fileName,
                                 final byte[] sha1Hash, final boolean allowed, final boolean start) {
        this.key = new DeploymentUnitKey(fileName, sha1Hash);
        this.allowed = allowed;
        this.start = start;
    }

    public DeploymentUnitElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        // Handle attributes
        String fileName = null;
        byte[] sha1Hash = null;
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
                        try {
                            sha1Hash = hexStringToByteArray(value);
                        }
                        catch (Exception e) {
                           throw new XMLStreamException("Value " + value +
                                   " for attribute " + attribute.getLocalName() +
                                   " does not represent a properly hex-encoded SHA1 hash",
                                   reader.getLocation(), e);
                        }
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
        this.key = new DeploymentUnitKey(fileName, sha1Hash);
        this.allowed = allowed == null ? true : Boolean.valueOf(allowed);
        this.start = start == null ? true : Boolean.valueOf(start);

        // Handle elements
        requireNoContent(reader);
    }

    /**
     * Gets the identifier of this deployment that's suitable for use as a map key.
     * @return the key
     */
    public DeploymentUnitKey getKey() {
        return key;
    }

    /**
     * Gets the name of the deployment.
     *
     * @return the name
     */
    public String getName() {
        return key.getName();
    }

    /**
     * Gets a defensive copy of the sha1 hash of the deployment.
     *
     * @return the hash
     */
    public byte[] getSha1Hash() {
        return key.getSha1Hash();
    }

    /**
     * Gets whether the deployment should be started upon server start.
     *
     * @return <code>true</code> if the deployment should be started; <code>false</code>
     *         if not.
     */
    public boolean isStart() {
        return start;
    }

    /**
     * Sets whether the deployments should be started upon server start.
     * @param start <code>true</code> if the deployment should be started; <code>false</code>
     *         if not.
     */
    void setStart(boolean start) {
        this.start = start;
    }

    /**
     * Gets whether the deployment can be mapped to a server group; i.e. made
     * available to servers.
     *
     * @return <code>true</code> if the deployment can be mapped; <code>false</code>
     *         if not.
     */
    public boolean isAllowed() {
        return allowed;
    }

    /**
     * Sets whether the deployment can be mapped to a server group; i.e. made
     * available to servers.
     *
     * @param allowed <code>true</code> if the deployment can be mapped; <code>false</code>
     *         if not.
     */
    void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    @Override
    public void activate(ServiceActivatorContext serviceActivatorContext) {
    }

    public long elementHash() {
        long hash = key.elementHash();
        hash = Long.rotateLeft(hash, 1) ^ Boolean.valueOf(start).hashCode() & 0xffffffffL;
        hash = Long.rotateLeft(hash, 1) ^ Boolean.valueOf(allowed).hashCode() & 0xffffffffL;
        return hash;
    }

    protected void appendDifference(final Collection<AbstractModelUpdate<DeploymentUnitElement>> target, final DeploymentUnitElement other) {
        // FIXME implement appendDifference
        throw new UnsupportedOperationException("implement me");
    }

    protected Class<DeploymentUnitElement> getElementClass() {
        return DeploymentUnitElement.class;
    }

    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), key.getName());
        streamWriter.writeAttribute(Attribute.SHA1.getLocalName(), key.getSha1HashAsHexString());
        streamWriter.writeEndElement();
    }
}
