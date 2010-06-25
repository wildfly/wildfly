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

package org.jboss.as.domain;

import java.util.Collection;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public abstract class AbstractDomainDeploymentUnit<E extends AbstractDomainDeploymentUnit<E>> extends AbstractDomainDeployment<E> {

    private final String fileName;
    private final byte[] sha1Hash;

    protected AbstractDomainDeploymentUnit(final String name, final String fileName, final byte[] sha1Hash) {
        super(name);
        this.fileName = fileName;
        this.sha1Hash = sha1Hash;
    }

    public Collection<? extends AbstractDomainUpdate<?>> getDifference(final E other) {
        return null;
    }

    public long elementHash() {
        return super.elementHash() ^ Long.rotateLeft(fileName.hashCode() & 0xffffffffL, 32) ^ elementHashOf(sha1Hash);
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getSha1Hash() {
        return sha1Hash.clone();
    }

    public boolean isSameElement(final E other) {
        return false;
    }

    public void writeContent(final XMLStreamWriter streamWriter) throws XMLStreamException {
    }
}
