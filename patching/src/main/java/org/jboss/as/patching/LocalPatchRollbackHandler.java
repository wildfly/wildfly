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

import static org.jboss.as.patching.PatchResourceDefinition.PATCH_ID;
import static org.jboss.as.patching.PatchResourceDefinition.RESTORE_CONFIGURATION;
import static org.jboss.as.patching.PatchResourceDefinition.ROLLBACK_TO;
import static org.jboss.as.patching.PatchMessages.MESSAGES;

import org.jboss.as.boot.DirectoryStructure;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.patching.runner.ContentVerificationPolicy;
import org.jboss.as.patching.runner.PatchingException;
import org.jboss.as.patching.runner.PatchingResult;
import org.jboss.as.patching.tool.PatchTool;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
public class LocalPatchRollbackHandler implements OperationStepHandler {

    public static final LocalPatchRollbackHandler INSTANCE = new LocalPatchRollbackHandler();

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final String patchId = PATCH_ID.resolveModelAttribute(context, operation).asString();
        final boolean rollbackTo = ROLLBACK_TO.resolveModelAttribute(context, operation).asBoolean();
        final boolean restoreConfiguration = RESTORE_CONFIGURATION.resolveModelAttribute(context, operation).asBoolean();

        // FIXME can we check whether the process is reload-required directly from the operation context?
        context.acquireControllerLock();
        final PatchInfoService service = (PatchInfoService) context.getServiceRegistry(false).getRequiredService(PatchInfoService.NAME).getValue();
        if (service.requiresReload()) {
            throw MESSAGES.serverRequiresReload();
        }

        final PatchInfo info = service.getPatchInfo();
        final DirectoryStructure structure = service.getStructure();
        final PatchTool runner = PatchTool.Factory.create(info, structure);
        final ContentVerificationPolicy policy = PatchTool.Factory.create(operation);
        try {
            // Rollback
            final PatchingResult result = runner.rollback(patchId, policy, rollbackTo, restoreConfiguration);
            context.completeStep(new OperationContext.ResultHandler() {

                @Override
                public void handleResult(OperationContext.ResultAction resultAction, OperationContext context, ModelNode operation) {
                    if(resultAction == OperationContext.ResultAction.KEEP) {
                        service.reloadRequired();
                        context.restartRequired();
                        result.commit();
                    } else {
                        result.rollback();
                    }
                }

            });
        } catch (PatchingException e) {
            if(e.hasConflicts()) {
                // TODO report conflicting items
            }
            throw new OperationFailedException(e.getMessage(), e);
        } finally {
            //
        }
    }
}
