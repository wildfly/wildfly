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

import org.jboss.msc.service.Location;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

/**
 * A root element in the model.  A root element has a fixed name.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractModelRootElement<E extends AbstractModelRootElement<E>> extends AbstractModelElement<E> {

    private final String namespace;

    /**
     * Construct a new instance.
     *
     * @param location the declaration location of this model root element
     * @param namespace
     */
    protected AbstractModelRootElement(final Location location, final String namespace) {
        super(location);
        this.namespace = namespace;
    }

    protected AbstractModelRootElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        namespace = reader.getNamespaceURI();
    }

    /**
     * Get the name of this root element.
     *
     * @return the name
     */
    protected abstract QName getElementName();

    /**
     * Get the namespace that this root element was created in.
     *
     * @return the namespace
     */
    public final String getNamespace() {
        return namespace;
    }
}
