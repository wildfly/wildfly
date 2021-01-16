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

import static org.jboss.as.controller.security.CredentialReference.applyCredentialReferenceUpdateToRuntime;
import static org.jboss.as.controller.security.CredentialReference.handleCredentialReferenceUpdate;
import static org.jboss.as.controller.security.CredentialReference.rollbackCredentialStoreUpdate;
import static org.jboss.as.mail.extension.MailServerDefinition.CREDENTIAL_REFERENCE;

import java.util.Collection;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RestartParentWriteAttributeHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * @author Tomaz Cerar
 * @created 22.12.11 18:31
 */
class MailServerWriteAttributeHandler extends RestartParentWriteAttributeHandler {

    MailServerWriteAttributeHandler(AttributeDefinition... attributeDefinitions) {
        super(MailSubsystemModel.MAIL_SESSION, attributeDefinitions);
    }

    MailServerWriteAttributeHandler(Collection<AttributeDefinition> attributeDefinitions) {
        super(MailSubsystemModel.MAIL_SESSION, attributeDefinitions);
    }

    @Override
    protected void finishModelStage(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue,
                                    ModelNode oldValue, Resource resource) throws OperationFailedException {
        super.finishModelStage(context, operation, attributeName, newValue, oldValue, resource);
        if (attributeName.equals(CREDENTIAL_REFERENCE.getName())) {
            handleCredentialReferenceUpdate(context, resource.getModel().get(attributeName), attributeName);
        }
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<ModelNode> handbackHolder) throws OperationFailedException {
        boolean requiresReload = false;
        if (attributeName.equals(CREDENTIAL_REFERENCE.getName())) {
            requiresReload = applyCredentialReferenceUpdateToRuntime(context, operation, resolvedValue, currentValue, attributeName);
        }
        return super.applyUpdateToRuntime(context, operation, attributeName, resolvedValue, currentValue, handbackHolder) || requiresReload;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode resolvedValue, ModelNode invalidatedParentModel) throws OperationFailedException {
        if (attributeName.equals(CREDENTIAL_REFERENCE.getName())) {
            rollbackCredentialStoreUpdate(MailServerDefinition.CREDENTIAL_REFERENCE, context, resolvedValue);
        }
        super.revertUpdateToRuntime(context, operation, attributeName, valueToRestore, resolvedValue, invalidatedParentModel);
    }

    @Override
    protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel) throws OperationFailedException {
        MailSessionAdd.installRuntimeServices(context, parentAddress, parentModel);
    }

    @Override
    protected ServiceName getParentServiceName(PathAddress parentAddress) {
        return MailSessionDefinition.SESSION_CAPABILITY.getCapabilityServiceName(parentAddress.getLastElement().getValue());
    }

    @Override
    protected void removeServices(OperationContext context, ServiceName parentService, ModelNode parentModel) throws OperationFailedException {
        super.removeServices(context, parentService, parentModel);
        String jndiName = MailSessionAdd.getJndiName(parentModel, context);
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
        context.removeService(bindInfo.getBinderServiceName());
    }
}
