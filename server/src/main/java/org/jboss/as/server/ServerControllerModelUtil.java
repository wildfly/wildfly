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
package org.jboss.as.server;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVICE_CONTAINER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.descriptions.common.CoreManagementDefinition;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.ExtensionResourceDefinition;
import org.jboss.as.controller.operations.common.SocketBindingGroupRemoveHandler;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.resource.InterfaceDefinition;
import org.jboss.as.controller.resource.SocketBindingGroupResourceDefinition;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.domain.management.connections.ldap.LdapConnectionResourceDefinition;
import org.jboss.as.domain.management.security.SecurityRealmResourceDefinition;
import org.jboss.as.platform.mbean.PlatformMBeanResourceRegistrar;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.server.controller.resources.DeploymentAttributes;
import org.jboss.as.server.controller.resources.ServerDeploymentResourceDescription;
import org.jboss.as.server.controller.resources.SystemPropertyResourceDefinition;
import org.jboss.as.server.controller.resources.VaultResourceDefinition;
import org.jboss.as.server.deploymentoverlay.DeploymentOverlayDefinition;
import org.jboss.as.server.deploymentoverlay.service.DeploymentOverlayPriority;
import org.jboss.as.server.mgmt.HttpManagementResourceDefinition;
import org.jboss.as.server.mgmt.NativeManagementResourceDefinition;
import org.jboss.as.server.mgmt.NativeRemotingManagementResourceDefinition;
import org.jboss.as.server.operations.DumpServicesHandler;
import org.jboss.as.server.services.net.BindingGroupAddHandler;
import org.jboss.as.server.services.net.LocalDestinationOutboundSocketBindingResourceDefinition;
import org.jboss.as.server.services.net.NetworkInterfaceRuntimeHandler;
import org.jboss.as.server.services.net.RemoteDestinationOutboundSocketBindingResourceDefinition;
import org.jboss.as.server.services.net.SocketBindingResourceDefinition;
import org.jboss.as.server.services.net.SpecifiedInterfaceAddHandler;
import org.jboss.as.server.services.net.SpecifiedInterfaceRemoveHandler;
import org.jboss.as.server.services.net.SpecifiedInterfaceResolveHandler;
import org.jboss.as.server.services.security.AbstractVaultReader;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author David Bosschaert
 */
public class ServerControllerModelUtil {

    public static void initOperations(final ManagementResourceRegistration root,
                                      final ContentRepository contentRepository,
                                      final ExtensibleConfigurationPersister extensibleConfigurationPersister,
                                      final ServerEnvironment serverEnvironment,
                                      final ControlledProcessState processState,
                                      final RunningModeControl runningModeControl,
                                      final AbstractVaultReader vaultReader,
                                      final ExtensionRegistry extensionRegistry,
                                      final boolean parallelBoot,
                                      final PathManagerService pathManager) {

        // System Properties
        root.registerSubModel(SystemPropertyResourceDefinition.createForStandaloneServer(serverEnvironment));

        //vault
        root.registerSubModel(new VaultResourceDefinition(vaultReader));

        // Central Management
        // Start with the base /core-service=management MNR. The Resource for this is added by ServerService itself, so there is no add/remove op handlers
        ManagementResourceRegistration management = root.registerSubModel(CoreManagementDefinition.INSTANCE);

        management.registerSubModel(SecurityRealmResourceDefinition.INSTANCE);
        management.registerSubModel(LdapConnectionResourceDefinition.INSTANCE);
        management.registerSubModel(NativeManagementResourceDefinition.INSTANCE);
        management.registerSubModel(NativeRemotingManagementResourceDefinition.INSTANCE);
        management.registerSubModel(HttpManagementResourceDefinition.INSTANCE);

        // Other core services
        ManagementResourceRegistration serviceContainer = root.registerSubModel(
                new SimpleResourceDefinition(PathElement.pathElement(CORE_SERVICE, SERVICE_CONTAINER), ControllerResolver.getResolver("core", SERVICE_CONTAINER)));
        serviceContainer.registerOperationHandler(DumpServicesHandler.DEFINITION, DumpServicesHandler.INSTANCE);

        // Platform MBeans
        PlatformMBeanResourceRegistrar.registerPlatformMBeanResources(root);

        // Paths
        root.registerSubModel(PathResourceDefinition.createSpecified(pathManager));

        // Interfaces
        ManagementResourceRegistration interfaces = root.registerSubModel(new InterfaceDefinition(
                SpecifiedInterfaceAddHandler.INSTANCE,
                SpecifiedInterfaceRemoveHandler.INSTANCE,
                true
        ));
        interfaces.registerReadOnlyAttribute(NetworkInterfaceRuntimeHandler.RESOLVED_ADDRESS, NetworkInterfaceRuntimeHandler.INSTANCE);
        interfaces.registerOperationHandler(SpecifiedInterfaceResolveHandler.DEFINITION, SpecifiedInterfaceResolveHandler.INSTANCE);

        //TODO socket-binding-group currently lives in controller and the child RDs live in server so they currently need passing in from here
        root.registerSubModel(new SocketBindingGroupResourceDefinition(BindingGroupAddHandler.INSTANCE,
                                        SocketBindingGroupRemoveHandler.INSTANCE,
                                        false,
                                        SocketBindingResourceDefinition.INSTANCE,
                                        RemoteDestinationOutboundSocketBindingResourceDefinition.INSTANCE,
                                        LocalDestinationOutboundSocketBindingResourceDefinition.INSTANCE));

        // Deployments
        ManagementResourceRegistration deployments = root.registerSubModel(ServerDeploymentResourceDescription.create(contentRepository, vaultReader));

        //deployment overlays
        root.registerSubModel(new DeploymentOverlayDefinition(DeploymentOverlayPriority.SERVER, contentRepository, null));

        // The sub-deployments registry
        deployments.registerSubModel(new SimpleResourceDefinition(PathElement.pathElement(SUBDEPLOYMENT), DeploymentAttributes.DEPLOYMENT_RESOLVER));

        // Extensions
        root.registerSubModel(new ExtensionResourceDefinition(extensionRegistry, parallelBoot, false));
        if (extensionRegistry != null) {
            //extension registry may be null during testing
            extensionRegistry.setSubsystemParentResourceRegistrations(root, deployments);
            extensionRegistry.setPathManager(pathManager);
        }

        // Util
        root.registerOperationHandler(DeployerChainAddHandler.DEFINITION, DeployerChainAddHandler.INSTANCE, false);
    }

    static ProcessType getProcessType(ServerEnvironment serverEnvironment) {
        if (serverEnvironment != null) {
            switch (serverEnvironment.getLaunchType()) {
                case DOMAIN:
                    return ProcessType.DOMAIN_SERVER;
                case STANDALONE:
                    return ProcessType.STANDALONE_SERVER;
                case EMBEDDED:
                    return ProcessType.EMBEDDED_SERVER;
                case APPCLIENT:
                    return ProcessType.APPLICATION_CLIENT;
            }
        }

        return ProcessType.EMBEDDED_SERVER;
    }
}
