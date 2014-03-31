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

package org.wildfly.extension.picketlink.federation.model.idp;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RestartParentResourceRemoveHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.federation.service.IdentityProviderService;

/**
 * <p>This remove handler is used during the removal of all partition-manager child resources. Its purpose is restart the
 * identity store services prior to the child removal, so we can stop all store services properly before restarting the parent.</p>
 *
 * @author Pedro Silva
 */
public class IdentityProviderConfigRemoveStepHandler extends RestartParentResourceRemoveHandler {

    static final IdentityProviderConfigRemoveStepHandler INSTANCE = new IdentityProviderConfigRemoveStepHandler();

    private IdentityProviderConfigRemoveStepHandler() {
        super(ModelElement.IDENTITY_PROVIDER.getName());
    }

    @Override
    protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel, ServiceVerificationHandler verificationHandler) throws OperationFailedException {
        IdentityProviderAddHandler.launchServices(context, parentModel, verificationHandler, null, parentAddress);
    }

    @Override
    protected ServiceName getParentServiceName(PathAddress parentAddress) {
        return IdentityProviderService.createServiceName(parentAddress.getLastElement().getValue());
    }

}
