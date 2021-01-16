/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

import java.util.function.Predicate;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.BinaryRequirement;
import org.wildfly.clustering.service.Requirement;
import org.wildfly.clustering.service.UnaryRequirement;

/**
 * A capability reference recorder that registers a given requirement conditionally based the attribute value.
 * @author Paul Ferraro
 */
public class PredicateCapabilityReference extends ResourceCapabilityReference {
    private static final Predicate<ModelNode> BOOLEAN = ModelNode::asBoolean;

    private final Predicate<ModelNode> predicate;

    /**
     * Creates a new reference between the specified capability and the specified requirement for boolean attributes.
     * @param capability the capability referencing the specified requirement
     * @param requirement the requirement of the specified capability
     */
    public PredicateCapabilityReference(Capability capability, Requirement requirement) {
        this(capability, requirement, BOOLEAN);
    }

    /**
     * Creates a new reference between the specified capability and the specified requirement
     * @param capability the capability referencing the specified requirement
     * @param requirement the requirement of the specified capability
     * @param predicate a predicate that determines for which values the requirement should be registered
     */
    public PredicateCapabilityReference(Capability capability, Requirement requirement, Predicate<ModelNode> predicate) {
        super(capability, requirement);
        this.predicate = predicate;
    }

    /**
     * Creates a new reference between the specified capability and the specified requirement
     * @param capability the capability referencing the specified requirement
     * @param requirement the requirement of the specified capability
     * @param requirementNameResolver function for resolving the dynamic elements of the requirement name
     */
    public PredicateCapabilityReference(Capability capability, UnaryRequirement requirement, UnaryCapabilityNameResolver requirementNameResolver) {
        this(capability, requirement, requirementNameResolver, BOOLEAN);
    }

    /**
     * Creates a new reference between the specified capability and the specified requirement
     * @param capability the capability referencing the specified requirement
     * @param requirement the requirement of the specified capability
     * @param requirementNameResolver function for resolving the dynamic elements of the requirement name
     * @param predicate a predicate that determines for which values the requirement should be registered
     */
    public PredicateCapabilityReference(Capability capability, UnaryRequirement requirement, UnaryCapabilityNameResolver requirementNameResolver, Predicate<ModelNode> predicate) {
        super(capability, requirement, requirementNameResolver);
        this.predicate = predicate;
    }

    /**
     * Creates a new reference between the specified capability and the specified requirement
     * @param capability the capability referencing the specified requirement
     * @param requirement the requirement of the specified capability
     * @param requirementNameResolver function for resolving the dynamic elements of the requirement name
     * @param predicate a predicate that determines for which values the requirement should be registered
     */
    public PredicateCapabilityReference(Capability capability, BinaryRequirement requirement, BinaryCapabilityNameResolver requirementNameResolver) {
        this(capability, requirement, requirementNameResolver, BOOLEAN);
    }

    /**
     * Creates a new reference between the specified capability and the specified requirement
     * @param capability the capability referencing the specified requirement
     * @param requirement the requirement of the specified capability
     * @param requirementNameResolver function for resolving the dynamic elements of the requirement name
     * @param predicate a predicate that determines for which values the requirement should be registered
     */
    public PredicateCapabilityReference(Capability capability, BinaryRequirement requirement, BinaryCapabilityNameResolver requirementNameResolver, Predicate<ModelNode> predicate) {
        super(capability, requirement, requirementNameResolver);
        this.predicate = predicate;
    }

    @Override
    public void addCapabilityRequirements(OperationContext context, Resource resource, String attributeName, String... values) {
        for (String value : values) {
            if (this.predicate.test(new ModelNode(value))) {
                super.addCapabilityRequirements(context, resource, attributeName, value);
            }
        }
    }

    @Override
    public void removeCapabilityRequirements(OperationContext context, Resource resource, String attributeName, String... values) {
        for (String value : values) {
            if (this.predicate.test(new ModelNode(value))) {
                super.removeCapabilityRequirements(context, resource, attributeName, value);
            }
        }
    }
}
