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

import java.util.function.UnaryOperator;

import org.jboss.as.controller.capability.RuntimeCapability;
import org.wildfly.clustering.service.Requirement;

/**
 * Provides a capability definition provider built from a requirement.
 * @author Paul Ferraro
 */
public class RequirementCapability implements Capability {

    private final RuntimeCapability<Void> definition;

    /**
     * Creates a new capability based on the specified requirement
     * @param requirement the requirement basis
     */
    public RequirementCapability(Requirement requirement) {
        this(requirement, UnaryOperator.identity());
    }

    /**
     * Creates a new capability based on the specified requirement
     * @param requirement the requirement basis
     * @param configurator configures the capability
     */
    public RequirementCapability(Requirement requirement, UnaryOperator<RuntimeCapability.Builder<Void>> configurator) {
        this.definition = configurator.apply(RuntimeCapability.Builder.of(requirement.getName()).setServiceType(requirement.getType())).build();
    }

    @Override
    public RuntimeCapability<Void> getDefinition() {
        return this.definition;
    }
}
