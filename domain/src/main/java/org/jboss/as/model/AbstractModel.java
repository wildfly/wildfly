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

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractModel<E extends AbstractModel<E>> extends AbstractModelElement<E> {

    private static final long serialVersionUID = 66064050420378211L;

    /**
     * The complete set of elements within this domain.
     */
    private final Set<AbstractModelElement<?>> elements = Collections.newSetFromMap(new IdentityHashMap<AbstractModelElement<?>, Boolean>());
    /**
     * An index of elements which have an associated ID.
     */
    private transient final Map<String, AbstractModelElement<?>> elementsById = new HashMap<String, AbstractModelElement<?>>();

    protected AbstractModel(final String id) {
        super(id);
    }

    // Protected members

    /**
     * Add an element to the model.  Called under a lock.  Should be called at the start of an element-specific
     * add operation.
     *
     * @param element the element to add
     */
    protected final void addElement(AbstractModelElement<?> element) {
        final String id = element.getId();
        if (id != null) {
            final Map<String, AbstractModelElement<?>> elementsById = this.elementsById;
            if (elementsById.containsKey(id)) {
                throw new IllegalArgumentException("Domain already contains an element with ID '" + id + "'");
            }
            elementsById.put(id, element);
        }
        if (! elements.add(element)) {
            if (id != null) {
                elementsById.remove(id);
            }
            throw new IllegalArgumentException("Domain already contains element " + element);
        }
    }

    /**
     * Remove an element from the model.  Called under a lock.  Should be called at the start of an element-specific
     * remove operation.
     *
     * @param element the element to remove
     * @return {@code true} if the element was found within the model
     */
    protected final boolean removeElement(AbstractModelElement<?> element) {
        if (! elements.remove(element)) {
            return false;
        }
        assert elementsById.get(element.getId()) == element;
        elementsById.remove(element.getId());
        return true;
    }

    /**
     * Get an element by ID.  Called under a lock.  If no such element exists, {@code null} is returned.
     *
     * @param id the ID
     * @return the element, or {@code null} if it is missing
     */
    protected final AbstractModelElement<?> getElementById(String id) {
        return elementsById.get(id);
    }
}
