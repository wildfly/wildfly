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

import java.io.IOException;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.patching.Constants;
import org.jboss.as.patching.PatchMessages;
import org.jboss.as.patching.installation.InstallationManager;
import org.jboss.as.patching.installation.InstallationManagerService;
import org.jboss.as.patching.installation.PatchableTarget;
import org.jboss.as.patching.metadata.Identity;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchElement;
import org.jboss.as.patching.tool.PatchingHistory;
import org.jboss.as.patching.tool.PatchingHistory.Entry;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * This handler returns the info about specific patch
 *
 * @author Alexey Loubyansky
 */
public class PatchInfoHandler implements OperationStepHandler {

    public static final PatchInfoHandler INSTANCE = new PatchInfoHandler();

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

        final String patchId = PatchResourceDefinition.PATCH_ID.resolveModelAttribute(context, operation).asString();
        final boolean verbose = PatchResourceDefinition.VERBOSE.resolveModelAttribute(context, operation).asBoolean();

        final InstallationManager mgr = getInstallationManager(context);
        if(mgr == null) {
            throw new OperationFailedException(PatchManagementMessages.MESSAGES.failedToLoadIdentity());
        }
        final PatchableTarget.TargetInfo info;
        try {
            info = mgr.getIdentity().loadTargetInfo();
        } catch (IOException e) {
            throw new OperationFailedException(PatchManagementMessages.MESSAGES.failedToLoadIdentity(), e);
        }

        ModelNode result = null;
        final PatchingHistory.Iterator i = PatchingHistory.Factory.iterator(mgr, info);
        while(i.hasNext()) {
            final Entry entry = i.next();
            if(patchId.equals(entry.getPatchId())) {
                result = new ModelNode();
                result.get(Constants.PATCH_ID).set(entry.getPatchId());
                result.get(Constants.TYPE).set(entry.getType().getName());
                result.get(Constants.DESCRIPTION).set(entry.getMetadata().getDescription());
                final Identity identity = entry.getMetadata().getIdentity();
                result.get(Constants.IDENTITY_NAME).set(identity.getName());
                result.get(Constants.IDENTITY_VERSION).set(identity.getVersion());

                if(verbose) {
                    final ModelNode list = result.get(Constants.ELEMENTS).setEmptyList();
                    final Patch metadata = entry.getMetadata();
                    for(PatchElement e : metadata.getElements()) {
                        final ModelNode element = new ModelNode();
                        element.get(Constants.PATCH_ID).set(e.getId());
                        element.get(Constants.TYPE).set(e.getProvider().isAddOn() ? Constants.ADD_ON : Constants.LAYER);
                        element.get(Constants.NAME).set(e.getProvider().getName());
                        element.get(Constants.DESCRIPTION).set(e.getDescription());
                        list.add(element);
                    }
                }

                context.getResult().set(result);
                break;
            }
        }
        if(result == null) {
            context.getFailureDescription().set(PatchMessages.MESSAGES.patchNotFoundInHistory(patchId).getLocalizedMessage());
        }
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    private InstallationManager getInstallationManager(OperationContext ctx) {
        final ServiceController<?> imController = ctx.getServiceRegistry(false).getRequiredService(InstallationManagerService.NAME);
        while (imController != null && imController.getState() == ServiceController.State.UP) {
            try {
                return (InstallationManager) imController.getValue();
            } catch (IllegalStateException e) {
                // ignore, caused by race from WFLY-3505
            }
        }
        return null;
    }
}
