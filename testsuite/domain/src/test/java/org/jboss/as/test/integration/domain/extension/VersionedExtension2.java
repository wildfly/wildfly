/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.domain.extension;

import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.AliasOperationTransformer;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.RejectExpressionValuesTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;

import java.util.EnumSet;

/**
 * Version 2 of an extension.
 *
 * @author Emanuel Muckenhuber
 */
public class VersionedExtension2 extends VersionedExtensionCommon {

    // New element which does not exist in v1
    private static final PathElement NEW_ELEMENT = PathElement.pathElement("new-element");
    // Element which is element>renamed in v2
    private static final PathElement RENAMED = PathElement.pathElement("renamed", "element");

    private static final SubsystemInitialization TEST_SUBSYSTEM = new SubsystemInitialization(SUBSYSTEM_NAME, true);
    private static final RejectExpressionValuesTransformer rejectExpressions = new RejectExpressionValuesTransformer("int", "string");

    void processTestSubsystem(final SubsystemRegistration subsystem, final ManagementResourceRegistration registration) {

        // Register a update operation, which requires the transformer to create composite operation
        registration.registerOperationHandler("update", new OperationStepHandler() {
            @Override
            public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
                final ModelNode model = resource.getModel();
                model.get("test-attribute").set("test");
                context.getResult().set(model);
                context.stepCompleted();
            }
        }, DESCRIPTION_PROVIDER);

        // Add a new model, which does not exist in the old model
        registration.registerSubModel(createResourceDefinition(NEW_ELEMENT));
        // Add the renamed model
        registration.registerSubModel(createResourceDefinition(RENAMED));
        registration.registerOperationHandler("test", new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                context.getResult().set(true);
                context.stepCompleted();
            }
        }, DESCRIPTION_PROVIDER, false, OperationEntry.EntryType.PUBLIC, EnumSet.of(OperationEntry.Flag.READ_ONLY));

        // Register the transformers
        final TransformersSubRegistration transformers =  subsystem.registerModelTransformers(ModelVersion.create(1, 0, 0), RESOURCE_TRANSFORMER);
        // Reject the expression values for attributes
        transformers.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, rejectExpressions.getWriteAttributeTransformer());
        //
        transformers.registerOperationTransformer("update", new UpdateTransformer());
        transformers.registerOperationTransformer("test", new OperationTransformer() {
            @Override
            public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
                return new TransformedOperation(operation, new OperationResultTransformer() {
                    @Override
                    public ModelNode transformResult(ModelNode result) {
                        result.get(RESULT).set(false);
                        return result;
                    }
                });
            }
        });

        // Discard the add/remove operation to the new element
        final TransformersSubRegistration newElement = transformers.registerSubResource(NEW_ELEMENT);
        newElement.discardOperations(TransformersSubRegistration.COMMON_OPERATIONS);

        // Register an alias operation transformer, transforming renamed>element to element>renamed
        transformers.registerSubResource(RENAMED, AliasOperationTransformer.replaceLastElement(PathElement.pathElement("element", "renamed")));
    }

    @Override
    public void initialize(final ExtensionContext context) {
        // Normal test subsystem
        final SubsystemInitialization.RegistrationResult result1 = TEST_SUBSYSTEM.initializeSubsystem(context, ModelVersion.create(2, 0, 0));
        processTestSubsystem(result1.getSubsystemRegistration(), result1.getResourceRegistration());
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        TEST_SUBSYSTEM.initializeParsers(context);
    }

    static ResourceTransformer RESOURCE_TRANSFORMER = new ResourceTransformer() {
        @Override
        public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
            final ResourceTransformationContext childContext = context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource);
            for(final Resource.ResourceEntry entry : resource.getChildren("renamed")) {
                childContext.processChild(PathElement.pathElement("element", "renamed"), entry);
            }
        }

    };

    static class UpdateTransformer implements OperationTransformer {

        @Override
        public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation) {

            // TODO does the operation transformer have to deal w/ profile in the address ?
            // final ModelNode addr = PathAddress.pathAddress(SUBSYSTEM_PATH).toModelNode();
            final ModelNode addr = address.toModelNode();

            final ModelNode write = new ModelNode();
            write.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            write.get(OP_ADDR).set(addr);
            write.get(NAME).set(TEST_ATTRIBUTE.getName());
            write.get(VALUE).set("test");

            final ModelNode read = new ModelNode();
            read.get(OP).set(READ_RESOURCE_OPERATION);
            read.get(OP_ADDR).set(addr);

            final ModelNode composite = new ModelNode();
            composite.get(OP).set(COMPOSITE);
            composite.get(OP_ADDR).setEmptyList();
            composite.get(STEPS).add(write);
            composite.get(STEPS).add(read);

            return new TransformedOperation(composite, new OperationResultTransformer() {
                @Override
                public ModelNode transformResult(final ModelNode result) {
                    final ModelNode transformed = result.clone();
                    transformed.get(RESULT).set(result.get(RESULT, "step-2", RESULT));
                    return transformed;
                }
            });
        }

    };

}
