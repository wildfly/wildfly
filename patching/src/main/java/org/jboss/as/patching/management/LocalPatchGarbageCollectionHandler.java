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

import java.io.File;
import java.io.IOException;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.patching.installation.InstallationManager;
import org.jboss.as.patching.installation.InstallationManagerService;
import org.jboss.as.patching.installation.InstalledImage;
import org.jboss.as.patching.installation.PatchableTarget;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * @author Emanuel Muckenhuber
 */
public class LocalPatchGarbageCollectionHandler implements OperationStepHandler {

    public static final LocalPatchGarbageCollectionHandler INSTANCE = new LocalPatchGarbageCollectionHandler();

    private final String PATCH_ID = PatchResourceDefinition.PATCH_ID.getName();

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final String patchId = operation.require(PATCH_ID).asString();
        //
        context.acquireControllerLock();
        final ServiceController<?> mgrService = context.getServiceRegistry(false).getRequiredService(InstallationManagerService.NAME);
        final InstallationManager mgr = (InstallationManager) mgrService.getValue();
        final PatchableTarget.TargetInfo info;
        try {
            info = mgr.getIdentity().loadTargetInfo();
        } catch (IOException e) {
            throw new OperationFailedException("failed to load identity info", e);
        }
        if(info.getCumulativePatchID().equals(patchId)) {
            throw PatchManagementMessages.MESSAGES.patchActive(patchId);
        }
        if(info.getPatchIDs().contains(patchId)) {
            throw PatchManagementMessages.MESSAGES.patchActive(patchId);
        }
        final InstalledImage installedImage = info.getDirectoryStructure().getInstalledImage();

        // Remove directories
        final File history = installedImage.getPatchHistoryDir(patchId);
        if(history.exists()) {
            recursiveDelete(history);
        }
        // Remove patch contents
        final File patchRoot = installedImage.getPatchHistoryDir(patchId);
        if(patchRoot.exists()) {
            recursiveDelete(patchRoot);
        }

        // TODO perhaps recursively remove one-off patches in case this targets a CP

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    static boolean recursiveDelete(File root) {
        boolean ok = true;
        if (root.isDirectory()) {
            final File[] files = root.listFiles();
            for (File file : files) {
                ok &= recursiveDelete(file);
            }
            return ok && (root.delete() || !root.exists());
        } else {
            ok &= root.delete() || !root.exists();
        }
        return ok;
    }

}
