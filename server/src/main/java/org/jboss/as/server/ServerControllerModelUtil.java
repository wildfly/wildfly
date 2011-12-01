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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NATIVE_REMOTING_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTBOUND_CONNECTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVICE_CONTAINER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.server.controller.descriptions.ServerDescriptionConstants.PROFILE_NAME;

import java.util.EnumSet;

import org.jboss.as.controller.CompositeOperationHandler;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ExtensionContextImpl;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.operations.common.ExtensionAddHandler;
import org.jboss.as.controller.operations.common.ExtensionRemoveHandler;
import org.jboss.as.controller.operations.common.InterfaceCriteriaWriteHandler;
import org.jboss.as.controller.operations.common.InterfaceLegacyCriteriaReadHandler;
import org.jboss.as.controller.operations.common.NamespaceAddHandler;
import org.jboss.as.controller.operations.common.NamespaceRemoveHandler;
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
import org.jboss.as.controller.operations.common.XmlMarshallingHandler;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers.StringLengthValidatingHandler;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.OperationEntry.EntryType;
import org.jboss.as.controller.resource.SocketBindingGroupResourceDefinition;
import org.jboss.as.domain.management.operations.ConnectionAddHandler;
import org.jboss.as.domain.management.operations.SecurityRealmAddHandler;
import org.jboss.as.platform.mbean.PlatformMBeanResourceRegistrar;
import org.jboss.as.server.controller.descriptions.ServerDescriptionConstants;
import org.jboss.as.server.controller.descriptions.ServerDescriptionProviders;
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
import org.jboss.as.server.deployment.repository.api.ContentRepository;
import org.jboss.as.server.mgmt.HttpManagementResourceDefinition;
import org.jboss.as.server.mgmt.NativeManagementResourceDefinition;
import org.jboss.as.server.operations.DumpServicesHandler;
import org.jboss.as.server.operations.LaunchTypeHandler;
import org.jboss.as.server.operations.NativeRemotingManagementAddHandler;
import org.jboss.as.server.operations.ProcessTypeHandler;
import org.jboss.as.server.operations.RootResourceHack;
import org.jboss.as.server.operations.ServerReloadHandler;
import org.jboss.as.server.operations.ServerRestartRequiredHandler;
import org.jboss.as.server.operations.ServerShutdownHandler;
import org.jboss.as.server.operations.ServerStateAttributeHandler;
import org.jboss.as.server.operations.SpecifiedPathAddHandler;
import org.jboss.as.server.operations.SpecifiedPathRemoveHandler;
import org.jboss.as.server.services.net.BindingGroupAddHandler;
import org.jboss.as.server.services.net.LocalDestinationOutboundSocketBindingResourceDefinition;
import org.jboss.as.server.services.net.NetworkInterfaceRuntimeHandler;
import org.jboss.as.server.services.net.RemoteDestinationOutboundSocketBindingResourceDefinition;
import org.jboss.as.server.services.net.SocketBindingResourceDefinition;
import org.jboss.as.server.services.net.SpecifiedInterfaceAddHandler;
import org.jboss.as.server.services.net.SpecifiedInterfaceRemoveHandler;
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
 * @version $Revision: 1.1 $
 */
public class ServerControllerModelUtil {

