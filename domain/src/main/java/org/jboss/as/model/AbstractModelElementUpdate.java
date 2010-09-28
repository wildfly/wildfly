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
 * An update to an element in the model.
 *
 * @param <E> the element type that this update applies to
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractModelElementUpdate<E extends AbstractModelElement<E>> implements Serializable {

    private static final long serialVersionUID = -46837337005143198L;

    /**
     * Construct a new instance.
     */
    protected AbstractModelElementUpdate() {
    }

    /**
     * Get the class of the model element that this update type applies to.
     *
     * @return the model element type class
     */
    public abstract Class<E> getModelElementType();

    /**
     * Apply this update to the given model element.
     *
     * @param element the element to which the update should be applied
     * @throws UpdateFailedException if the update failed
     */
    protected abstract void applyUpdate(E element) throws UpdateFailedException;

    /**
     * Get an update which would revert (compensate for) this update.  This method may only be called before the update
     * is applied to the target element. May return {@code null} if the state of
     * {@code original} is such that no compensating update is possible (e.g.
     * if this update is intended to remove something from {@code original}
     * that does not exist).
     *
     * @param original the original element
     * @return the compensating update, or {@code null} if no compensating update
     *           is possible
     */
    public abstract AbstractModelElementUpdate<E> getCompensatingUpdate(E original);
}
