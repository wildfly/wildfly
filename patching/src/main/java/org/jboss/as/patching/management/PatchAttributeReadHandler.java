/**
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
import org.jboss.as.patching.installation.Identity;
import org.jboss.as.patching.installation.InstallationManager;
import org.jboss.as.patching.installation.InstallationManagerService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

abstract class PatchAttributeReadHandler implements OperationStepHandler {

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

        final ServiceController<?> mgrService = context.getServiceRegistry(false).getRequiredService(InstallationManagerService.NAME);
        final InstallationManager mgr = (InstallationManager) mgrService.getValue();
        final ModelNode result = context.getResult();
        final Identity info = mgr.getIdentity();
        try {
            handle(result, info);
        } catch (IOException e) {
            throw new OperationFailedException(PatchManagementMessages.MESSAGES.failedToLoadIdentity(), e);
        }
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    abstract void handle(ModelNode result, Identity info) throws IOException;
}
