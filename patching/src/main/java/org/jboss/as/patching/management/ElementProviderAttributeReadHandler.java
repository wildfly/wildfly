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


import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.patching.PatchMessages;
import org.jboss.as.patching.installation.AddOn;
import org.jboss.as.patching.installation.InstallationManager;
import org.jboss.as.patching.installation.InstallationManagerService;
import org.jboss.as.patching.installation.InstalledIdentity;
import org.jboss.as.patching.installation.Layer;
import org.jboss.as.patching.installation.PatchableTarget;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * @author Alexey Loubyansky
 */
abstract class ElementProviderAttributeReadHandler implements OperationStepHandler {

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
        final PathElement element = address.getLastElement();
        final String name = element.getValue();

        final ServiceController<?> mgrService = context.getServiceRegistry(false).getRequiredService(InstallationManagerService.NAME);
        final InstallationManager mgr = (InstallationManager) mgrService.getValue();
        PatchableTarget target = getProvider(name, mgr);
        final ModelNode result = context.getResult();
        handle(result, target);
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    protected abstract PatchableTarget getProvider(final String name, final InstalledIdentity identity) throws OperationFailedException;

    abstract void handle(ModelNode result, PatchableTarget layer) throws OperationFailedException;

    /**
     * @author Alexey Loubyansky
     */
    abstract static class AddOnAttributeReadHandler extends ElementProviderAttributeReadHandler {

        @Override
        protected PatchableTarget getProvider(final String name, final InstalledIdentity identity) throws OperationFailedException {
            final AddOn target = identity.getAddOn(name);
            if (target == null) {
                throw new OperationFailedException(PatchMessages.MESSAGES.noSuchLayer(name).getLocalizedMessage());
            }
            return target;
        }
    }

    /**
     * @author Alexey Loubyansky
     */
    abstract static class LayerAttributeReadHandler extends ElementProviderAttributeReadHandler {

        @Override
        protected Layer getProvider(final String name, final InstalledIdentity identity) throws OperationFailedException {
            final Layer target = identity.getLayer(name);
            if (target == null) {
                throw new OperationFailedException(PatchMessages.MESSAGES.noSuchLayer(name).getLocalizedMessage());
            }
            return target;
        }
    }
}