    public static void updateCoreModel(final ModelNode root) {

        root.get(RELEASE_VERSION).set(Version.AS_VERSION);
        root.get(RELEASE_CODENAME).set(Version.AS_RELEASE_CODENAME);

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

    public static void initOperations(final ManagementResourceRegistration root, final ContentRepository contentRepository,
                                      final ExtensibleConfigurationPersister extensibleConfigurationPersister,
                                      final ServerEnvironment serverEnvironment,
                                      final ControlledProcessState processState,
                                      final AbstractVaultReader vaultReader,
                                      final boolean parallelBoot) {
        // Build up the core model registry
        root.registerReadWriteAttribute(NAME, null, new StringLengthValidatingHandler(1), AttributeAccess.Storage.CONFIGURATION);

        // Global operations
        root.registerOperationHandler(READ_RESOURCE_OPERATION, GlobalOperationHandlers.READ_RESOURCE, CommonProviders.READ_RESOURCE_PROVIDER, true);
        root.registerOperationHandler(READ_ATTRIBUTE_OPERATION, GlobalOperationHandlers.READ_ATTRIBUTE, CommonProviders.READ_ATTRIBUTE_PROVIDER, true);
        root.registerOperationHandler(READ_RESOURCE_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_RESOURCE_DESCRIPTION, CommonProviders.READ_RESOURCE_DESCRIPTION_PROVIDER, true);
        root.registerOperationHandler(READ_CHILDREN_NAMES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_NAMES, CommonProviders.READ_CHILDREN_NAMES_PROVIDER, true);
        root.registerOperationHandler(READ_CHILDREN_TYPES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_TYPES, CommonProviders.READ_CHILDREN_TYPES_PROVIDER, true);
        root.registerOperationHandler(READ_CHILDREN_RESOURCES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_RESOURCES, CommonProviders.READ_CHILDREN_RESOURCES_PROVIDER, true);
        root.registerOperationHandler(READ_OPERATION_NAMES_OPERATION, GlobalOperationHandlers.READ_OPERATION_NAMES, CommonProviders.READ_OPERATION_NAMES_PROVIDER, true);
        root.registerOperationHandler(READ_OPERATION_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_OPERATION_DESCRIPTION, CommonProviders.READ_OPERATION_PROVIDER, true);
        root.registerOperationHandler(WRITE_ATTRIBUTE_OPERATION, GlobalOperationHandlers.WRITE_ATTRIBUTE, CommonProviders.WRITE_ATTRIBUTE_PROVIDER, true);
        root.registerOperationHandler(GlobalOperationHandlers.VALIDATE_ADDRESS_OPERATION_NAME, GlobalOperationHandlers.VALIDATE_ADDRESS, CommonProviders.VALIDATE_ADDRESS_PROVIDER, true);

        // Other root resource operations
        root.registerOperationHandler(CompositeOperationHandler.NAME, CompositeOperationHandler.INSTANCE, CompositeOperationHandler.INSTANCE, false, EntryType.PRIVATE);
        XmlMarshallingHandler xmh = new XmlMarshallingHandler(extensibleConfigurationPersister);
        root.registerOperationHandler(XmlMarshallingHandler.OPERATION_NAME, xmh, xmh, false);
        root.registerOperationHandler(NamespaceAddHandler.OPERATION_NAME, NamespaceAddHandler.INSTANCE, NamespaceAddHandler.INSTANCE, false);
        root.registerOperationHandler(NamespaceRemoveHandler.OPERATION_NAME, NamespaceRemoveHandler.INSTANCE, NamespaceRemoveHandler.INSTANCE, false);
        root.registerOperationHandler(SchemaLocationAddHandler.OPERATION_NAME, SchemaLocationAddHandler.INSTANCE, SchemaLocationAddHandler.INSTANCE, false);
        root.registerOperationHandler(SchemaLocationRemoveHandler.OPERATION_NAME, SchemaLocationRemoveHandler.INSTANCE, SchemaLocationRemoveHandler.INSTANCE, false);

        DeploymentUploadBytesHandler dubh = new DeploymentUploadBytesHandler(contentRepository);
        root.registerOperationHandler(DeploymentUploadBytesHandler.OPERATION_NAME, dubh, dubh, false);
        DeploymentUploadURLHandler duuh = new DeploymentUploadURLHandler(contentRepository);
        root.registerOperationHandler(DeploymentUploadURLHandler.OPERATION_NAME, duuh, duuh, false);
        DeploymentUploadStreamAttachmentHandler dush = new DeploymentUploadStreamAttachmentHandler(contentRepository);
        root.registerOperationHandler(DeploymentUploadStreamAttachmentHandler.OPERATION_NAME, dush, dush, false);
        final DeploymentReplaceHandler drh = new DeploymentReplaceHandler(contentRepository);
        root.registerOperationHandler(DeploymentReplaceHandler.OPERATION_NAME, drh, drh, false);
        DeploymentFullReplaceHandler dfrh = new DeploymentFullReplaceHandler(contentRepository);
        root.registerOperationHandler(DeploymentFullReplaceHandler.OPERATION_NAME, dfrh, dfrh, false);

        SnapshotDeleteHandler snapshotDelete = new SnapshotDeleteHandler(extensibleConfigurationPersister);
        root.registerOperationHandler(SnapshotDeleteHandler.OPERATION_NAME, snapshotDelete, snapshotDelete, false);
        SnapshotListHandler snapshotList = new SnapshotListHandler(extensibleConfigurationPersister);
        root.registerOperationHandler(SnapshotListHandler.OPERATION_NAME, snapshotList, snapshotList, false);
        SnapshotTakeHandler snapshotTake = new SnapshotTakeHandler(extensibleConfigurationPersister);
        root.registerOperationHandler(SnapshotTakeHandler.OPERATION_NAME, snapshotTake, snapshotTake, false);
        root.registerOperationHandler(ServerRestartRequiredHandler.OPERATION_NAME, ServerRestartRequiredHandler.INSTANCE, ServerRestartRequiredHandler.INSTANCE, false);

        root.registerReadOnlyAttribute(ServerDescriptionConstants.SERVER_STATE, new ServerStateAttributeHandler(processState), Storage.RUNTIME);
        root.registerReadOnlyAttribute(ServerDescriptionConstants.PROCESS_TYPE, ProcessTypeHandler.INSTANCE, Storage.RUNTIME);
        root.registerOperationHandler(ResolveExpressionHandler.OPERATION_NAME, ResolveExpressionHandler.INSTANCE,
                ResolveExpressionHandler.INSTANCE, EnumSet.of(OperationEntry.Flag.READ_ONLY));

        //Hack to be able to access the registry for the jmx facade
        root.registerOperationHandler(RootResourceHack.NAME, RootResourceHack.INSTANCE, RootResourceHack.INSTANCE, false, OperationEntry.EntryType.PRIVATE);


        // Runtime operations
        if (serverEnvironment != null) {
            // Reload op -- does not work on a domain mode server
            if (serverEnvironment.getLaunchType() != ServerEnvironment.LaunchType.DOMAIN)
                root.registerOperationHandler(ServerReloadHandler.OPERATION_NAME, ServerReloadHandler.INSTANCE, ServerReloadHandler.INSTANCE, false);

            // The System.exit() based shutdown command is only valid for a server process directly launched from the command line
            if (serverEnvironment.getLaunchType() == ServerEnvironment.LaunchType.STANDALONE)
                root.registerOperationHandler(ServerShutdownHandler.OPERATION_NAME, ServerShutdownHandler.INSTANCE, ServerShutdownHandler.INSTANCE, false);

            root.registerReadOnlyAttribute(ServerDescriptionConstants.LAUNCH_TYPE, new LaunchTypeHandler(serverEnvironment.getLaunchType()), Storage.RUNTIME);

            root.registerSubModel(ServerEnvironmentResourceDescription.of(serverEnvironment));
        }
        // System Properties
        ManagementResourceRegistration sysProps = root.registerSubModel(PathElement.pathElement(SYSTEM_PROPERTY), ServerDescriptionProviders.SYSTEM_PROPERTIES_PROVIDER);
        sysProps.registerOperationHandler(SystemPropertyAddHandler.OPERATION_NAME, SystemPropertyAddHandler.INSTANCE_WITHOUT_BOOTTIME, SystemPropertyAddHandler.INSTANCE_WITHOUT_BOOTTIME, false);
        sysProps.registerOperationHandler(SystemPropertyRemoveHandler.OPERATION_NAME, SystemPropertyRemoveHandler.INSTANCE, SystemPropertyRemoveHandler.INSTANCE, false);
        sysProps.registerReadWriteAttribute(VALUE, null, SystemPropertyValueWriteAttributeHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);

        //vault
        ManagementResourceRegistration vault = root.registerSubModel(PathElement.pathElement(CORE_SERVICE, VAULT), CommonProviders.VAULT_PROVIDER);
        VaultAddHandler vah = new VaultAddHandler(vaultReader);
        vault.registerOperationHandler(VaultAddHandler.OPERATION_NAME, vah, vah, false);
        VaultRemoveHandler vrh = new VaultRemoveHandler(vaultReader);
        vault.registerOperationHandler(VaultRemoveHandler.OPERATION_NAME, vrh, vrh, false);
        VaultWriteAttributeHandler.INSTANCE.registerAttributes(vault);

        // Central Management
        ManagementResourceRegistration management = root.registerSubModel(PathElement.pathElement(CORE_SERVICE, MANAGEMENT), CommonProviders.MANAGEMENT_WITH_INTERFACES_PROVIDER);
        ManagementResourceRegistration securityRealm = management.registerSubModel(PathElement.pathElement(SECURITY_REALM), CommonProviders.MANAGEMENT_SECURITY_REALM_PROVIDER);
        securityRealm.registerOperationHandler(SecurityRealmAddHandler.OPERATION_NAME, SecurityRealmAddHandler.INSTANCE, SecurityRealmAddHandler.INSTANCE, false);

        ManagementResourceRegistration connection = management.registerSubModel(PathElement.pathElement(OUTBOUND_CONNECTION), CommonProviders.MANAGEMENT_OUTBOUND_CONNECTION_PROVIDER);
        connection.registerOperationHandler(ConnectionAddHandler.OPERATION_NAME, ConnectionAddHandler.INSTANCE, ConnectionAddHandler.INSTANCE, false);

        // Management Interface protocols
        management.registerSubModel(NativeManagementResourceDefinition.INSTANCE);

        ManagementResourceRegistration managementNativeRemoting = management.registerSubModel(PathElement.pathElement(MANAGEMENT_INTERFACE, NATIVE_REMOTING_INTERFACE), CommonProviders.NATIVE_REMOTING_MANAGEMENT_PROVIDER);
        managementNativeRemoting.registerOperationHandler(NativeRemotingManagementAddHandler.OPERATION_NAME, NativeRemotingManagementAddHandler.INSTANCE, NativeRemotingManagementAddHandler.INSTANCE, false);

        management.registerSubModel(HttpManagementResourceDefinition.INSTANCE);

        // Other core services
        ManagementResourceRegistration serviceContainer = root.registerSubModel(PathElement.pathElement(CORE_SERVICE, SERVICE_CONTAINER), CommonProviders.SERVICE_CONTAINER_PROVIDER);
        serviceContainer.registerOperationHandler(DumpServicesHandler.OPERATION_NAME, DumpServicesHandler.INSTANCE, DumpServicesHandler.INSTANCE, false);

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
        interfaces.registerReadOnlyAttribute(ModelDescriptionConstants.CRITERIA, InterfaceLegacyCriteriaReadHandler.INSTANCE, Storage.CONFIGURATION);

        // Sockets
        ManagementResourceRegistration socketGroup = root.registerSubModel(new SocketBindingGroupResourceDefinition(BindingGroupAddHandler.INSTANCE, SocketBindingGroupRemoveHandler.INSTANCE, false));
        socketGroup.registerSubModel(SocketBindingResourceDefinition.INSTANCE);
        // client-socket-binding (for remote destination)
        socketGroup.registerSubModel(RemoteDestinationOutboundSocketBindingResourceDefinition.INSTANCE);
        // client-socket-binding (for local destination)
        socketGroup.registerSubModel(LocalDestinationOutboundSocketBindingResourceDefinition.INSTANCE);

        // Deployments
        ManagementResourceRegistration deployments = root.registerSubModel(PathElement.pathElement(DEPLOYMENT), ServerDescriptionProviders.DEPLOYMENT_PROVIDER);
        DeploymentAddHandler dah = new DeploymentAddHandler(contentRepository);
        deployments.registerOperationHandler(DeploymentAddHandler.OPERATION_NAME, dah, dah, false);
        deployments.registerOperationHandler(DeploymentRemoveHandler.OPERATION_NAME, DeploymentRemoveHandler.INSTANCE, DeploymentRemoveHandler.INSTANCE, false);
        deployments.registerOperationHandler(DeploymentDeployHandler.OPERATION_NAME, DeploymentDeployHandler.INSTANCE, DeploymentDeployHandler.INSTANCE, false);
        deployments.registerOperationHandler(DeploymentUndeployHandler.OPERATION_NAME, DeploymentUndeployHandler.INSTANCE, DeploymentUndeployHandler.INSTANCE, false);
        deployments.registerOperationHandler(DeploymentRedeployHandler.OPERATION_NAME, DeploymentRedeployHandler.INSTANCE, DeploymentRedeployHandler.INSTANCE, false);
        deployments.registerMetric(DeploymentStatusHandler.ATTRIBUTE_NAME, DeploymentStatusHandler.INSTANCE);

        // The sub-deployments registry
        deployments.registerSubModel(PathElement.pathElement(SUBDEPLOYMENT), ServerDescriptionProviders.SUBDEPLOYMENT_PROVIDER);


        // Extensions
        ManagementResourceRegistration extensions = root.registerSubModel(PathElement.pathElement(EXTENSION), CommonProviders.EXTENSION_PROVIDER);
        ExtensionContext extensionContext = new ExtensionContextImpl(root, deployments, extensibleConfigurationPersister, getProcessType(serverEnvironment));
        ExtensionAddHandler addExtensionHandler = new ExtensionAddHandler(extensionContext, parallelBoot);
        extensions.registerOperationHandler(ExtensionAddHandler.OPERATION_NAME, addExtensionHandler, addExtensionHandler, false);
        extensions.registerOperationHandler(ExtensionRemoveHandler.OPERATION_NAME, ExtensionRemoveHandler.INSTANCE, ExtensionRemoveHandler.INSTANCE, false);

        // Util
        root.registerOperationHandler(DeployerChainAddHandler.NAME, DeployerChainAddHandler.INSTANCE, DeployerChainAddHandler.INSTANCE, false, EntryType.PRIVATE);
    }

    static ExtensionContext.ProcessType getProcessType(ServerEnvironment serverEnvironment) {
        if (serverEnvironment != null) {
            switch (serverEnvironment.getLaunchType()) {
            case DOMAIN:
                return ExtensionContext.ProcessType.DOMAIN_SERVER;
            case STANDALONE:
                return ExtensionContext.ProcessType.STANDALONE_SERVER;
            case EMBEDDED:
                return ExtensionContext.ProcessType.EMBEDDED;
            }
        }

        return ExtensionContext.ProcessType.EMBEDDED;
    }
}
