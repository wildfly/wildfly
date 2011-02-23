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

package org.jboss.as.ee.component;

import org.jboss.msc.service.ServiceName;

/**
 * Used to provide a means to bind a component to a location in JNDI.
 *
 * @author John Bailey
 */
public class ComponentBinding {
    private final Class<?> viewClass;
    private final ServiceName contextServiceName;
    private final String bindName;

    public ComponentBinding(final ServiceName contextServiceName, final String bindName,  final Class<?> viewClass) {
        this.contextServiceName = contextServiceName;
        this.bindName = bindName;
        this.viewClass = viewClass;
    }

    /**
     * Get the service name of context to bind the reference.
     *
     * @return The context service name
     */
    public ServiceName getContextStoreServiceName() {
        return contextServiceName;
    }

    /**
     * Get the name used to bind the reference into the context.
     *
     * @return The bind name
     */
    public String getBindName() {
        return bindName;
    }

    /**
     * Get the view class for this component binding.
     *
     * @return The view class
     */
    public Class<?> getViewClass() {
        return viewClass;
    }
}
