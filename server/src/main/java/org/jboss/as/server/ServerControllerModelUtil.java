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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_TYPES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELEASE_CODENAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELEASE_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVICE_CONTAINER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.server.controller.descriptions.ServerDescriptionConstants.PROFILE_NAME;

import java.util.EnumSet;

import org.jboss.as.controller.CompositeOperationHandler;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.ExtensionResourceDefinition;
import org.jboss.as.controller.operations.common.InterfaceCriteriaWriteHandler;
import org.jboss.as.controller.operations.common.NamespaceAddHandler;
import org.jboss.as.controller.operations.common.NamespaceRemoveHandler;
import org.jboss.as.controller.operations.common.ProcessReloadHandler;
import org.jboss.as.controller.operations.common.ProcessStateAttributeHandler;
import org.jboss.as.controller.operations.common.ResolveExpressionHandler;
import org.jboss.as.controller.operations.common.SchemaLocationAddHandler;
import org.jboss.as.controller.operations.common.SchemaLocationRemoveHandler;
import org.jboss.as.controller.operations.common.SnapshotDeleteHandler;
import org.jboss.as.controller.operations.common.SnapshotListHandler;
import org.jboss.as.controller.operations.common.SnapshotTakeHandler;
import org.jboss.as.controller.operations.common.SocketBindingGroupRemoveHandler;
import org.jboss.as.controller.operations.common.SystemPropertyAddHandler;
import org.jboss.as.controller.operations.common.SystemPropertyRemoveHandler;
import org.jboss.as.controller.operations.common.SystemPropertyValueWriteAttributeHandler;
import org.jboss.as.controller.operations.common.ValidateAddressOperationHandler;
import org.jboss.as.controller.operations.common.ValidateOperationHandler;
import org.jboss.as.controller.operations.common.XmlMarshallingHandler;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers.StringLengthValidatingHandler;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.OperationEntry.EntryType;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.as.controller.resource.SocketBindingGroupResourceDefinition;
import org.jboss.as.domain.management.connections.ldap.LdapConnectionResourceDefinition;
import org.jboss.as.domain.management.security.SecurityRealmResourceDefinition;
import org.jboss.as.domain.management.security.WhoAmIOperation;
import org.jboss.as.platform.mbean.PlatformMBeanResourceRegistrar;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.DeploymentFileRepository;
import org.jboss.as.server.ServerEnvironment.LaunchType;
import org.jboss.as.server.controller.descriptions.ServerDescriptionConstants;
import org.jboss.as.server.controller.descriptions.ServerDescriptionProviders;
import org.jboss.as.server.controller.descriptions.ServerDescriptions;
import org.jboss.as.server.deployment.DeploymentAddHandler;
import org.jboss.as.server.deployment.DeploymentDeployHandler;
import org.jboss.as.server.deployment.DeploymentFullReplaceHandler;
import org.jboss.as.server.deployment.DeploymentRedeployHandler;
import org.jboss.as.server.deployment.DeploymentRemoveHandler;
import org.jboss.as.server.deployment.DeploymentReplaceHandler;
import org.jboss.as.server.deployment.DeploymentStatusHandler;
import org.jboss.as.server.deployment.DeploymentUndeployHandler;
import org.jboss.as.server.deployment.DeploymentUploadBytesHandler;
import org.jboss.as.server.deployment.DeploymentUploadStreamAttachmentHandler;
import org.jboss.as.server.deployment.DeploymentUploadURLHandler;
import org.jboss.as.server.mgmt.HttpManagementResourceDefinition;
import org.jboss.as.server.mgmt.NativeManagementResourceDefinition;
import org.jboss.as.server.mgmt.NativeRemotingManagementResourceDefinition;
import org.jboss.as.server.operations.DumpServicesHandler;
import org.jboss.as.server.operations.LaunchTypeHandler;
import org.jboss.as.server.operations.ProcessTypeHandler;
import org.jboss.as.server.operations.RootResourceHack;
import org.jboss.as.server.operations.RunningModeReadHandler;
import org.jboss.as.server.operations.ServerRestartRequiredHandler;
import org.jboss.as.server.operations.ServerShutdownHandler;
import org.jboss.as.server.operations.SpecifiedPathAddHandler;
import org.jboss.as.server.operations.SpecifiedPathRemoveHandler;
import org.jboss.as.server.services.net.BindingGroupAddHandler;
import org.jboss.as.server.services.net.LocalDestinationOutboundSocketBindingResourceDefinition;
import org.jboss.as.server.services.net.NetworkInterfaceRuntimeHandler;
import org.jboss.as.server.services.net.RemoteDestinationOutboundSocketBindingResourceDefinition;
import org.jboss.as.server.services.net.SocketBindingResourceDefinition;
import org.jboss.as.server.services.net.SpecifiedInterfaceAddHandler;
import org.jboss.as.server.services.net.SpecifiedInterfaceRemoveHandler;
import org.jboss.as.server.services.net.SpecifiedInterfaceResolveHandler;
import org.jboss.as.server.services.security.AbstractVaultReader;
import org.jboss.as.server.services.security.VaultAddHandler;
import org.jboss.as.server.services.security.VaultRemoveHandler;
import org.jboss.as.server.services.security.VaultWriteAttributeHandler;
import org.jboss.as.version.Version;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author David Bosschaert
 */
