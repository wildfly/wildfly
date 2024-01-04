/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.function.Predicate;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
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
