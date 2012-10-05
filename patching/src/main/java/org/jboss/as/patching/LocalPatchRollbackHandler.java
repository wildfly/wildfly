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

import org.jboss.as.boot.DirectoryStructure;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.patching.runner.PatchingException;
import org.jboss.as.patching.runner.PatchingTaskRunner;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
public class LocalPatchRollbackHandler implements OperationStepHandler {

    public static final LocalPatchRollbackHandler INSTANCE = new LocalPatchRollbackHandler();

    private final String PATCH_ID = CommonAttributes.PATCH_ID.getName();

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final String patchId = operation.require(PATCH_ID).asString();
        //
        context.acquireControllerLock();
        final PatchInfoService service = (PatchInfoService) context.getServiceRegistry(false).getRequiredService(PatchInfoService.NAME).getValue();

        final PatchInfo info = service.getPatchInfo();
        final DirectoryStructure structure = service.getStructure();
        final PatchingTaskRunner taskRunner = new PatchingTaskRunner(info, structure);
        try {
            // Rollback
            taskRunner.rollback(patchId);
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        } catch (PatchingException e) {
            throw new OperationFailedException(e.getMessage(), e);
        } finally {
            //
        }
    }
}
