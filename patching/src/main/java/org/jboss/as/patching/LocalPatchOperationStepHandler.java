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
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.runner.PatchingException;
import org.jboss.as.patching.runner.PatchingResult;
import org.jboss.as.patching.runner.PatchingTaskRunner;
import org.jboss.dmr.ModelNode;

import java.io.InputStream;

/**
 * @author Emanuel Muckenhuber
 */
public final class LocalPatchOperationStepHandler implements OperationStepHandler {
    public static final OperationStepHandler INSTANCE = new LocalPatchOperationStepHandler();

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        context.acquireControllerLock();
        // Setup
        final PatchInfoService service = (PatchInfoService) context.getServiceRegistry(false).getRequiredService(PatchInfoService.NAME).getValue();

        final PatchInfo info = service.getPatchInfo();
        final DirectoryStructure structure = service.getStructure();
        final PatchingTaskRunner runner = new PatchingTaskRunner(info, structure);

        final InputStream is = context.getAttachmentStream(0);
        try {
            final PatchingResult result = runner.executeDirect(is);
            final PatchInfo newInfo = result.getPatchInfo();
            service.setPatchInfo(info, newInfo);
            context.completeStep(new OperationContext.RollbackHandler() {
                @Override
                public void handleRollback(OperationContext context, ModelNode operation) {
                    result.rollback();
                }
            });
        } catch (PatchingException e) {
            throw new OperationFailedException(e.getMessage(), e);
        }
    }

}
