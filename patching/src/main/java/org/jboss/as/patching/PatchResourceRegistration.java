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

package org.jboss.as.patching;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchResourceRegistration {

    static final AttributeDefinition VERSION = SimpleAttributeDefinitionBuilder.create("version", ModelType.STRING)
            .build();
    static final AttributeDefinition CUMULATIVE = SimpleAttributeDefinitionBuilder.create("cumulative", ModelType.STRING)
            .build();
    static final AttributeDefinition PATCHES = PrimitiveListAttributeDefinition.Builder.of("patches", ModelType.STRING)
            .build();

    static final OperationStepHandler VERSION_HANDLER = new VersionReadHandler();
    static final OperationStepHandler CUMULATIVE_HANDLER = new CumulativeReadHandler();
    static final OperationStepHandler PATCHES_HANDLER = new PatchesReadHandler();

    static final OperationDefinition PATCH = new SimpleOperationDefinitionBuilder("patch", PatchResourceDefinition.getResourceDescriptionResolver(PatchResourceDefinition.NAME))
            .build();

    /**
     * Register the patching resource registration and handlers
     *
     * @param parent the parent registration
     * @return the registration
     */
    public static ManagementResourceRegistration registerPatchModel(final ManagementResourceRegistration parent) {
        final ManagementResourceRegistration registration = parent.registerSubModel(PatchResourceDefinition.INSTANCE);

        // register the patch operation handler
        registration.registerOperationHandler(PATCH, LocalPatchOperationStepHandler.INSTANCE);

        // Read attributes
        registration.registerReadOnlyAttribute(VERSION, VERSION_HANDLER);
        registration.registerReadOnlyAttribute(CUMULATIVE, CUMULATIVE_HANDLER);
        registration.registerReadOnlyAttribute(PATCHES, PATCHES_HANDLER);

        return registration;
    }

    /**
     * Register the patching resource.
     *
     * @param parent the parent resource
     */
    public static void registerPatchResource(final Resource parent) {
        System.out.println("PatchResourceRegistration.registerPatchResource()");
        parent.registerChild(PatchResourceDefinition.PATH, Resource.Factory.create());
    }

    abstract static class AbstractReadAttributeHandler implements OperationStepHandler {

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            //
            final PatchInfoService service = (PatchInfoService) context.getServiceRegistry(false).getRequiredService(PatchInfoService.NAME).getValue();
            final PatchInfo info = service.getPatchInfo();
            final ModelNode result = context.getResult();
            handle(result, info);
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }

        abstract void handle(ModelNode result, PatchInfo info);
    }

    static class VersionReadHandler extends AbstractReadAttributeHandler {

        @Override
        void handle(ModelNode result, PatchInfo info) {
            result.set(info.getVersion());
        }
    }

    static class CumulativeReadHandler extends AbstractReadAttributeHandler {

        @Override
        void handle(ModelNode result, PatchInfo info) {
            result.set(info.getCumulativeID());
        }
    }

    static class PatchesReadHandler extends AbstractReadAttributeHandler {

        @Override
        void handle(ModelNode result, PatchInfo info) {
            result.setEmptyList();
            for(final String id : info.getPatchIDs()) {
                result.add(id);
            }
        }
    }

}