public class ServerControllerModelUtil {

    public static void updateCoreModel(final ModelNode root, ServerEnvironment environment) {

        root.get(RELEASE_VERSION).set(Version.AS_VERSION);
        root.get(RELEASE_CODENAME).set(Version.AS_RELEASE_CODENAME);
        root.get(MANAGEMENT_MAJOR_VERSION).set(Version.MANAGEMENT_MAJOR_VERSION);
        root.get(MANAGEMENT_MINOR_VERSION).set(Version.MANAGEMENT_MINOR_VERSION);

         // Community uses UNDEF values
        ModelNode nameNode = root.get(PRODUCT_NAME);
        ModelNode versionNode = root.get(PRODUCT_VERSION);

        if (environment != null) {
            String productName = environment.getProductConfig().getProductName();
            String productVersion = environment.getProductConfig().getProductVersion();

            if (productName != null) {
                nameNode.set(productName);
            }
            if (productVersion != null) {
                versionNode.set(productVersion);
            }
        }

        root.get(NAMESPACES).setEmptyList();
        root.get(SCHEMA_LOCATIONS).setEmptyList();
        root.get(NAME);
        root.get(CORE_SERVICE);
        root.get(PROFILE_NAME);
        root.get(EXTENSION);
        root.get(SYSTEM_PROPERTY);
        root.get(PATH);
        root.get(SUBSYSTEM);
        root.get(INTERFACE);
        root.get(SOCKET_BINDING_GROUP);
        root.get(DEPLOYMENT);
    }

