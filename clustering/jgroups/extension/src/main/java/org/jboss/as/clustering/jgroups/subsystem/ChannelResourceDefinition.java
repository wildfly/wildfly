/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.clustering.controller.AddStepHandler;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.RemoveStepHandler;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.validation.ModuleIdentifierValidatorBuilder;
import org.jboss.as.clustering.controller.validation.ParameterValidatorBuilder;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.Resource.ResourceEntry;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker.DefaultDiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Definition for /subsystem=jgroups/channel=* resources
 *
 * @author Paul Ferraro
 */
public class ChannelResourceDefinition extends ChildResourceDefinition {

    public static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    public static PathElement pathElement(String name) {
        return PathElement.pathElement("channel", name);
    }

    public enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        STACK("stack", ModelType.STRING, false), // 'stack' is required since model 4.0.0
        MODULE("module", ModelType.STRING, new ModelNode("org.wildfly.clustering.server"), new ModuleIdentifierValidatorBuilder()),
        CLUSTER("cluster", ModelType.STRING, true)
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, boolean allowNull) {
            this.definition = createBuilder(name, type, allowNull).build();
        }

        Attribute(String name, ModelType type, ModelNode defaultValue, ParameterValidatorBuilder validator) {
            SimpleAttributeDefinitionBuilder builder = createBuilder(name, type, true).setDefaultValue(defaultValue);
            this.definition = builder.setValidator(validator.configure(builder).build()).build();
        }

        private static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type, boolean allowNull) {
            return new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setAllowNull(allowNull)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            ;
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(WILDCARD_PATH);

        if (JGroupsModel.VERSION_4_0_0.requiresTransformation(version)) {
            DiscardAttributeChecker discarder = new DefaultDiscardAttributeChecker(false, true) {
                @Override
                protected boolean isValueDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
                    return !attributeValue.isDefined() || attributeValue.equals(new ModelNode(address.getLastElement().getValue()));
                }
            };
            builder.getAttributeBuilder()
                    .setDiscard(discarder, Attribute.CLUSTER.getDefinition())
                    .addRejectCheck(RejectAttributeChecker.DEFINED, Attribute.CLUSTER.getDefinition())
                    ;
        }
    }

    final boolean allowRuntimeOnlyRegistration;

    ChannelResourceDefinition(boolean allowRuntimeOnlyRegistration) {
        super(WILDCARD_PATH, new JGroupsResourceDescriptionResolver(WILDCARD_PATH));
        this.allowRuntimeOnlyRegistration = allowRuntimeOnlyRegistration;
    }

    @Override
    public void register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver()).addAttributes(Attribute.class);
        ResourceServiceHandler handler = new ChannelServiceHandler();
        new AddStepHandler(descriptor, handler) {
            @SuppressWarnings("deprecation")
            @Override
            protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
                // Handle recipe for version < 4.0 where stack was not required and the stack attribute would use default-stack for a default value
                if (!operation.hasDefined(Attribute.STACK.getDefinition().getName())) {
                    ModelNode parentModel = context.readResourceFromRoot(context.getCurrentAddress().getParent()).getModel();
                    // If default-stack is not defined either, then recipe must be for version >= 4.0 and so this really is an invalid operation
                    if (parentModel.hasDefined(JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_STACK.getDefinition().getName())) {
                        operation.get(Attribute.STACK.getDefinition().getName()).set(parentModel.get(JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_STACK.getDefinition().getName()));
                    }
                }
                super.populateModel(context, operation, resource);
                // Register runtime resource children for channel protocols
                if (ChannelResourceDefinition.this.allowRuntimeOnlyRegistration && (context.getRunningMode() == RunningMode.NORMAL)) {
                    String name = context.getCurrentAddressValue();
                    String stack = ModelNodes.asString(Attribute.STACK.getDefinition().resolveModelAttribute(context, resource.getModel()));

                    PathAddress address = context.getCurrentAddress();
                    PathAddress subsystemAddress = address.subAddress(0, address.size() - 1);
                    // Lookup the name of the default stack if necessary
                    PathAddress stackAddress = subsystemAddress.append(StackResourceDefinition.pathElement(stack));

                    context.addStep(new ProtocolResourceRegistrationHandler(name, stackAddress), OperationContext.Stage.MODEL);
                }
            }
        }.register(registration);
        new RemoveStepHandler(descriptor, handler) {
            @Override
            protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
                // Unregister runtime resource children for channel protocols
                if (ChannelResourceDefinition.this.allowRuntimeOnlyRegistration && (context.getRunningMode() == RunningMode.NORMAL)) {
                    Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
                    for (ResourceEntry entry: resource.getChildren(ProtocolResourceDefinition.WILDCARD_PATH.getKey())) {
                        context.removeResource(PathAddress.pathAddress(entry.getPathElement()));
                    }
                    context.getResourceRegistrationForUpdate().unregisterOverrideModel(context.getCurrentAddressValue());
                }
                super.performRemove(context, operation, model);
            }
        }.register(registration);

        if (this.allowRuntimeOnlyRegistration) {
            new MetricHandler<>(new ChannelMetricExecutor(), ChannelMetric.class).register(registration);
        }

        new ForkResourceDefinition(this.allowRuntimeOnlyRegistration).register(registration);
    }
}
