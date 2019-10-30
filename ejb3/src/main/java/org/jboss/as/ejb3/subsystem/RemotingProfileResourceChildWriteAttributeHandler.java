/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RestartParentWriteAttributeHandler;
import org.jboss.as.ejb3.remote.RemotingProfileService;
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
    protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel)
            throws OperationFailedException {
        RemotingProfileAdd.INSTANCE.installServices(context, parentAddress, parentModel);
    }

    @Override
    protected ServiceName getParentServiceName(PathAddress parentAddress) {
        String profileName = null;
        for (final PathElement element : parentAddress) {
            if (element.getKey().equals(EJB3SubsystemModel.REMOTING_PROFILE)) {
                profileName = element.getValue();
            }
        }
        return RemotingProfileService.BASE_SERVICE_NAME.append(profileName);
    }
}
