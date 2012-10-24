/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import org.jboss.as.boot.DirectoryStructure;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.patching.runner.PatchingException;
import org.jboss.as.patching.runner.PatchingResult;
import org.jboss.as.patching.runner.PatchingTaskRunner;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
public class LocalPatchRollbackHandler implements OperationStepHandler {

    public static final LocalPatchRollbackHandler INSTANCE = new LocalPatchRollbackHandler();

    private final String PATCH_ID = Constants.PATCH_ID.getName();
    private final String OVERRIDE_ALL = Constants.OVERRIDE_ALL.getName();

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final String patchId = operation.require(PATCH_ID).asString();
        final boolean overrideAll = operation.get(OVERRIDE_ALL).asBoolean(false);
        //
        context.acquireControllerLock();
        final PatchInfoService service = (PatchInfoService) context.getServiceRegistry(false).getRequiredService(PatchInfoService.NAME).getValue();

        final PatchInfo info = service.getPatchInfo();
        final DirectoryStructure structure = service.getStructure();
        final PatchingTaskRunner taskRunner = new PatchingTaskRunner(info, structure);
        try {
            // Rollback
            final PatchingResult result = taskRunner.rollback(patchId, overrideAll);
            if(result.hasFailures()) {
                final ModelNode failureDescription = context.getFailureDescription();
                failureDescription.get("content-items").set("TODO");
                context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
                return;
            }
            context.completeStep(new OperationContext.ResultHandler() {

                @Override
                public void handleResult(OperationContext.ResultAction resultAction, OperationContext context, ModelNode operation) {
                    if(resultAction == OperationContext.ResultAction.KEEP) {
                        result.commit();
                    } else {
                        result.rollback();
                    }
                }

            });
        } catch (PatchingException e) {
            throw new OperationFailedException(e.getMessage(), e);
        } finally {
            //
        }
    }
}
