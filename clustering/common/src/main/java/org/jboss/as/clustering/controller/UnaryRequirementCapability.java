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

import org.jboss.as.controller.capability.RuntimeCapability;
import org.wildfly.clustering.service.UnaryRequirement;

/**
 * Provides a capability definition provider built from a unary requirement.
 * @author Paul Ferraro
 */
public class UnaryRequirementCapability implements Capability {

    private final RuntimeCapability<Void> definition;

    /**
     * Creates a new capability based on the specified unary requirement
     * @param requirement the unary requirement basis
     */
    public UnaryRequirementCapability(UnaryRequirement requirement) {
        this(requirement, UnaryCapabilityNameResolver.DEFAULT);
    }

    /**
     * Creates a new capability based on the specified unary requirement
     * @param requirement the unary requirement basis
     */
    public UnaryRequirementCapability(UnaryRequirement requirement, UnaryCapabilityNameResolver resolver) {
        this.definition = RuntimeCapability.Builder.of(requirement.getName(), true)
                .setServiceType(requirement.getType())
                .setDynamicNameMapper(resolver)
                .build();
    }

    @Override
    public RuntimeCapability<Void> getDefinition() {
        return this.definition;
    }
}
