/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
        if (attributeName.equals(CREDENTIAL_REFERENCE.getName())) {
            applyCredentialReferenceUpdateToRuntime(context, operation, resolvedValue, currentValue, attributeName);
        }
        return super.applyUpdateToRuntime(context, operation, attributeName, resolvedValue, currentValue, handbackHolder);
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
        MailSessionAdd.installSessionProviderService(context, parentAddress, parentModel);
    }

    @Override
    protected ServiceName getParentServiceName(PathAddress parentAddress) {
        return MailSessionDefinition.SESSION_CAPABILITY.getCapabilityServiceName(parentAddress).append("provider");
    }
}
