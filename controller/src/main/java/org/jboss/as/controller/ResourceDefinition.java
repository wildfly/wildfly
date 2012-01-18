/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Provides essential information defining a management resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface ResourceDefinition {

    /**
     * Gets the path element that describes how to navigate to this resource from its parent resource, or {@code null}
     * if this is a definition of a root resource.
     *
     * @return the path element, or {@code null} if this is a definition of a root resource.
     */
    PathElement getPathElement();

    /**
     * Gets a {@link DescriptionProvider} for the given resource.
     *
     * @param resourceRegistration  the resource. Cannot be {@code null}
     * @return  the description provider. Will not be {@code null}
     */
    DescriptionProvider getDescriptionProvider(ImmutableManagementResourceRegistration resourceRegistration);

    /**
     * Register operations associated with this resource.
     *
     * @param resourceRegistration a {@link ManagementResourceRegistration} created from this definition
     */
    void registerOperations(final ManagementResourceRegistration resourceRegistration);

    /**
     * Register operations associated with this resource.
     *
     * @param resourceRegistration a {@link ManagementResourceRegistration} created from this definition
     */
    void registerAttributes(final ManagementResourceRegistration resourceRegistration);

    /**
     * Register child resources associated with this resource.
     *
     * @param resourceRegistration a {@link ManagementResourceRegistration} created from this definition
     */
    void registerChildren(final ManagementResourceRegistration resourceRegistration);
}
