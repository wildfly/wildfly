/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.access.management;

import java.util.Set;

import org.jboss.as.controller.PathAddress;

/**
 * Provides information about how an {@link AccessConstraintDefinition} is utilized for a particular
 * {@link org.jboss.as.controller.registry.ImmutableManagementResourceRegistration management resource registration}.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public interface AccessConstraintUtilization {

    /**
     * Gets the address under which the resource registrations is registered.
     *
     * @return the address. Will not be {@code null}
     */
    PathAddress getPathAddress();

    /**
     * Gets whether the constraint applies to the resource as a whole
     * @return  {@code true} if the entire resource is constrained; {@code false} if the constraint only applies
     *          to attributes or operations
     */
    boolean isEntireResourceConstrained();

    /**
     * Gets the names of any attributes that are specifically constrained.
     * @return  the attribute names, or an empty set. Will not be {@code null}
     */
    Set<String> getAttributes();

    /**
     * Gets the names of any operations that are specifically constrained.
     * @return  the operation names, or an empty set. Will not be {@code null}
     */
    Set<String> getOperations();
}
