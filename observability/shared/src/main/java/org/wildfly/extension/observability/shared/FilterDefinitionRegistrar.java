/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.observability.shared;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;

import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;

public class FilterDefinitionRegistrar implements ChildResourceDefinitionRegistrar {

    public static final PathElement PATH = PathElement.pathElement("filter");
    public static final ResourceRegistration RESOURCE_REGISTRATION = ResourceRegistration.of(PATH, Stability.COMMUNITY);

    public static final SimpleAttributeDefinition OUTCOME = create("outcome", ModelType.STRING, true)
            .setDefaultValue(new ModelNode("reject"))
            .setAllowedValues("accept", "reject")
            .setAllowExpression(false)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition FIELD = create("field", ModelType.STRING, false)
            .setAllowedValues("meter-name", "tag-name", "tag-value")
            .setAllowExpression(false)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition CONDITION = create("condition", ModelType.STRING, false)
            .setAllowedValues("starts-with", "ends-with", "contains", "equals")
            .setAllowExpression(false)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition VALUE = create("value", ModelType.STRING, false)
            .setAllowExpression(false)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition NEGATE = create("negate", ModelType.BOOLEAN, true)
            .setDefaultValue(ModelNode.FALSE)
            .setAllowExpression(false)
            .setRestartAllServices()
            .build();

    public static final Collection<AttributeDefinition> ATTRIBUTES = List.of(OUTCOME, FIELD, CONDITION, VALUE, NEGATE);

    private final ResourceOperationRuntimeHandler parentRuntimeHandler;
    private final ParentResourceDescriptionResolver resolver;

    public FilterDefinitionRegistrar(ResourceOperationRuntimeHandler parentRuntimeHandler, ParentResourceDescriptionResolver resolver) {
        this.parentRuntimeHandler = parentRuntimeHandler;
        this.resolver = resolver;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptor descriptor = ResourceDescriptor.builder(
                        resolver.createChildResolver(PATH))
                .addAttributes(ATTRIBUTES)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.restartParent(parentRuntimeHandler))
                .build();

        ManagementResourceRegistration registration = parent.registerSubModel(
                ResourceDefinition.builder(RESOURCE_REGISTRATION, descriptor.getResourceDescriptionResolver()).build());

        ManagementResourceRegistrar.of(descriptor).register(registration);

        return registration;
    }
}
