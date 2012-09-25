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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchResourceRegistration {

    static final VersionReadHandler VERSION = new VersionReadHandler();
    static final CumulativeReadHandler CUMULATIVE = new CumulativeReadHandler();
    static final PatchesReadHandler PATCHES = new PatchesReadHandler();

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
        registration.registerReadOnlyAttribute(VERSION, VERSION);
        registration.registerReadOnlyAttribute(CUMULATIVE, CUMULATIVE);
        registration.registerReadOnlyAttribute(PATCHES, PATCHES);

        return registration;
    }

    /**
     * Register the patching resource.
     *
     * @param parent the parent resource
     */
    public static void registerPatchResource(final Resource parent) {
        parent.registerChild(PatchResourceDefinition.PATH, Resource.Factory.create());
    }

    abstract static class AbstractReadAttribute extends SimpleAttributeDefinition implements OperationStepHandler {

        protected AbstractReadAttribute(String name, ModelType type) {
            super(name, type, false);
        }

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

    static class VersionReadHandler extends AbstractReadAttribute {

        VersionReadHandler() {
            super("version", ModelType.STRING);
        }

        @Override
        void handle(ModelNode result, PatchInfo info) {
            result.set(info.getVersion());
        }
    }

    static class CumulativeReadHandler extends AbstractReadAttribute {

        CumulativeReadHandler() {
            super("cumulative", ModelType.STRING);
        }

        @Override
        void handle(ModelNode result, PatchInfo info) {
            result.set(info.getCumulativeID());
        }
    }

    static class PatchesReadHandler extends AbstractReadAttribute {

        PatchesReadHandler() {
            super("patches", ModelType.LIST);
        }

        @Override
        void handle(ModelNode result, PatchInfo info) {
            result.setEmptyList();
            for(final String id : info.getPatchIDs()) {
                result.add(id);
            }
        }
    }

}