    public static void initOperations(final ManagementResourceRegistration root,
                                      final ContentRepository contentRepository,
                                      final ExtensibleConfigurationPersister extensibleConfigurationPersister,
                                      final ServerEnvironment serverEnvironment,
                                      final ControlledProcessState processState,
                                      final RunningModeControl runningModeControl,
                                      final AbstractVaultReader vaultReader,
                                      final ExtensionRegistry extensionRegistry,
                                      final boolean parallelBoot,
                                      final DeploymentFileRepository remoteFileRepository) {

        boolean isDomain = serverEnvironment == null || serverEnvironment.getLaunchType() == LaunchType.DOMAIN;

        // Root resource attributes
        if (serverEnvironment != null) { // TODO eliminate test cases that result in serverEnviroment == null
            if (isDomain) {
                root.registerReadOnlyAttribute(NAME, serverEnvironment.getProcessNameReadHandler(), AttributeAccess.Storage.CONFIGURATION);
                root.registerReadWriteAttribute(PROFILE_NAME, null, new StringLengthValidatingHandler(1), AttributeAccess.Storage.CONFIGURATION);
            } else {
                root.registerReadWriteAttribute(NAME, serverEnvironment.getProcessNameReadHandler(), serverEnvironment.getProcessNameWriteHandler(), AttributeAccess.Storage.CONFIGURATION);
            }
        }

        EnumSet<Flag> runtimeOnlyFlag = EnumSet.of(Flag.RUNTIME_ONLY);

        // Global operations
        root.registerOperationHandler(READ_RESOURCE_OPERATION, GlobalOperationHandlers.READ_RESOURCE, CommonProviders.READ_RESOURCE_PROVIDER, true, runtimeOnlyFlag);
        root.registerOperationHandler(READ_ATTRIBUTE_OPERATION, GlobalOperationHandlers.READ_ATTRIBUTE, CommonProviders.READ_ATTRIBUTE_PROVIDER, true, runtimeOnlyFlag);
        root.registerOperationHandler(READ_RESOURCE_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_RESOURCE_DESCRIPTION, CommonProviders.READ_RESOURCE_DESCRIPTION_PROVIDER, true, runtimeOnlyFlag);
        root.registerOperationHandler(READ_CHILDREN_NAMES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_NAMES, CommonProviders.READ_CHILDREN_NAMES_PROVIDER, true, runtimeOnlyFlag);
        root.registerOperationHandler(READ_CHILDREN_TYPES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_TYPES, CommonProviders.READ_CHILDREN_TYPES_PROVIDER, true, runtimeOnlyFlag);
        root.registerOperationHandler(READ_CHILDREN_RESOURCES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_RESOURCES, CommonProviders.READ_CHILDREN_RESOURCES_PROVIDER, true, runtimeOnlyFlag);
        root.registerOperationHandler(READ_OPERATION_NAMES_OPERATION, GlobalOperationHandlers.READ_OPERATION_NAMES, CommonProviders.READ_OPERATION_NAMES_PROVIDER, true, runtimeOnlyFlag);
        root.registerOperationHandler(READ_OPERATION_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_OPERATION_DESCRIPTION, CommonProviders.READ_OPERATION_PROVIDER, true, runtimeOnlyFlag);
        root.registerOperationHandler(UNDEFINE_ATTRIBUTE_OPERATION, GlobalOperationHandlers.UNDEFINE_ATTRIBUTE, CommonProviders.UNDEFINE_ATTRIBUTE_PROVIDER, true);
        root.registerOperationHandler(WRITE_ATTRIBUTE_OPERATION, GlobalOperationHandlers.WRITE_ATTRIBUTE, CommonProviders.WRITE_ATTRIBUTE_PROVIDER, true);

        if (serverEnvironment != null) {
            root.registerOperationHandler(ValidateOperationHandler.OPERATION_NAME, ValidateOperationHandler.INSTANCE, ValidateOperationHandler.INSTANCE);
        } else {
            root.registerOperationHandler(ValidateOperationHandler.OPERATION_NAME, ValidateOperationHandler.INSTANCE, ValidateOperationHandler.INSTANCE, false, runtimeOnlyFlag);
        }

        // Other root resource operations
        root.registerOperationHandler(CompositeOperationHandler.NAME, CompositeOperationHandler.INSTANCE, CompositeOperationHandler.INSTANCE, false, EntryType.PRIVATE);
        XmlMarshallingHandler xmh = new XmlMarshallingHandler(extensibleConfigurationPersister);
        root.registerOperationHandler(XmlMarshallingHandler.OPERATION_NAME, xmh, xmh, false, runtimeOnlyFlag);
        root.registerOperationHandler(NamespaceAddHandler.OPERATION_NAME, NamespaceAddHandler.INSTANCE, NamespaceAddHandler.INSTANCE, false);
        root.registerOperationHandler(NamespaceRemoveHandler.OPERATION_NAME, NamespaceRemoveHandler.INSTANCE, NamespaceRemoveHandler.INSTANCE, false);
        root.registerOperationHandler(SchemaLocationAddHandler.OPERATION_NAME, SchemaLocationAddHandler.INSTANCE, SchemaLocationAddHandler.INSTANCE, false);
        root.registerOperationHandler(SchemaLocationRemoveHandler.OPERATION_NAME, SchemaLocationRemoveHandler.INSTANCE, SchemaLocationRemoveHandler.INSTANCE, false);
        root.registerOperationHandler(ValidateAddressOperationHandler.OPERATION_NAME, ValidateAddressOperationHandler.INSTANCE, ValidateAddressOperationHandler.INSTANCE, false);

        DeploymentUploadBytesHandler dubh = new DeploymentUploadBytesHandler(contentRepository);
        root.registerOperationHandler(DeploymentUploadBytesHandler.OPERATION_NAME, dubh, dubh, false);
        DeploymentUploadURLHandler duuh = new DeploymentUploadURLHandler(contentRepository);
        root.registerOperationHandler(DeploymentUploadURLHandler.OPERATION_NAME, duuh, duuh, false);
        DeploymentUploadStreamAttachmentHandler dush = new DeploymentUploadStreamAttachmentHandler(contentRepository);
        root.registerOperationHandler(DeploymentUploadStreamAttachmentHandler.OPERATION_NAME, dush, dush, false);
        final DeploymentReplaceHandler drh = isDomain ? DeploymentReplaceHandler.createForDomainServer(contentRepository, remoteFileRepository)
                                                      : DeploymentReplaceHandler.createForStandalone(contentRepository);
        root.registerOperationHandler(DeploymentReplaceHandler.OPERATION_NAME, drh, drh, false);
        DeploymentFullReplaceHandler dfrh = isDomain ? DeploymentFullReplaceHandler.createForDomainServer(contentRepository, remoteFileRepository)
                                                     : DeploymentFullReplaceHandler.createForStandalone(contentRepository);
        root.registerOperationHandler(DeploymentFullReplaceHandler.OPERATION_NAME, dfrh, dfrh, false);

        if (!isDomain) {
            SnapshotDeleteHandler snapshotDelete = new SnapshotDeleteHandler(extensibleConfigurationPersister);
            root.registerOperationHandler(SnapshotDeleteHandler.OPERATION_NAME, snapshotDelete, snapshotDelete, false);
            SnapshotListHandler snapshotList = new SnapshotListHandler(extensibleConfigurationPersister);
            root.registerOperationHandler(SnapshotListHandler.OPERATION_NAME, snapshotList, snapshotList, false);
            SnapshotTakeHandler snapshotTake = new SnapshotTakeHandler(extensibleConfigurationPersister);
            root.registerOperationHandler(SnapshotTakeHandler.OPERATION_NAME, snapshotTake, snapshotTake, false);
            root.registerOperationHandler(ServerRestartRequiredHandler.OPERATION_NAME, ServerRestartRequiredHandler.INSTANCE, ServerRestartRequiredHandler.INSTANCE, false);
        }
        root.registerReadOnlyAttribute(ServerDescriptionConstants.PROCESS_STATE, new ProcessStateAttributeHandler(processState), Storage.RUNTIME);
        root.registerReadOnlyAttribute(ServerDescriptionConstants.PROCESS_TYPE, ProcessTypeHandler.INSTANCE, Storage.RUNTIME);
        RunningModeReadHandler.createAndRegister(runningModeControl, root);
        root.registerOperationHandler(ResolveExpressionHandler.OPERATION_NAME, ResolveExpressionHandler.INSTANCE,
                ResolveExpressionHandler.INSTANCE, EnumSet.of(OperationEntry.Flag.READ_ONLY, OperationEntry.Flag.RUNTIME_ONLY));

        root.registerOperationHandler(SpecifiedInterfaceResolveHandler.OPERATION_NAME, SpecifiedInterfaceResolveHandler.INSTANCE, SpecifiedInterfaceResolveHandler.INSTANCE, runtimeOnlyFlag);
        root.registerOperationHandler(WhoAmIOperation.OPERATION_NAME, WhoAmIOperation.INSTANCE, WhoAmIOperation.INSTANCE, true);

        //Hack to be able to access the registry for the jmx facade
        root.registerOperationHandler(RootResourceHack.NAME, RootResourceHack.INSTANCE, RootResourceHack.INSTANCE, false, OperationEntry.EntryType.PRIVATE, runtimeOnlyFlag);


        // Runtime operations
        if (serverEnvironment != null) {
            // Reload op -- does not work on a domain mode server
            if (serverEnvironment.getLaunchType() != ServerEnvironment.LaunchType.DOMAIN)  {
                ProcessReloadHandler reloadHandler = new ProcessReloadHandler<RunningModeControl>(Services.JBOSS_AS, runningModeControl, ServerDescriptions.getResourceDescriptionResolver("server"));
                root.registerOperationHandler(ProcessReloadHandler.OPERATION_NAME, reloadHandler, reloadHandler);
            }

            // The System.exit() based shutdown command is only valid for a server process directly launched from the command line
            if (serverEnvironment.getLaunchType() == ServerEnvironment.LaunchType.STANDALONE) {
                root.registerOperationHandler(ServerShutdownHandler.OPERATION_NAME, ServerShutdownHandler.INSTANCE, ServerShutdownHandler.INSTANCE, false);
            }
            root.registerReadOnlyAttribute(ServerDescriptionConstants.LAUNCH_TYPE, new LaunchTypeHandler(serverEnvironment.getLaunchType()), Storage.RUNTIME);

            root.registerSubModel(ServerEnvironmentResourceDescription.of(serverEnvironment));
        }
        // System Properties
        ManagementResourceRegistration sysProps = root.registerSubModel(PathElement.pathElement(SYSTEM_PROPERTY), ServerDescriptionProviders.SYSTEM_PROPERTIES_PROVIDER);
        SystemPropertyAddHandler spah = new SystemPropertyAddHandler(serverEnvironment, false);
        sysProps.registerOperationHandler(SystemPropertyAddHandler.OPERATION_NAME, spah, spah, false);
        SystemPropertyRemoveHandler sprh = new SystemPropertyRemoveHandler(serverEnvironment);
        sysProps.registerOperationHandler(SystemPropertyRemoveHandler.OPERATION_NAME, sprh, sprh, false);
        sysProps.registerReadWriteAttribute(VALUE, null, SystemPropertyValueWriteAttributeHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);

        //vault
        ManagementResourceRegistration vault = root.registerSubModel(PathElement.pathElement(CORE_SERVICE, VAULT), CommonProviders.VAULT_PROVIDER);
        VaultAddHandler vah = new VaultAddHandler(vaultReader);
        vault.registerOperationHandler(VaultAddHandler.OPERATION_NAME, vah, vah, false);
        VaultRemoveHandler vrh = new VaultRemoveHandler(vaultReader);
        vault.registerOperationHandler(VaultRemoveHandler.OPERATION_NAME, vrh, vrh, false);
        VaultWriteAttributeHandler.INSTANCE.registerAttributes(vault);

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
        ManagementResourceRegistration paths = root.registerSubModel(PathElement.pathElement(PATH), CommonProviders.SPECIFIED_PATH_PROVIDER);
        paths.registerOperationHandler(SpecifiedPathAddHandler.OPERATION_NAME, SpecifiedPathAddHandler.INSTANCE, SpecifiedPathAddHandler.INSTANCE, false);
        paths.registerOperationHandler(SpecifiedPathRemoveHandler.OPERATION_NAME, SpecifiedPathRemoveHandler.INSTANCE, SpecifiedPathRemoveHandler.INSTANCE, false);

        // Interfaces
        ManagementResourceRegistration interfaces = root.registerSubModel(PathElement.pathElement(INTERFACE), CommonProviders.SPECIFIED_INTERFACE_PROVIDER);
        interfaces.registerOperationHandler(SpecifiedInterfaceAddHandler.OPERATION_NAME, SpecifiedInterfaceAddHandler.INSTANCE, SpecifiedInterfaceAddHandler.INSTANCE, false);
        interfaces.registerOperationHandler(SpecifiedInterfaceRemoveHandler.OPERATION_NAME, SpecifiedInterfaceRemoveHandler.INSTANCE, SpecifiedInterfaceRemoveHandler.INSTANCE, false);
        InterfaceCriteriaWriteHandler.register(interfaces);
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
        DeploymentAddHandler dah = isDomain ? DeploymentAddHandler.createForDomainServer(contentRepository, remoteFileRepository)
                                            : DeploymentAddHandler.createForStandalone(contentRepository);
        deployments.registerOperationHandler(DeploymentAddHandler.OPERATION_NAME, dah, dah, false);
        DeploymentRemoveHandler dremh = new DeploymentRemoveHandler(contentRepository);
        deployments.registerOperationHandler(DeploymentRemoveHandler.OPERATION_NAME, dremh, dremh, false);
        deployments.registerOperationHandler(DeploymentDeployHandler.OPERATION_NAME, DeploymentDeployHandler.INSTANCE, DeploymentDeployHandler.INSTANCE, false);
        deployments.registerOperationHandler(DeploymentUndeployHandler.OPERATION_NAME, DeploymentUndeployHandler.INSTANCE, DeploymentUndeployHandler.INSTANCE, false);
        deployments.registerOperationHandler(DeploymentRedeployHandler.OPERATION_NAME, DeploymentRedeployHandler.INSTANCE, DeploymentRedeployHandler.INSTANCE, false);
        deployments.registerMetric(DeploymentStatusHandler.ATTRIBUTE_NAME, DeploymentStatusHandler.INSTANCE);

        // The sub-deployments registry
        deployments.registerSubModel(PathElement.pathElement(SUBDEPLOYMENT), ServerDescriptionProviders.SUBDEPLOYMENT_PROVIDER);

        // Extensions
        root.registerSubModel(new ExtensionResourceDefinition(extensionRegistry, parallelBoot));
        extensionRegistry.setSubsystemParentResourceRegistrations(root, deployments);

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
