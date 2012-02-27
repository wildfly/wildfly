/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.alias;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * A {@link ResourceDefinition} for a resource that also has an alias address.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface AliasedResourceDefinition extends ResourceDefinition {

    /**
     * Gets a {@link DescriptionProvider} for the given alias resource.
     *
     * @param resourceRegistration  the resource. Cannot be {@code null}
     * @param alias the resource's alias
     * @return  the description provider. Will not be {@code null}
     */
    DescriptionProvider getAliasDescriptionProvider(ImmutableManagementResourceRegistration resourceRegistration, final PathElement alias);

    /**
     * Register operations associated with an alias resource.
     *
     * @param resourceRegistration a {@link ManagementResourceRegistration} created from this definition
     * @param alias the resource's alias
     */
    void registerAliasOperations(final ManagementResourceRegistration resourceRegistration, final PathElement alias);

    /**
     * Register operations associated with an alias resource.
     *
     * @param resourceRegistration a {@link ManagementResourceRegistration} created from this definition
     * @param alias the resource's alias
     */
    void registerAliasAttributes(final ManagementResourceRegistration resourceRegistration, final PathElement alias);

    /**
     * Register child resources associated with an alias resource.
     *
     * @param resourceRegistration a {@link ManagementResourceRegistration} created from this definition
     * @param alias the resource's alias
     */
    void registerAliasChildren(final ManagementResourceRegistration resourceRegistration, final PathElement alias);
}
