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

import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

/**
 * An locally configured domain controller on a {@link HostModel}.
 *
 * @author Brian Stansberry
 */
public final class LocalDomainControllerElement extends AbstractModelElement<LocalDomainControllerElement> {

    private static final long serialVersionUID = 7667892965813702351L;

    /**
     * Construct a new instance.
     *
     */
    public LocalDomainControllerElement() {
    }

    /**
     * Construct a new instance.
     *
     * @param reader the reader from which to build this element
     * @throws XMLStreamException if an error occurs
     */
    public LocalDomainControllerElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super();
        requireNoContent(reader);
    }

    /** {@inheritDoc} */
    public long elementHash() {
        return 42;
    }

    /** {@inheritDoc} */
    protected Class<LocalDomainControllerElement> getElementClass() {
        return LocalDomainControllerElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeEndElement();
    }
}
