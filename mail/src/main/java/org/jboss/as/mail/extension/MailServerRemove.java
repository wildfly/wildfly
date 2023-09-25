/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.mail.extension;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RestartParentResourceRemoveHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * Remove attribute operation handler for attributes of the server resources.
 * @author Paul Ferraro
 */
class MailServerRemove extends RestartParentResourceRemoveHandler {

    MailServerRemove() {
        super(MailSubsystemModel.MAIL_SESSION);
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