/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

import org.jboss.as.clustering.function.Predicates;
import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.wildfly.clustering.service.BinaryRequirement;
import org.wildfly.clustering.service.Requirement;
import org.wildfly.clustering.service.UnaryRequirement;

/**
 * {@link CapabilityReferenceRecorder} for resource-level capability references.
 * @author Paul Ferraro
 */
public class ResourceCapabilityReference extends PredicateCapabilityReference {

    /**
     * Creates a new reference between the specified capability and the specified requirement
     * @param capability the capability referencing the specified requirement
     * @param requirement the requirement of the specified capability
     */
    public ResourceCapabilityReference(Capability capability, Requirement requirement) {
        super(capability, requirement, Predicates.always());
    }

    /**
     * Creates a new reference between the specified capability and the specified requirement
     * @param capability the capability referencing the specified requirement
     * @param requirement the requirement of the specified capability
     * @param requirementNameResolver function for resolving the dynamic elements of the requirement name
     */
    public ResourceCapabilityReference(Capability capability, UnaryRequirement requirement, UnaryCapabilityNameResolver requirementNameResolver) {
        super(capability, requirement, requirementNameResolver, Predicates.always());
    }

    /**
     * Creates a new reference between the specified capability and the specified requirement
     * @param capability the capability referencing the specified requirement
     * @param requirement the requirement of the specified capability
     * @param requirementNameResolver function for resolving the dynamic elements of the requirement name
     */
    public ResourceCapabilityReference(Capability capability, BinaryRequirement requirement, BinaryCapabilityNameResolver requirementNameResolver) {
        super(capability, requirement, requirementNameResolver, Predicates.always());
    }
}
