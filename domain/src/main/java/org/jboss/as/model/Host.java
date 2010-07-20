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
import org.jboss.msc.service.Location;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Host extends AbstractModel<Host> {

    private static final long serialVersionUID = 7667892965813702351L;

    /**
     * Construct a new instance.
     *
     * @param location the declaration location of the host element
     * @param elementName the name of this host element
     */
    public Host(final Location location, final QName elementName) {
        super(location, elementName);
    }

    /**
     * Construct a new instance.
     *
     * @param reader the reader from which to build this element
     * @throws XMLStreamException if an error occurs
     */
    public Host(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
    }

    /** {@inheritDoc} */
    public long elementHash() {
        return 0;
    }

    /** {@inheritDoc} */
    protected void appendDifference(final Collection<AbstractModelUpdate<Host>> target, final Host other) {
        
    }

    /** {@inheritDoc} */
    protected Class<Host> getElementClass() {
        return Host.class;
    }

    /** {@inheritDoc} */
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeEndElement();
    }
}
