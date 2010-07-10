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
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Location;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceContainer;

/**
 * The base class of all container elements.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractContainerElement<E extends AbstractContainerElement<E>> extends AbstractModelRootElement<E> implements ServiceActivator {

    private static final long serialVersionUID = 899219830157478004L;

    /**
     * Construct a new instance.
     *
     * @param location the declaration location of this element
     */
    protected AbstractContainerElement(final Location location) {
        super(location);
    }

    /**
     * Activate this container within a service container.
     *
     * @param container the container
     * @param batchBuilder the current batch builder
     */
    public abstract void activate(final ServiceContainer container, final BatchBuilder batchBuilder);

    /**
     * Get the collection of all socket bindings referenced by this container.  Used to validate
     * the domain model.
     *
     * @return the collection of socket bindings
     */
    public abstract Collection<String> getReferencedSocketBindings();
}
