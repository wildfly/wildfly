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
 * A deployment which is mapped into a {@link ServerGroupElement}.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServerGroupDeploymentElement extends AbstractModelElement<ServerGroupDeploymentElement> {
    private static final long serialVersionUID = -7282640684801436543L;



    private final String uniqueName;
    private final String runtimeName;
    private final byte[] sha1Hash;
    private boolean start;

    /**
     * Construct a new instance.
     *
     * @param uniqueName the name of the deployment unit
     * @param runtimeName file name of the deployment unit
     * @param sha1Hash the hash of the deployment unit
     */
    public ServerGroupDeploymentElement(final String uniqueName,
            final String runtimeName,
            final byte[] sha1Hash,
            final boolean start) {
        if (uniqueName == null) {
            throw new IllegalArgumentException("uniqueName is null");
        }
        if (runtimeName == null) {
            throw new IllegalArgumentException("runtimeName is null");
        }
        if (sha1Hash.length != 20) {
            throw new IllegalArgumentException("sha1Hash is not a valid length");
        }
        this.uniqueName = uniqueName;
        this.runtimeName = runtimeName;
        this.sha1Hash = sha1Hash;
        this.start = start;
    }

    /**
     * Gets the unique identifier of this deployment.
     *
     * @return the key
     */
    public String getUniqueName() {
        return uniqueName;
    }

    /**
     * Gets the name by which the deployment is known within a running server.
     *
     * @return the runtime name
     */
    public String getRuntimeName() {
        return runtimeName;
    }

    /**
     * Gets a defensive copy of the sha1 hash of the deployment.
     *
     * @return the hash
     */
    public byte[] getSha1Hash() {
        byte[] copy = new byte[sha1Hash.length];
        System.arraycopy(sha1Hash, 0, copy, 0, sha1Hash.length);
        return copy;
    }

    /**
     * Gets the sha1 hash of the deployment in string form.
     *
     * @return the hash
     */
    public String getSha1HashAsHexString() {
        return bytesToHexString(sha1Hash);
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
     *
     * @param start <code>true</code> if the deployment should be started; <code>false</code>
     *         if not.
     */
    void setStart(boolean start) {
        this.start = start;
    }

    /** {@inheritDoc} */
    @Override
    protected Class<ServerGroupDeploymentElement> getElementClass() {
        return ServerGroupDeploymentElement.class;
    }

    /** {@inheritDoc} */
    @Override
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), uniqueName);
        streamWriter.writeAttribute(Attribute.RUNTIME_NAME.getLocalName(), runtimeName);
        streamWriter.writeAttribute(Attribute.SHA1.getLocalName(), getSha1HashAsHexString());
        if (!this.start) streamWriter.writeAttribute(Attribute.START.getLocalName(), "false");

        streamWriter.writeEndElement();
    }

}
