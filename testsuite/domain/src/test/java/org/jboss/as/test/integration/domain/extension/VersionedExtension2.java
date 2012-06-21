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
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.SubSystemTransformersRegistry;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
public class VersionedExtension2 extends VersionedExtensionCommon {

    @Override
    public void initialize(final ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, 2, 0, 0);

        final ManagementResourceRegistration registration = initializeSubsystem(subsystem);
        registration.registerOperationHandler("update", new OperationStepHandler() {
            @Override
            public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
                final ModelNode model = resource.getModel();
                model.get("test-attribute").set("test");
                context.getResult().set(model);
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            }
        }, DESCRIPTION_PROVIDER);

        // Register the transformers
        final SubSystemTransformersRegistry transformers =  subsystem.registerModelTransformers(ModelVersion.create(1, 0, 0));
        transformers.registerOperationTransformer(PathAddress.EMPTY_ADDRESS, "update", new UpdateTransformer());
    }

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
                    return result.get(RESULT, "step-2");
                }
            });
        }

    };



}
