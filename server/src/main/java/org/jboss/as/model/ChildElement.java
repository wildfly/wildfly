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

import java.io.Serializable;

/**
 * A child element of an {@link org.jboss.as.model.AbstractModelElement}.  Since an {@code AbstractModelElement}
 * generally corresponds to an XML type, and a type may be attached to more than one possible child element under
 * a different name (and thus with different semantics), this mechanism is provided to associate a local name with such
 * an element.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ChildElement<E extends AbstractModelElement<?>> implements Serializable {
    private static final long serialVersionUID = 7293514039091602017L;

    private final String localName;
    private final E element;

    /**
     * Construct a new instance.
     *
     * @param localName the element local name
     * @param element the element
     */
    public ChildElement(final String localName, final E element) {
        if (localName == null) {
            throw new IllegalArgumentException("localName is null");
        }
        if (element == null) {
            throw new IllegalArgumentException("element is null");
        }
        this.localName = localName;
        this.element = element;
    }

    /**
     * Get the element local name.
     *
     * @return the local name
     */
    public String getLocalName() {
        return localName;
    }

    /**
     * Get the element.
     *
     * @return the element
     */
    public E getElement() {
        return element;
    }
}
