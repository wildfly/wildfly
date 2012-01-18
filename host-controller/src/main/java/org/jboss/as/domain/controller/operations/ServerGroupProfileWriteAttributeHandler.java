/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.domain.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers.StringLengthValidatingHandler;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.Resource.ResourceEntry;
import org.jboss.as.domain.controller.DomainControllerMessages;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.controller.ServerIdentity;
import org.jboss.as.domain.controller.operations.coordination.DomainServerUtils;
import org.jboss.as.server.operations.ServerRestartRequiredHandler;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ServerGroupProfileWriteAttributeHandler extends StringLengthValidatingHandler {

    private final ImmutableManagementResourceRegistration rootResourceRegistration;
    private final LocalHostControllerInfo localHostControllerInfo;

    public ServerGroupProfileWriteAttributeHandler(final ImmutableManagementResourceRegistration rootResourceRegistration, final LocalHostControllerInfo localHostControllerInfo) {
        super(1, false);
        this.localHostControllerInfo = localHostControllerInfo;
        this.rootResourceRegistration = rootResourceRegistration;
    }

    @Override
    protected void modelChanged(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue,
            ModelNode currentValue) throws OperationFailedException {
        if (!newValue.equals(currentValue)) {
            validateProfileName(context, newValue.asString());

            final Resource rootResource = context.getRootResource();
            final Resource hostResource = rootResource.getChild(PathElement.pathElement(HOST, localHostControllerInfo.getLocalHostName()));

            final String groupName = PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement().getValue();

            final Set<ServerIdentity> affectedServers = getServersForServerGroup(hostResource, groupName);

            if (!affectedServers.isEmpty()) {
                final ModelNode hostModel = Resource.Tools.readModel(hostResource);
                final Map<String, ProxyController> serverProxies = DomainServerUtils.getServerProxies(localHostControllerInfo.getLocalHostName(), rootResource, rootResourceRegistration);
                final Set<ServerIdentity> runningServers = DomainServerUtils.getAllRunningServers(hostModel, localHostControllerInfo.getLocalHostName(), serverProxies);

                for (ServerIdentity serverIdentity : affectedServers) {
                    if(!runningServers.contains(serverIdentity)) {
                        continue;
                    }
                    final PathAddress serverAddress = PathAddress.pathAddress(PathElement.pathElement(HOST, serverIdentity.getHostName()), PathElement.pathElement(SERVER, serverIdentity.getServerName()));
                    final OperationStepHandler handler = rootResourceRegistration.getOperationHandler(serverAddress, ServerRestartRequiredHandler.OPERATION_NAME);
                    final ModelNode op = new ModelNode();
                    op.get(OP).set(ServerRestartRequiredHandler.OPERATION_NAME);
                    op.get(OP_ADDR).set(serverAddress.toModelNode());

                    context.addStep(op, handler, OperationContext.Stage.IMMEDIATE);
                }
            }
        }

        context.completeStep();
    }

    private void validateProfileName(final OperationContext context, final String profileName) throws OperationFailedException {
        final Resource profile = context.getOriginalRootResource().getChild(PathElement.pathElement(PROFILE, profileName));
        if (profile == null) {
            throw DomainControllerMessages.MESSAGES.noProfileCalled(profileName);
        }
    }

    private Set<ServerIdentity> getServersForServerGroup(final Resource hostResource, final String group){
        final HashSet<ServerIdentity> servers = new HashSet<ServerIdentity>();
        for (ResourceEntry entry : hostResource.getChildren(SERVER_CONFIG)) {
            final ModelNode config = entry.getModel();
            final String configuredGroup = config.require(GROUP).asString();
            if (configuredGroup.equals(group)) {
                final ServerIdentity serverIdentity = new ServerIdentity(localHostControllerInfo.getLocalHostName(), group, entry.getName());
                servers.add(serverIdentity);
            }
        }
        return servers;
    }
}
