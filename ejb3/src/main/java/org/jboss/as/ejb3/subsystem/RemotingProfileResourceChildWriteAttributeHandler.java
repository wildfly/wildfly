/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RestartParentWriteAttributeHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * A write handler for the "value" attribute of the channel creation option
 *
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class RemotingProfileResourceChildWriteAttributeHandler extends RestartParentWriteAttributeHandler {

    public RemotingProfileResourceChildWriteAttributeHandler(final AttributeDefinition attributeDefinition) {
        super(EJB3SubsystemModel.REMOTING_PROFILE, attributeDefinition);
    }

    @Override
    protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel) throws OperationFailedException {
        RemotingProfileResourceDefinition.ADD_HANDLER.installServices(context, parentAddress, parentModel);
    }

    @Override
    protected ServiceName getParentServiceName(PathAddress parentAddress) {
        return RemotingProfileResourceDefinition.REMOTING_PROFILE_CAPABILITY.getCapabilityServiceName(parentAddress);
    }
}
