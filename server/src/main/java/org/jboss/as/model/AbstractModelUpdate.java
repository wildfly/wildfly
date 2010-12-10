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


/**
 * An update to an element in the model.
 *
 * @param <E> the element type that this update applies to
 * @param <R> the type of result that is returned by this update type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractModelUpdate<E extends AbstractModelElement<E>, R> extends AbstractModelElementUpdate<E> {

    private static final long serialVersionUID = -46837337005143198L;

    protected AbstractModelUpdate() {
    }

    /**
     * Get the server model update which corresponds to this model update, if any.
     *
     * @return the server model update
     */
    protected abstract AbstractServerModelUpdate<R> getServerModelUpdate();

    /** {@inheritDoc} */
    @Override
    public abstract AbstractModelUpdate<E, ?> getCompensatingUpdate(E original);
}
