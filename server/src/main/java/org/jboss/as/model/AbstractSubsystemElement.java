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

import java.util.List;

import javax.xml.namespace.QName;

/**
 * The base class of all subsystem elements.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractSubsystemElement<E extends AbstractSubsystemElement<E>> extends AbstractModelRootElement<E> {

    private static final long serialVersionUID = 899219830157478004L;

    /**
     * Construct a new instance.
     *
     * @param namespaceUri the namespace URI of this subsystem
     */
    protected AbstractSubsystemElement(final String namespaceUri) {
        super(new QName(namespaceUri, "subsystem"));
    }

    /**
     * Append a list of updates which will yield the same state as this element.  Adding this subsystem
     * followed by executing the updates on the list should result in the exact same configuration that
     * this subsystem currently holds.  Executing the compensating updates on this model in reverse order
     * should result in a configuration for which {@link #isEmpty()} will return true.
     *
     * @param list the list of updates required to duplicate this subsystem
     */
    protected abstract void getUpdates(List<? super AbstractSubsystemUpdate<E, ?>> list);

    /**
     * Determine whether this subsystem is empty (i.e. can be removed).  This method should return {@code true}
     * whenever {@link #getUpdates(java.util.List)} would add no elements to the list.
     *
     * @return {@code true} if this subsystem is empty
     */
    protected abstract boolean isEmpty();

    /**
     * Get the add update which would add this subsystem element.
     *
     * @return the add
     */
    protected abstract AbstractSubsystemAdd<E> getAdd();

    /**
     * Remove this element from a running service container.  The given result handler is called with the result of the
     * removal.
     *
     * @param updateContext the update context
     * @param resultHandler the handler to call back with the result
     * @param param the parameter value to pass to the result handler
     */
    protected abstract <P> void applyRemove(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param);
}
