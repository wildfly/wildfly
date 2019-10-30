/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

import java.util.EnumSet;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.BinaryCapabilityNameResolver;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.controller.transform.LegacyPropertyAddOperationTransformer;
import org.jboss.as.clustering.controller.transform.LegacyPropertyResourceTransformer;
import org.jboss.as.clustering.controller.transform.OperationTransformer;
import org.jboss.as.clustering.controller.transform.SimpleOperationTransformer;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;

/**
 * @author Paul Ferraro
 */
public class ProtocolResourceDefinition extends AbstractProtocolResourceDefinition {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    public static PathElement pathElement(String name) {
        return PathElement.pathElement("protocol", name);
    }

    enum Capability implements org.jboss.as.clustering.controller.Capability {
        PROTOCOL("org.wildfly.clustering.jgroups.protocol"),
        ;
        private final RuntimeCapability<Void> definition;

        Capability(String name) {
            this.definition = RuntimeCapability.Builder.of(name, true).setDynamicNameMapper(BinaryCapabilityNameResolver.PARENT_CHILD).setAllowMultipleRegistrations(true).build();
        }

        @Override
        public RuntimeCapability<?> getDefinition() {
            return this.definition;
        }
    }

    static void addTransformations(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {
        AbstractProtocolResourceDefinition.addTransformations(version, builder);

        if (JGroupsModel.VERSION_4_1_0.requiresTransformation(version)) {
            // See WFLY-6782, add-index parameter was missing from add operation definition
            builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.DEFINED, ModelDescriptionConstants.ADD_INDEX);
        }

        if (JGroupsModel.VERSION_3_0_0.requiresTransformation(version)) {
            // Translate /subsystem=jgroups/stack=*/protocol=*:add() -> /subsystem=jgroups/stack=*:add-protocol()
            OperationTransformer addTransformer = new OperationTransformer() {
                @SuppressWarnings("deprecation")
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    PathAddress address = Operations.getPathAddress(operation);
                    PathAddress stackAddress = address.getParent();
                    ModelNode addProtocolOp = operation.clone();
                    addProtocolOp.get(ModelDescriptionConstants.OP_ADDR).set(stackAddress.toModelNode());
                    addProtocolOp.get(ModelDescriptionConstants.OP).set("add-protocol");

                    addProtocolOp = new LegacyPropertyAddOperationTransformer(op -> Operations.getPathAddress(op).append(pathElement(op.get(AbstractProtocolResourceDefinition.DeprecatedAttribute.TYPE.getName()).asString()))).transformOperation(addProtocolOp);

                    return addProtocolOp;
                }
            };
            builder.addOperationTransformationOverride(ModelDescriptionConstants.ADD).setCustomOperationTransformer(new SimpleOperationTransformer(addTransformer)).inheritResourceAttributeDefinitions();

            // Translate /subsystem=jgroups/stack=*/protocol=*:remove() -> /subsystem=jgroups/stack=*:remove-protocol()
            OperationTransformer removeTransformer = new OperationTransformer() {
                @SuppressWarnings("deprecation")
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    PathAddress address = Operations.getPathAddress(operation);
                    String protocol = address.getLastElement().getValue();
                    PathAddress stackAddress = address.getParent();
                    ModelNode legacyOperation = Util.createOperation("remove-protocol", stackAddress);
                    legacyOperation.get(AbstractProtocolResourceDefinition.DeprecatedAttribute.TYPE.getName()).set(protocol);
                    return legacyOperation;
                }
            };
            builder.addOperationTransformationOverride(ModelDescriptionConstants.REMOVE).setCustomOperationTransformer(new SimpleOperationTransformer(removeTransformer));

            builder.setCustomResourceTransformer(new LegacyPropertyResourceTransformer());
        }
    }

    static final UnaryOperator<OperationStepHandler> LEGACY_OPERATION_TRANSFORMER = new UnaryOperator<OperationStepHandler>() {
        @Override
        public OperationStepHandler apply(OperationStepHandler handler) {
            return new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    PathAddress address = context.getCurrentAddress();
                    PathAddress parentAddress = address.getParent();
                    Resource parent = context.readResourceFromRoot(parentAddress, false);
                    PathElement legacyPath = GenericProtocolResourceDefinition.pathElement(address.getLastElement().getValue());
                    // If legacy protocol exists, transform this operation using the legacy protocol as the target resource
                    if (parent.hasChild(legacyPath)) {
                        PathAddress legacyAddress = parentAddress.append(legacyPath);
                        Operations.setPathAddress(operation, legacyAddress);
                        String operationName = Operations.getName(operation);
                        context.addStep(operation, context.getRootResourceRegistration().getOperationHandler(legacyAddress, operationName), OperationContext.Stage.MODEL);
                    } else {
                        handler.execute(context, operation);
                    }
                }
            };
        }
    };

    static class LegacyAddOperationTransformation implements UnaryOperator<OperationStepHandler> {
        private final Predicate<ModelNode> legacy;

        <E extends Enum<E> & org.jboss.as.clustering.controller.Attribute> LegacyAddOperationTransformation(Class<E> attributeClass) {
            // If none of the specified attributes are defined, then this is a legacy operation
            this.legacy = operation -> {
                for (org.jboss.as.clustering.controller.Attribute attribute : EnumSet.allOf(attributeClass)) {
                    if (operation.hasDefined(attribute.getName())) return false;
                }
                return true;
            };
        }

        LegacyAddOperationTransformation(String... legacyProperties) {
            // If any of the specified properties are defined, then this is a legacy operation
            this.legacy = operation -> {
                if (!operation.hasDefined(Attribute.PROPERTIES.getName())) return false;
                for (String legacyProperty : legacyProperties) {
                    if (operation.get(Attribute.PROPERTIES.getName()).hasDefined(legacyProperty)) return true;
                }
                return false;
            };
        }

        @Override
        public OperationStepHandler apply(OperationStepHandler handler) {
            return (context, operation) -> {
                if (this.legacy.test(operation)) {
                    PathElement path = context.getCurrentAddress().getLastElement();
                    // This is a legacy add operation - process it using the generic handler
                    OperationStepHandler genericHandler = context.getResourceRegistration().getParent().getOperationHandler(PathAddress.pathAddress(ProtocolResourceDefinition.WILDCARD_PATH), ModelDescriptionConstants.ADD);
                    Operations.setPathAddress(operation, context.getCurrentAddress().getParent().append(GenericProtocolResourceDefinition.pathElement(path.getValue())));
                    // Process this step first to preserve protocol order
                    context.addStep(operation, genericHandler, OperationContext.Stage.MODEL, true);
                } else {
                    handler.execute(context, operation);
                }
            };
        }
    }

    private static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        private final UnaryOperator<ResourceDescriptor> configurator;

        ResourceDescriptorConfigurator(UnaryOperator<ResourceDescriptor> configurator) {
            this.configurator = configurator;
        }

        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return this.configurator.apply(descriptor).addCapabilities(Capability.class);
        }
    }

    ProtocolResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfiguratorFactory serviceConfiguratorFactory, ResourceServiceConfiguratorFactory parentServiceConfiguratorFactory) {
        super(new Parameters(path, path.isWildcard() ? JGroupsExtension.SUBSYSTEM_RESOLVER.createChildResolver(path) : JGroupsExtension.SUBSYSTEM_RESOLVER.createChildResolver(path, WILDCARD_PATH)).setOrderedChild(), new ResourceDescriptorConfigurator(configurator), serviceConfiguratorFactory, parentServiceConfiguratorFactory);
    }
}
