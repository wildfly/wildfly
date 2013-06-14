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

package org.jboss.as.patching.management;

import static org.jboss.as.patching.management.PatchManagementMessages.MESSAGES;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.patching.Constants;
import org.jboss.as.patching.installation.InstallationManager;
import org.jboss.as.patching.installation.InstallationManagerService;
import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.ContentType;
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
        final String patchId = PatchResourceDefinition.PATCH_ID.resolveModelAttribute(context, operation).asString();
        final boolean rollbackTo = PatchResourceDefinition.ROLLBACK_TO.resolveModelAttribute(context, operation).asBoolean();
        final boolean restoreConfiguration = PatchResourceDefinition.RESTORE_CONFIGURATION.resolveModelAttribute(context, operation).asBoolean();

        context.acquireControllerLock();
        final InstallationManager installationManager = (InstallationManager) context.getServiceRegistry(false).getRequiredService(InstallationManagerService.NAME).getValue();

        // FIXME can we check whether the process is reload-required directly from the operation context?
        if (installationManager.requiresRestart()) {
            throw MESSAGES.serverRequiresRestart();
        }

        final PatchTool runner = PatchTool.Factory.create(installationManager);
        final ContentVerificationPolicy policy = PatchTool.Factory.create(operation);
        try {
            // Rollback
            final PatchingResult result = runner.rollback(patchId, policy, rollbackTo, restoreConfiguration);
            installationManager.restartRequired();
            context.restartRequired();
            context.completeStep(new OperationContext.ResultHandler() {

                @Override
                public void handleResult(OperationContext.ResultAction resultAction, OperationContext context, ModelNode operation) {
                    if(resultAction == OperationContext.ResultAction.KEEP) {
                        result.commit();
                    } else {
                        installationManager.clearRestartRequired();
                        context.revertRestartRequired();
                        result.rollback();
                    }
                }

            });
        } catch (PatchingException e) {
            if(e.hasConflicts()) {
                final ModelNode failureDescription = context.getFailureDescription();
                for(final ContentItem item : e.getConflicts()) {
                    final ContentType type = item.getContentType();
                    switch (type) {
                        case BUNDLE:
                            failureDescription.get(Constants.BUNDLES).add(item.getRelativePath());
                            break;
                        case MODULE:
                            failureDescription.get(Constants.MODULES).add(item.getRelativePath());
                            break;
                        case MISC:
                            failureDescription.get(Constants.MISC).add(item.getRelativePath());
                            break;
                    }
                }
                context.stepCompleted();
            } else {
                throw new OperationFailedException(e.getMessage(), e);
            }
        } finally {
            //
        }
    }
}
