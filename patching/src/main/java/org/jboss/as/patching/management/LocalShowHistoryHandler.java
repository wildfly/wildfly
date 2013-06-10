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

import static org.jboss.as.patching.PatchInfo.BASE;
import static org.jboss.as.patching.metadata.Patch.PatchType.CUMULATIVE;
import static org.jboss.as.patching.metadata.Patch.PatchType.ONE_OFF;

import java.io.File;
import java.util.List;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.patching.Constants;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.installation.InstalledImage;
import org.jboss.as.patching.metadata.Patch.PatchType;
import org.jboss.as.patching.runner.PatchUtils;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012, Red Hat Inc
 */
public final class LocalShowHistoryHandler implements OperationStepHandler {
    public static final OperationStepHandler INSTANCE = new LocalShowHistoryHandler();

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        context.acquireControllerLock();
        // Setup
        final PatchInfoService service = (PatchInfoService) context.getServiceRegistry(false).getRequiredService(PatchInfoService.NAME).getValue();

        try {
            final PatchInfo info = service.getPatchInfo();
            final InstalledImage installedImage =  service.getStructure().getInstalledImage();

            ModelNode result = new ModelNode();
            result.setEmptyList();

            String cumulativePatchID = info.getCumulativeID();
            if (!BASE.equals(cumulativePatchID)) {
                fillHistory(result, CUMULATIVE, cumulativePatchID, installedImage.getPatchHistoryDir(cumulativePatchID));
            }

            List<String> oneOffPatchIDs = info.getPatchIDs();
            for (String oneOffPatchID : oneOffPatchIDs) {
                File historyDir = installedImage.getPatchHistoryDir(oneOffPatchID);
                fillHistory(result, ONE_OFF, oneOffPatchID, historyDir);
            }
            context.getResult().set(result);
            context.stepCompleted();
        } catch (Throwable t) {
            t.printStackTrace();
            throw PatchManagementMessages.MESSAGES.failedToShowHistory(t);
        }
    }

    private void fillHistory(ModelNode result, PatchType type, String oneOffPatchID, File historyDir) throws Exception {
        ModelNode history = new ModelNode();
        history.get(type.getName()).set(oneOffPatchID);

        File timestampFile = new File(historyDir, Constants.TIMESTAMP);
        String timestamp = PatchUtils.readRef(timestampFile);
        history.get(Constants.APPLIED_AT).set(timestamp);
        result.add(history);
    }

}
