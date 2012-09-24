/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_SUBSYSTEM_ENDPOINT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers.IntRangeValidatingHandler;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.resource.SocketBindingGroupResourceDefinition;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.domain.controller.descriptions.DomainDescriptionProviders;
import org.jboss.as.domain.controller.operations.DomainServerLifecycleHandlers;
import org.jboss.as.domain.controller.operations.DomainSocketBindingGroupRemoveHandler;
import org.jboss.as.domain.controller.operations.ServerGroupAddHandler;
import org.jboss.as.domain.controller.operations.ServerGroupProfileWriteAttributeHandler;
import org.jboss.as.domain.controller.operations.ServerGroupRemoveHandler;
import org.jboss.as.domain.controller.operations.SocketBindingGroupAddHandler;
import org.jboss.as.domain.controller.operations.deployment.ServerGroupDeploymentReplaceHandler;
import org.jboss.as.domain.controller.resource.DomainDeploymentResourceDescription;
import org.jboss.as.domain.controller.resource.SocketBindingResourceDefinition;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.host.controller.model.jvm.JvmResourceDefinition;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.server.controller.resources.DeploymentAttributes;
import org.jboss.as.server.controller.resources.SystemPropertyResourceDefinition;
import org.jboss.as.server.controller.resources.SystemPropertyResourceDefinition.Location;
import org.jboss.as.server.deploymentoverlay.DeploymentOverlayDefinition;
import org.jboss.as.server.deploymentoverlay.service.DeploymentOverlayPriority;
import org.jboss.as.server.services.net.LocalDestinationOutboundSocketBindingResourceDefinition;
import org.jboss.as.server.services.net.RemoteDestinationOutboundSocketBindingResourceDefinition;


/**
 * Utilities related to the domain model.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class DomainModelUtil {

    /**
     * @deprecated This should be removed once the remaining resources have been converted
     */
    @Deprecated
    public static void initializeDomainRegistry(final ManagementResourceRegistration root, final ExtensibleConfigurationPersister configurationPersister,
                                                 final ContentRepository contentRepo, final HostFileRepository fileRepository, final boolean isMaster,
                                                 final LocalHostControllerInfo hostControllerInfo,
                                                 final ExtensionRegistry extensionRegistry, final IgnoredDomainResourceRegistry ignoredDomainResourceRegistry,
                                                 final PathManagerService pathManager) {



        final ManagementResourceRegistration socketBindingGroup = root.registerSubModel(new SocketBindingGroupResourceDefinition(SocketBindingGroupAddHandler.INSTANCE, DomainSocketBindingGroupRemoveHandler.INSTANCE, true));
        socketBindingGroup.registerSubModel(SocketBindingResourceDefinition.INSTANCE);
        // outbound-socket-binding (for remote destination)
        socketBindingGroup.registerSubModel(RemoteDestinationOutboundSocketBindingResourceDefinition.INSTANCE);
        // outbound-socket-binding (for local destination)
        socketBindingGroup.registerSubModel(LocalDestinationOutboundSocketBindingResourceDefinition.INSTANCE);


        final ManagementResourceRegistration serverGroups = root.registerSubModel(PathElement.pathElement(SERVER_GROUP), DomainDescriptionProviders.SERVER_GROUP);
        serverGroups.registerOperationHandler(ADD, ServerGroupAddHandler.INSTANCE, ServerGroupAddHandler.INSTANCE, false);
        serverGroups.registerOperationHandler(REMOVE, ServerGroupRemoveHandler.INSTANCE, ServerGroupRemoveHandler.INSTANCE, false);
        serverGroups.registerReadWriteAttribute(SOCKET_BINDING_GROUP, null, WriteAttributeHandlers.WriteAttributeOperationHandler.INSTANCE, Storage.CONFIGURATION);
        serverGroups.registerReadWriteAttribute(SOCKET_BINDING_PORT_OFFSET, null, new IntRangeValidatingHandler(0, true), Storage.CONFIGURATION);
        serverGroups.registerReadWriteAttribute(PROFILE, null, ServerGroupProfileWriteAttributeHandler.INSTANCE, Storage.CONFIGURATION);
        serverGroups.registerReadOnlyAttribute(MANAGEMENT_SUBSYSTEM_ENDPOINT, null, Storage.CONFIGURATION);
        DomainServerLifecycleHandlers.registerServerGroupHandlers(serverGroups);

        serverGroups.registerSubModel(JvmResourceDefinition.GLOBAL);

        serverGroups.registerOperationHandler(DeploymentAttributes.SERVER_GROUP_REPLACE_DEPLOYMENT_DEFINITION, new ServerGroupDeploymentReplaceHandler(fileRepository));
        serverGroups.registerSubModel(DomainDeploymentResourceDescription.createForServerGroup(contentRepo, fileRepository));


        // Server Group System Properties
        serverGroups.registerSubModel(SystemPropertyResourceDefinition.createForDomainOrHost(Location.SERVER_GROUP));


        //server group deployment overlay links
        serverGroups.registerSubModel(new DeploymentOverlayDefinition(DeploymentOverlayPriority.SERVER_GROUP, null, null));
    }
}
