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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PERSISTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVICE_CONTAINER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;

import java.util.EnumSet;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.ExtensionResourceDefinition;
import org.jboss.as.controller.operations.common.InterfaceCriteriaWriteHandler;
import org.jboss.as.controller.operations.common.SocketBindingGroupRemoveHandler;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry.EntryType;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.as.controller.resource.SocketBindingGroupResourceDefinition;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.domain.management.connections.ldap.LdapConnectionResourceDefinition;
import org.jboss.as.domain.management.security.SecurityRealmResourceDefinition;
import org.jboss.as.platform.mbean.PlatformMBeanResourceRegistrar;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.server.controller.descriptions.ServerDescriptionProviders;
import org.jboss.as.server.controller.resources.SystemPropertyResourceDefinition;
import org.jboss.as.server.controller.resources.VaultResourceDefinition;
import org.jboss.as.server.deployment.DeploymentAddHandler;
import org.jboss.as.server.deployment.DeploymentDeployHandler;
import org.jboss.as.server.deployment.DeploymentRedeployHandler;
import org.jboss.as.server.deployment.DeploymentRemoveHandler;
import org.jboss.as.server.deployment.DeploymentStatusHandler;
import org.jboss.as.server.deployment.DeploymentUndeployHandler;
import org.jboss.as.server.deploymentoverlay.ContentDefinition;
import org.jboss.as.server.deploymentoverlay.DeploymentOverlayDefinition;
import org.jboss.as.server.deploymentoverlay.DeploymentOverlayDeploymentDefinition;
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

        EnumSet<Flag> runtimeOnlyFlag = EnumSet.of(Flag.RUNTIME_ONLY);


        // System Properties
        root.registerSubModel(SystemPropertyResourceDefinition.createForStandaloneServer(serverEnvironment));

        //vault
        root.registerSubModel(new VaultResourceDefinition(vaultReader));

        // Central Management
        // Start with the base /core-service=management MNR. The Resource for this is added by ServerService itself, so there is no add/remove op handlers
        ManagementResourceRegistration management = root.registerSubModel(PathElement.pathElement(CORE_SERVICE, MANAGEMENT), CommonProviders.MANAGEMENT_WITH_INTERFACES_PROVIDER);

        management.registerSubModel(SecurityRealmResourceDefinition.INSTANCE);
        management.registerSubModel(LdapConnectionResourceDefinition.INSTANCE);
        management.registerSubModel(NativeManagementResourceDefinition.INSTANCE);
        management.registerSubModel(NativeRemotingManagementResourceDefinition.INSTANCE);
        management.registerSubModel(HttpManagementResourceDefinition.INSTANCE);

        // Other core services
        ManagementResourceRegistration serviceContainer = root.registerSubModel(PathElement.pathElement(CORE_SERVICE, SERVICE_CONTAINER), CommonProviders.SERVICE_CONTAINER_PROVIDER);
        serviceContainer.registerOperationHandler(DumpServicesHandler.OPERATION_NAME, DumpServicesHandler.INSTANCE, DumpServicesHandler.INSTANCE, false, runtimeOnlyFlag);

        // Platform MBeans
        PlatformMBeanResourceRegistrar.registerPlatformMBeanResources(root);

        // Paths
        root.registerSubModel(PathResourceDefinition.createSpecified(pathManager));

        // Interfaces
        ManagementResourceRegistration interfaces = root.registerSubModel(PathElement.pathElement(INTERFACE), CommonProviders.SPECIFIED_INTERFACE_PROVIDER_SERVER);
        interfaces.registerOperationHandler(SpecifiedInterfaceAddHandler.OPERATION_NAME, SpecifiedInterfaceAddHandler.INSTANCE, SpecifiedInterfaceAddHandler.INSTANCE, false);
        interfaces.registerOperationHandler(SpecifiedInterfaceRemoveHandler.OPERATION_NAME, SpecifiedInterfaceRemoveHandler.INSTANCE, SpecifiedInterfaceRemoveHandler.INSTANCE, false);
        InterfaceCriteriaWriteHandler.UPDATE_RUNTIME.register(interfaces);
        interfaces.registerReadOnlyAttribute(NetworkInterfaceRuntimeHandler.RESOLVED_ADDRESS, NetworkInterfaceRuntimeHandler.INSTANCE, Storage.RUNTIME);
        interfaces.registerOperationHandler(SpecifiedInterfaceResolveHandler.OPERATION_NAME, SpecifiedInterfaceResolveHandler.INSTANCE, SpecifiedInterfaceResolveHandler.INSTANCE, runtimeOnlyFlag);

        // Sockets
        ManagementResourceRegistration socketGroup = root.registerSubModel(new SocketBindingGroupResourceDefinition(BindingGroupAddHandler.INSTANCE, SocketBindingGroupRemoveHandler.INSTANCE, false));
        socketGroup.registerSubModel(SocketBindingResourceDefinition.INSTANCE);
        // client-socket-binding (for remote destination)
        socketGroup.registerSubModel(RemoteDestinationOutboundSocketBindingResourceDefinition.INSTANCE);
        // client-socket-binding (for local destination)
        socketGroup.registerSubModel(LocalDestinationOutboundSocketBindingResourceDefinition.INSTANCE);

        // Deployments
        ManagementResourceRegistration deployments = root.registerSubModel(PathElement.pathElement(DEPLOYMENT), ServerDescriptionProviders.DEPLOYMENT_PROVIDER);

        DeploymentAddHandler dah = DeploymentAddHandler.create(contentRepository, vaultReader);
        deployments.registerOperationHandler(DeploymentAddHandler.OPERATION_NAME, dah, dah, false);
        DeploymentRemoveHandler dremh = new DeploymentRemoveHandler(contentRepository, vaultReader);
        deployments.registerOperationHandler(DeploymentRemoveHandler.OPERATION_NAME, dremh, dremh, false);
        final DeploymentDeployHandler ddhu = new DeploymentDeployHandler(vaultReader);
        deployments.registerOperationHandler(DeploymentDeployHandler.OPERATION_NAME, ddhu, ddhu, false);
        final DeploymentUndeployHandler duh = new DeploymentUndeployHandler(vaultReader);
        deployments.registerOperationHandler(DeploymentUndeployHandler.OPERATION_NAME, duh, duh, false);
        final DeploymentRedeployHandler drdh = new DeploymentRedeployHandler(vaultReader);
        deployments.registerOperationHandler(DeploymentRedeployHandler.OPERATION_NAME, drdh, drdh, false);
        deployments.registerMetric(DeploymentStatusHandler.ATTRIBUTE_NAME, DeploymentStatusHandler.INSTANCE);
        //These are in the description
        deployments.registerReadOnlyAttribute(CONTENT, null, Storage.CONFIGURATION);
        deployments.registerReadOnlyAttribute(NAME, null, Storage.CONFIGURATION);
        deployments.registerReadOnlyAttribute(RUNTIME_NAME, null, Storage.CONFIGURATION);
        deployments.registerReadOnlyAttribute(ENABLED, null, Storage.CONFIGURATION);
        deployments.registerReadOnlyAttribute(PERSISTENT, null, Storage.CONFIGURATION);

        //deployment overlays
        final ManagementResourceRegistration contentOverrides = root.registerSubModel(DeploymentOverlayDefinition.INSTANCE);
        contentOverrides.registerSubModel(new ContentDefinition(contentRepository, null));

        //deployment overlay links
        contentOverrides.registerSubModel(new DeploymentOverlayDeploymentDefinition(DeploymentOverlayPriority.SERVER));

        // The sub-deployments registry
        deployments.registerSubModel(PathElement.pathElement(SUBDEPLOYMENT), ServerDescriptionProviders.SUBDEPLOYMENT_PROVIDER);

        // Extensions
        root.registerSubModel(new ExtensionResourceDefinition(extensionRegistry, parallelBoot, false));
        if (extensionRegistry != null) {
            //extension registry may be null during testing
            extensionRegistry.setSubsystemParentResourceRegistrations(root, deployments);
            extensionRegistry.setPathManager(pathManager);
        }

        // Util
        root.registerOperationHandler(DeployerChainAddHandler.NAME, DeployerChainAddHandler.INSTANCE, DeployerChainAddHandler.INSTANCE, false, EntryType.PRIVATE);
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
