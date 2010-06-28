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

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import org.jboss.marshalling.FieldSetter;

/**
 * A controlled object model which is related to an XML representation.  Such an object model can be serialized to
 * XML or to binary.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractModel<E extends AbstractModel<E>> extends AbstractModelElement<E> {

    private static final long serialVersionUID = 66064050420378211L;

    /**
     * The complete set of elements within this model.
     */
    private transient final Set<AbstractModelElement<?>> elements = Collections.newSetFromMap(new IdentityHashMap<AbstractModelElement<?>, Boolean>());

    protected AbstractModel() {
    }

    // Protected members

    /**
     * Add an element to the model.  Called under a lock.  Should be called at the start of an element-specific
     * add operation.
     *
     * @param element the element to add
     */
    protected final void addElement(AbstractModelElement<?> element) {
        if (! elements.add(element)) {
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
        return true;
    }

    // Serialization

    private static final FieldSetter elementsSetter = FieldSetter.get(AbstractModel.class, "elements");

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        final int objectCount = ois.readInt();
        final Set<AbstractModelElement<?>> elements = Collections.newSetFromMap(new IdentityHashMap<AbstractModelElement<?>, Boolean>(objectCount));
        elementsSetter.set(this, elements);
        for (int i = 0; i < objectCount; i ++) {
            deserializeElement((AbstractModelElement<?>) ois.readObject());
        }
    }

    /**
     * Override to perform additional actions upon deserialize.
     *
     * @param element
     * @throws InvalidObjectException
     */
    protected void deserializeElement(AbstractModelElement<?> element) throws InvalidObjectException {
        try {
            addElement(element);
        } catch (IllegalArgumentException e) {
            final InvalidObjectException ioe = new InvalidObjectException("An element in the model is not valid");
            ioe.initCause(e);
            throw ioe;
        }
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        final Set<AbstractModelElement<?>> elements = this.elements;
        oos.writeInt(elements.size());
        for (AbstractModelElement<?> element : elements) {
            oos.writeObject(element);
        }
    }
}
