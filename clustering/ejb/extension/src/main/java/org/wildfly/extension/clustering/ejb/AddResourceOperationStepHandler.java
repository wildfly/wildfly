/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.resource.AttributeTranslation;
import org.wildfly.subsystem.resource.operation.AddResourceOperationStepHandlerDescriptor;
import org.wildfly.subsystem.resource.operation.DescribedOperationStepHandler;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;

/**
 * Temporary workaround for WFCORE-7188
 * @author Paul Ferraro
 */
public class AddResourceOperationStepHandler  implements OperationStepHandler, DescribedOperationStepHandler<AddResourceOperationStepHandlerDescriptor>, AddResourceOperationStepHandlerDescriptor {
    private final AddResourceOperationStepHandlerDescriptor descriptor;
    private final OperationStepHandler handler;

    AddResourceOperationStepHandler(OperationStepHandler handler) {
        this.descriptor = ((org.wildfly.subsystem.resource.operation.AddResourceOperationStepHandler) handler).getDescriptor();
        this.handler = handler;
    }

    @Override
    public AddResourceOperationStepHandlerDescriptor getDescriptor() {
        return this.descriptor;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        this.handler.execute(context, operation);
    }

    @Override
    public Optional<ResourceOperationRuntimeHandler> getRuntimeHandler() {
        return this.descriptor.getRuntimeHandler();
    }

    @Override
    public BiPredicate<OperationContext, Resource> getCapabilityFilter(RuntimeCapability<?> capability) {
        return this.descriptor.getCapabilityFilter(capability);
    }

    @Deprecated
    @Override
    public Set<PathElement> getRequiredChildren() {
        return this.descriptor.getRequiredChildren();
    }

    @Override
    public Map<PathElement, ResourceRegistration> getRequiredChildResources() {
        return this.descriptor.getRequiredChildResources();
    }

    @Deprecated
    @Override
    public Set<PathElement> getRequiredSingletonChildren() {
        return this.descriptor.getRequiredSingletonChildren();
    }

    @Override
    public Map<PathElement, ResourceRegistration> getRequiredSingletonChildResources() {
        return this.descriptor.getRequiredSingletonChildResources();
    }

    @Override
    public AttributeTranslation getAttributeTranslation(AttributeDefinition attribute) {
        return this.descriptor.getAttributeTranslation(attribute);
    }

    @Override
    public UnaryOperator<Resource> getResourceTransformation() {
        return this.descriptor.getResourceTransformation();
    }

    @Override
    public Optional<Consumer<DeploymentProcessorTarget>> getDeploymentChainContributor() {
        return this.descriptor.getDeploymentChainContributor();
    }
}
