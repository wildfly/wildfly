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
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

public final class DeploymentUnitElement extends AbstractModelElement<DeploymentUnitElement> {

    private static final long serialVersionUID = 5335163070198512362L;

    private final String fileName;
    private final byte[] sha1Hash;

    protected DeploymentUnitElement(final String fileName, final byte[] sha1Hash) {
        super(null);
        this.fileName = fileName;
        this.sha1Hash = sha1Hash;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getSha1Hash() {
        return sha1Hash.clone();
    }

    public long elementHash() {
        return fileName.hashCode() & 0xffffffffL ^ calculateElementHashOf(sha1Hash);
    }

    protected void appendDifference(final Collection<AbstractModelUpdate<DeploymentUnitElement>> target, final DeploymentUnitElement other) {
    }

    protected Class<DeploymentUnitElement> getElementClass() {
        return DeploymentUnitElement.class;
    }

    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute("name", fileName);
        streamWriter.writeAttribute("sha1", bytesToHexString(sha1Hash));
        streamWriter.writeEndElement();
    }
}
