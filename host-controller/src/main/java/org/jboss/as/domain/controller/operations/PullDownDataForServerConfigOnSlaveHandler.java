/*
 *
 *  * JBoss, Home of Professional Open Source.
 *  * Copyright 2013, Red Hat, Inc., and individual contributors
 *  * as indicated by the @author tags. See the copyright.txt file in the
 *  * distribution for a full listing of individual contributors.
 *  *
 *  * This is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */
package org.jboss.as.domain.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;

import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.ResultAction;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.Transformers;
import org.jboss.as.host.controller.HostControllerMessages;
import org.jboss.as.host.controller.IgnoredNonAffectedServerGroupsUtil;
import org.jboss.as.host.controller.IgnoredNonAffectedServerGroupsUtil.ServerConfigInfo;
import org.jboss.as.host.controller.mgmt.DomainControllerRuntimeIgnoreTransformationRegistry;
import org.jboss.dmr.ModelNode;

/**
 * Executed on the DC when a slave's server-config has its server-group or socket-binding-group override changed
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class PullDownDataForServerConfigOnSlaveHandler implements OperationStepHandler {

    public static String OPERATION_NAME = "slave-server-config-change";

    protected final String host;
    protected final Transformers transformers;
    protected final DomainControllerRuntimeIgnoreTransformationRegistry runtimeIgnoreTransformationRegistry;

    public PullDownDataForServerConfigOnSlaveHandler(final String host, final Transformers transformers, DomainControllerRuntimeIgnoreTransformationRegistry runtimeIgnoreTransformationRegistry) {
        this.host = host;
        this.transformers = transformers;
        this.runtimeIgnoreTransformationRegistry = runtimeIgnoreTransformationRegistry;
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        context.acquireControllerLock();

        final ServerConfigInfo serverInfo = IgnoredNonAffectedServerGroupsUtil.createServerConfigInfo(operation.require(SERVER));

        final Resource root = context.readResource(PathAddress.EMPTY_ADDRESS);

        final Set<PathElement> unknownElements = new HashSet<>();

        //Check the server group exists and if it is unknown
        final String serverGroupName = serverInfo.getServerGroup();
        final PathElement serverGroupElement = PathElement.pathElement(SERVER_GROUP, serverGroupName);
        final Resource serverGroupResource = root.getChild(serverGroupElement);
        if (serverGroupResource == null) {
            throw HostControllerMessages.MESSAGES.noResourceFor(PathAddress.pathAddress(serverGroupElement));
        }

        String serverGroupSocketBindingGroup = null;
        if (!runtimeIgnoreTransformationRegistry.isServerGroupKnown(root, host, serverGroupName)) {
            unknownElements.add(serverGroupElement);

            //Look at the things brought in by the server group and see if they also need adding
            final ModelNode serverGroupModel = serverGroupResource.getModel();

            //server-group's profile
            final String profileName = serverGroupModel.get(PROFILE).asString();
            final PathElement profileElement = PathElement.pathElement(PROFILE, profileName);
            final Resource profileResource = root.getChild(profileElement);
            if (profileResource == null) {
                throw HostControllerMessages.MESSAGES.noResourceFor(PathAddress.pathAddress(profileElement));
            }

            if (!runtimeIgnoreTransformationRegistry.isProfileKnown(root, host, profileName)) {
                unknownElements.add(profileElement);
                Set<PathElement> unknownExtensions = runtimeIgnoreTransformationRegistry.getUnknownExtensionsForProfile(root, host, profileName);
                unknownElements.addAll(unknownExtensions);

            }

            //server-group's socket-binding group
            if (serverGroupModel.hasDefined(SOCKET_BINDING_GROUP)) {
                serverGroupSocketBindingGroup = serverGroupModel.get(SOCKET_BINDING_GROUP).asString();
                addSocketBindingGroup(unknownElements, root, serverGroupSocketBindingGroup);
            }

        }

        if (serverInfo.getSocketBindingGroup() != null && !serverInfo.getSocketBindingGroup().equals(serverGroupSocketBindingGroup)) {
            addSocketBindingGroup(unknownElements, root, serverInfo.getSocketBindingGroup());
        }

        final ReadMasterDomainModelUtil readUtil = ReadMasterDomainModelUtil.readMasterDomainResourcesForSlaveRequest(context, unknownElements, transformers, root, runtimeIgnoreTransformationRegistry);
        context.getResult().set(readUtil.getDescribedResources());
        context.completeStep(new OperationContext.ResultHandler() {
            @Override
            public void handleResult(ResultAction resultAction, OperationContext context, ModelNode operation) {
                if (resultAction == ResultAction.KEEP) {
                    //Since this method gets invoked as part of a transaction on the slave, gather the known resources and record them once the tx completes
                    runtimeIgnoreTransformationRegistry.addKnownDataForSlave(host, readUtil.getNewKnownRootResources());
                    runtimeIgnoreTransformationRegistry.updateSlaveServerConfig(host, serverInfo);
                }
            }
        });
    }

    private void addSocketBindingGroup(Set<PathElement> unknownElements, Resource root, String socketBindingGroup) throws OperationFailedException {
        final PathElement socketBindingGroupElement = PathElement.pathElement(SOCKET_BINDING_GROUP, socketBindingGroup);
        final Resource socketBindingGroupResource = root.getChild(socketBindingGroupElement);
        if (socketBindingGroupResource == null) {
            throw HostControllerMessages.MESSAGES.noResourceFor(PathAddress.pathAddress(socketBindingGroupElement));
        }
        if (!runtimeIgnoreTransformationRegistry.isSocketBindingGroupKnown(root, host, socketBindingGroup)) {
            unknownElements.add(socketBindingGroupElement);
        }
    }

}
