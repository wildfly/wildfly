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

package org.jboss.as.model;

import java.io.Serializable;
import java.util.Collection;
import org.jboss.staxmapper.XMLContentWriter;

/**
 * @param <E> the concrete model element type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractModelElement<E extends AbstractModelElement<E>> implements Serializable, XMLContentWriter {

    private static final long serialVersionUID = 66064050420378211L;

    private final String id;

    protected AbstractModelElement(final String id) {
        this.id = id;
    }

    /**
     * Calculate a checksum of this model element.  This checksum is used to verify the state of the model
     * after applying a change; it is unlikely (but not guaranteed) to return the same value for two complete model
     * representations that differ by either small or large changes.
     *
     * @return the checksum
     */
    public abstract long checksum();

    /**
     * Get the difference between this model element and another, as a list of updates which, when applied, would make
     * this model element equivalent to the other.
     *
     * @param other the other model element
     * @return the collection of updates
     */
    public abstract Collection<? extends AbstractModelUpdate<?>> getDifference(E other);

    /**
     * Get the XML ID that was declared with this element, if any.
     *
     * @return the ID or {@code null} for none
     */
    public String getId() {
        return id;
    }
}
