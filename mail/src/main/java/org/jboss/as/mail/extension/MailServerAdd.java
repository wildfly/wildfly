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

package org.jboss.as.mail.extension;

import static org.jboss.as.controller.security.CredentialReference.handleCredentialReferenceUpdate;
import static org.jboss.as.controller.security.CredentialReference.rollbackCredentialStoreUpdate;

import java.util.List;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RestartParentResourceAddHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * @author Tomaz Cerar
 * @created 8.12.11 0:19
 */
class MailServerAdd extends RestartParentResourceAddHandler {

    MailServerAdd(AttributeDefinition[] attributes) {
        super(MailSubsystemModel.MAIL_SESSION, Set.of(), List.of(attributes));
    }

    @Override
    protected void updateModel(OperationContext context, ModelNode operation) throws OperationFailedException {
        final Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
        populateModel(operation, resource.getModel());
        handleCredentialReferenceUpdate(context, resource.getModel());
        recordCapabilitiesAndRequirements(context, operation, resource);
    }

    @Override
    protected boolean isResourceServiceRestartAllowed(OperationContext context, ServiceController<?> service) {
        // Perform runtime operations here, as distinct from rollbackRuntime(...)
        ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        try {
            ModelNode resolvedValue = MailServerDefinition.CREDENTIAL_REFERENCE.resolveModelAttribute(context, model);
            if (resolvedValue.isDefined()) {
                // This call will force the creation of the new alias in the credential-store if it is needed
                CredentialReference.getCredentialSourceSupplier(context, MailServerDefinition.CREDENTIAL_REFERENCE, model, null);
            }
        } catch (OperationFailedException e) {
            throw new IllegalStateException(e);
        }
        return super.isResourceServiceRestartAllowed(context, service);
    }

    @Override
    protected void rollbackRuntime(OperationContext context, final ModelNode operation, final Resource resource) {
        rollbackCredentialStoreUpdate(MailServerDefinition.CREDENTIAL_REFERENCE, context, resource);
    }

    @Override
    protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel) throws OperationFailedException {
        MailSessionAdd.installSessionProviderService(context, parentAddress, parentModel);
    }

    @Override
    protected ServiceName getParentServiceName(PathAddress parentAddress) {
        return MailSessionDefinition.SESSION_CAPABILITY.getCapabilityServiceName(parentAddress).append("provider");
    }
}
