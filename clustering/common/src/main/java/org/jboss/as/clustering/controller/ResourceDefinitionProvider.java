/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.controller;

import java.util.List;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Provides a {@link ResourceDefinition} and handles its registration.
 * @author Paul Ferraro
 */
public interface ResourceDefinitionProvider extends Definable<ResourceDefinition>, ResourceDefinition, Registration<ManagementResourceRegistration> {

    @Override
    default PathElement getPathElement() {
        return this.getDefinition().getPathElement();
    }

    @Override
    default DescriptionProvider getDescriptionProvider(ImmutableManagementResourceRegistration registration) {
        return this.getDefinition().getDescriptionProvider(registration);
    }

    @Override
    default List<AccessConstraintDefinition> getAccessConstraints() {
        return this.getDefinition().getAccessConstraints();
    }

    @Override
    default boolean isRuntime() {
        return this.getDefinition().isRuntime();
    }

    @Override
    default boolean isOrderedChild() {
        return this.getDefinition().isOrderedChild();
    }

    @Override
    default void registerOperations(ManagementResourceRegistration registration) {
        // We will handle registration ourselves
    }

    @Override
    default void registerAttributes(ManagementResourceRegistration registration) {
        // We will handle registration ourselves
    }

    @Override
    default void registerNotifications(ManagementResourceRegistration registration) {
        // We will handle registration ourselves
    }

    @Override
    default void registerChildren(ManagementResourceRegistration registration) {
        // We will handle registration ourselves
    }
}
