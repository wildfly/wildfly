/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.host.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTO_START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BOOT_TIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CPU_AFFINITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST_STATE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MASTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRIORITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_TYPES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.util.EnumSet;

import org.jboss.as.controller.CompositeOperationHandler;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.common.InterfaceAddHandler;
import org.jboss.as.controller.operations.common.InterfaceCriteriaWriteHandler;
import org.jboss.as.controller.operations.common.InterfaceRemoveHandler;
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
import org.jboss.as.controller.operations.common.SystemPropertyAddHandler;
import org.jboss.as.controller.operations.common.SystemPropertyRemoveHandler;
import org.jboss.as.controller.operations.common.SystemPropertyValueWriteAttributeHandler;
import org.jboss.as.controller.operations.common.ValidateOperationHandler;
import org.jboss.as.controller.operations.common.XmlMarshallingHandler;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.OperationEntry.EntryType;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.operations.DomainServerLifecycleHandlers;
import org.jboss.as.domain.controller.operations.deployment.HostProcessReloadHandler;
import org.jboss.as.domain.management.connections.ldap.LdapConnectionResourceDefinition;
import org.jboss.as.domain.management.security.SecurityRealmResourceDefinition;
import org.jboss.as.domain.management.security.WhoAmIOperation;
import org.jboss.as.host.controller.descriptions.HostControllerResourceDescription;
import org.jboss.as.host.controller.descriptions.HostDescriptionProviders;
import org.jboss.as.host.controller.descriptions.HostRootDescription;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.host.controller.model.jvm.JvmResourceDefinition;
import org.jboss.as.host.controller.operations.HostModelRegistrationHandler;
import org.jboss.as.host.controller.operations.HostShutdownHandler;
import org.jboss.as.host.controller.operations.HostSpecifiedInterfaceAddHandler;
import org.jboss.as.host.controller.operations.HostSpecifiedInterfaceRemoveHandler;
import org.jboss.as.host.controller.operations.HostXmlMarshallingHandler;
import org.jboss.as.host.controller.operations.IsMasterHandler;
import org.jboss.as.host.controller.operations.LocalDomainControllerAddHandler;
import org.jboss.as.host.controller.operations.LocalDomainControllerRemoveHandler;
import org.jboss.as.host.controller.operations.LocalHostControllerInfoImpl;
import org.jboss.as.host.controller.operations.RemoteDomainControllerAddHandler;
import org.jboss.as.host.controller.operations.RemoteDomainControllerRemoveHandler;
import org.jboss.as.host.controller.operations.ResolveExpressionOnHostHandler;
import org.jboss.as.host.controller.operations.ServerAddHandler;
import org.jboss.as.host.controller.operations.ServerRemoveHandler;
import org.jboss.as.host.controller.operations.ServerRestartHandler;
import org.jboss.as.host.controller.operations.ServerRestartRequiredServerConfigWriteAttributeHandler;
import org.jboss.as.host.controller.operations.ServerStartHandler;
import org.jboss.as.host.controller.operations.ServerStatusHandler;
import org.jboss.as.host.controller.operations.ServerStopHandler;
import org.jboss.as.host.controller.operations.StartServersHandler;
import org.jboss.as.host.controller.resources.HttpManagementResourceDefinition;
import org.jboss.as.host.controller.resources.NativeManagementResourceDefinition;
import org.jboss.as.platform.mbean.PlatformMBeanResourceRegistrar;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.server.operations.RunningModeReadHandler;
import org.jboss.as.server.services.net.SpecifiedInterfaceAddHandler;
import org.jboss.as.server.services.net.SpecifiedInterfaceRemoveHandler;
import org.jboss.as.server.services.net.SpecifiedInterfaceResolveHandler;
import org.jboss.as.server.services.security.AbstractVaultReader;
import org.jboss.as.server.services.security.VaultAddHandler;
import org.jboss.as.server.services.security.VaultRemoveHandler;
import org.jboss.as.server.services.security.VaultWriteAttributeHandler;
import org.jboss.dmr.ModelType;

/**
 * Utility for creating the root element and populating the {@link org.jboss.as.controller.registry.ManagementResourceRegistration}
 * for an individual host's portion of the model.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:jperkins@jboss.com">James R. Perkins</a>
 */
public class HostModelUtil {

    public static interface HostModelRegistrar {
        void registerHostModel(final String hostName, final ManagementResourceRegistration root);
    }

    public static void createRootRegistry(final ManagementResourceRegistration root, final HostControllerEnvironment environment,
                                          final IgnoredDomainResourceRegistry ignoredDomainResourceRegistry,
                                          final HostModelRegistrar hostModelRegistrar) {

        // Add of the host itself
        final HostModelRegistrationHandler hostModelRegistratorHandler = new HostModelRegistrationHandler(environment, ignoredDomainResourceRegistry, hostModelRegistrar);
        root.registerOperationHandler(HostModelRegistrationHandler.OPERATION_NAME, hostModelRegistratorHandler, hostModelRegistratorHandler, false, OperationEntry.EntryType.PRIVATE);

        // Global operations
        EnumSet<OperationEntry.Flag> flags = EnumSet.of(OperationEntry.Flag.READ_ONLY);
        root.registerOperationHandler(READ_RESOURCE_OPERATION, GlobalOperationHandlers.READ_RESOURCE, CommonProviders.READ_RESOURCE_PROVIDER, true, OperationEntry.EntryType.PUBLIC, flags);
        root.registerOperationHandler(READ_ATTRIBUTE_OPERATION, GlobalOperationHandlers.READ_ATTRIBUTE, CommonProviders.READ_ATTRIBUTE_PROVIDER, true, OperationEntry.EntryType.PUBLIC, flags);
        root.registerOperationHandler(READ_RESOURCE_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_RESOURCE_DESCRIPTION, CommonProviders.READ_RESOURCE_DESCRIPTION_PROVIDER, true, OperationEntry.EntryType.PUBLIC, flags);
        root.registerOperationHandler(READ_CHILDREN_NAMES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_NAMES, CommonProviders.READ_CHILDREN_NAMES_PROVIDER, true, OperationEntry.EntryType.PUBLIC, flags);
        root.registerOperationHandler(READ_CHILDREN_TYPES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_TYPES, CommonProviders.READ_CHILDREN_TYPES_PROVIDER, true, OperationEntry.EntryType.PUBLIC, flags);
        root.registerOperationHandler(READ_CHILDREN_RESOURCES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_RESOURCES, CommonProviders.READ_CHILDREN_RESOURCES_PROVIDER, true, OperationEntry.EntryType.PUBLIC, flags);
        root.registerOperationHandler(READ_OPERATION_NAMES_OPERATION, GlobalOperationHandlers.READ_OPERATION_NAMES, CommonProviders.READ_OPERATION_NAMES_PROVIDER, true, OperationEntry.EntryType.PUBLIC, flags);
        root.registerOperationHandler(READ_OPERATION_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_OPERATION_DESCRIPTION, CommonProviders.READ_OPERATION_PROVIDER, true, OperationEntry.EntryType.PUBLIC, flags);
        root.registerOperationHandler(UNDEFINE_ATTRIBUTE_OPERATION, GlobalOperationHandlers.UNDEFINE_ATTRIBUTE, CommonProviders.UNDEFINE_ATTRIBUTE_PROVIDER, true);
        root.registerOperationHandler(WRITE_ATTRIBUTE_OPERATION, GlobalOperationHandlers.WRITE_ATTRIBUTE, CommonProviders.WRITE_ATTRIBUTE_PROVIDER, true);
        root.registerOperationHandler(ValidateOperationHandler.OPERATION_NAME, ValidateOperationHandler.INSTANCE, ValidateOperationHandler.INSTANCE, false, EntryType.PUBLIC, EnumSet.of(Flag.READ_ONLY));
        root.registerOperationHandler(WhoAmIOperation.OPERATION_NAME, WhoAmIOperation.INSTANCE, WhoAmIOperation.INSTANCE, true);

        // Other root resource operations
        root.registerOperationHandler(CompositeOperationHandler.NAME, CompositeOperationHandler.INSTANCE, CompositeOperationHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
    }

    public static void createHostRegistry(final String hostName,
                                          final ManagementResourceRegistration root, final HostControllerConfigurationPersister configurationPersister,
                                          final HostControllerEnvironment environment, final HostRunningModeControl runningModeControl,
                                          final HostFileRepository localFileRepository,
                                          final LocalHostControllerInfoImpl hostControllerInfo, final ServerInventory serverInventory,
                                          final HostFileRepository remoteFileRepository,
                                          final ContentRepository contentRepository,
                                          final DomainController domainController,
                                          final ExtensionRegistry extensionRegistry,
                                          final AbstractVaultReader vaultReader,
                                          final IgnoredDomainResourceRegistry ignoredRegistry,
                                          final ControlledProcessState processState,
                                          final PathManagerService pathManager) {
        // Add of the host itself
        ManagementResourceRegistration hostRegistration = root.registerSubModel(PathElement.pathElement(HOST, hostName), HostDescriptionProviders.HOST_ROOT_PROVIDER);

        // Global operations
        EnumSet<OperationEntry.Flag> flags = EnumSet.of(OperationEntry.Flag.READ_ONLY);

        // Host root resource operations
        XmlMarshallingHandler xmh = new HostXmlMarshallingHandler(configurationPersister.getHostPersister(), hostControllerInfo);
        hostRegistration.registerOperationHandler(XmlMarshallingHandler.OPERATION_NAME, xmh, xmh, false, OperationEntry.EntryType.PUBLIC, flags);

        hostRegistration.registerReadWriteAttribute(HostRootDescription.DIRECTORY_GROUPING, null, new ReloadRequiredWriteAttributeHandler(HostRootDescription.DIRECTORY_GROUPING));

        hostRegistration.registerOperationHandler(NamespaceAddHandler.OPERATION_NAME, NamespaceAddHandler.INSTANCE, NamespaceAddHandler.INSTANCE, false);
        hostRegistration.registerOperationHandler(NamespaceRemoveHandler.OPERATION_NAME, NamespaceRemoveHandler.INSTANCE, NamespaceRemoveHandler.INSTANCE, false);
        hostRegistration.registerOperationHandler(SchemaLocationAddHandler.OPERATION_NAME, SchemaLocationAddHandler.INSTANCE, SchemaLocationAddHandler.INSTANCE, false);
        hostRegistration.registerOperationHandler(SchemaLocationRemoveHandler.OPERATION_NAME, SchemaLocationRemoveHandler.INSTANCE, SchemaLocationRemoveHandler.INSTANCE, false);

        hostRegistration.registerReadWriteAttribute(NAME, environment.getProcessNameReadHandler(), environment.getProcessNameWriteHandler(), Storage.CONFIGURATION);
        hostRegistration.registerReadOnlyAttribute(MASTER, IsMasterHandler.INSTANCE, Storage.RUNTIME);
        hostRegistration.registerReadOnlyAttribute(HOST_STATE, new ProcessStateAttributeHandler(processState), Storage.RUNTIME);

        StartServersHandler ssh = new StartServersHandler(environment, serverInventory, runningModeControl);
        hostRegistration.registerOperationHandler(StartServersHandler.OPERATION_NAME, ssh, ssh, false, OperationEntry.EntryType.PRIVATE);

        HostShutdownHandler hsh = new HostShutdownHandler(domainController);
        hostRegistration.registerOperationHandler(HostShutdownHandler.OPERATION_NAME, hsh, hsh, EnumSet.of(Flag.HOST_CONTROLLER_ONLY));

        hostRegistration.registerOperationHandler(ResolveExpressionHandler.OPERATION_NAME, ResolveExpressionHandler.INSTANCE,
                ResolveExpressionHandler.INSTANCE, EnumSet.of(OperationEntry.Flag.READ_ONLY));
        hostRegistration.registerOperationHandler(ResolveExpressionOnHostHandler.OPERATION_NAME, ResolveExpressionOnHostHandler.INSTANCE,
                ResolveExpressionOnHostHandler.INSTANCE, EnumSet.of(OperationEntry.Flag.READ_ONLY, OperationEntry.Flag.DOMAIN_PUSH_TO_SERVERS));

        HostProcessReloadHandler reloadHandler = new HostProcessReloadHandler(HostControllerService.HC_SERVICE_NAME, runningModeControl, processState, HostRootDescription.getResourceDescriptionResolver("host"));
        hostRegistration.registerOperationHandler(ProcessReloadHandler.OPERATION_NAME, reloadHandler, reloadHandler, EnumSet.of(Flag.HOST_CONTROLLER_ONLY));
        RunningModeReadHandler.createAndRegister(runningModeControl, hostRegistration);

        DomainServerLifecycleHandlers.initializeServerInventory(serverInventory);

        hostRegistration.registerOperationHandler(SpecifiedInterfaceResolveHandler.OPERATION_NAME, SpecifiedInterfaceResolveHandler.INSTANCE, SpecifiedInterfaceResolveHandler.INSTANCE);
        ValidateOperationHandler validateOperationHandler = hostControllerInfo.isMasterDomainController() ? ValidateOperationHandler.INSTANCE : ValidateOperationHandler.SLAVE_HC_INSTANCE;
        hostRegistration.registerOperationHandler(ValidateOperationHandler.OPERATION_NAME, validateOperationHandler, validateOperationHandler, false, EntryType.PRIVATE, EnumSet.of(Flag.READ_ONLY));


        // System Properties
        ManagementResourceRegistration sysProps = hostRegistration.registerSubModel(PathElement.pathElement(SYSTEM_PROPERTY), HostDescriptionProviders.SYSTEM_PROPERTIES_PROVIDER);
        sysProps.registerOperationHandler(SystemPropertyAddHandler.OPERATION_NAME, SystemPropertyAddHandler.INSTANCE_WITH_BOOTTIME, SystemPropertyAddHandler.INSTANCE_WITH_BOOTTIME, false);
        sysProps.registerOperationHandler(SystemPropertyRemoveHandler.OPERATION_NAME, SystemPropertyRemoveHandler.INSTANCE, SystemPropertyRemoveHandler.INSTANCE, false);
        sysProps.registerReadWriteAttribute(VALUE, null, SystemPropertyValueWriteAttributeHandler.INSTANCE, Storage.CONFIGURATION);
        sysProps.registerReadWriteAttribute(BOOT_TIME, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);

        /////////////////////////////////////////
        // Core Services

        //vault
        ManagementResourceRegistration vault = hostRegistration.registerSubModel(PathElement.pathElement(CORE_SERVICE, VAULT), CommonProviders.VAULT_PROVIDER);
        VaultAddHandler vah = new VaultAddHandler(vaultReader);
        vault.registerOperationHandler(VaultAddHandler.OPERATION_NAME, vah, vah, false);
        VaultRemoveHandler vrh = new VaultRemoveHandler(vaultReader);
        vault.registerOperationHandler(VaultRemoveHandler.OPERATION_NAME, vrh, vrh, false);
        VaultWriteAttributeHandler.INSTANCE.registerAttributes(vault);

        // Central Management
        ManagementResourceRegistration management = hostRegistration.registerSubModel(PathElement.pathElement(CORE_SERVICE, MANAGEMENT), CommonProviders.MANAGEMENT_WITH_INTERFACES_PROVIDER);
        management.registerSubModel(SecurityRealmResourceDefinition.INSTANCE);
        management.registerSubModel(LdapConnectionResourceDefinition.INSTANCE);
        management.registerSubModel(new NativeManagementResourceDefinition(hostControllerInfo));
        management.registerSubModel(new HttpManagementResourceDefinition(hostControllerInfo, environment));

        // Other core services
        // TODO get a DumpServicesHandler that works on the domain
//        ManagementResourceRegistration serviceContainer = hostRegistration.registerSubModel(PathElement.pathElement(CORE_SERVICE, SERVICE_CONTAINER), CommonProviders.SERVICE_CONTAINER_PROVIDER);
//        serviceContainer.registerOperationHandler(DumpServicesHandler.OPERATION_NAME, DumpServicesHandler.INSTANCE, DumpServicesHandler.INSTANCE, false);

        // Platform MBeans
        PlatformMBeanResourceRegistrar.registerPlatformMBeanResources(hostRegistration);

        //host-environment
        hostRegistration.registerSubModel(HostControllerResourceDescription.of(environment));



        LocalDomainControllerAddHandler localDcAddHandler = LocalDomainControllerAddHandler.getInstance(root, hostControllerInfo,
                configurationPersister, localFileRepository, contentRepository, domainController, extensionRegistry, pathManager);
        hostRegistration.registerOperationHandler(LocalDomainControllerAddHandler.OPERATION_NAME, localDcAddHandler, localDcAddHandler, false);
        hostRegistration.registerOperationHandler(LocalDomainControllerRemoveHandler.OPERATION_NAME, LocalDomainControllerRemoveHandler.INSTANCE, LocalDomainControllerRemoveHandler.INSTANCE, false);
        RemoteDomainControllerAddHandler remoteDcAddHandler = new RemoteDomainControllerAddHandler(root, hostControllerInfo,
                configurationPersister, contentRepository, remoteFileRepository, extensionRegistry, ignoredRegistry, pathManager);
        hostRegistration.registerOperationHandler(RemoteDomainControllerAddHandler.OPERATION_NAME, remoteDcAddHandler, remoteDcAddHandler, false);
        hostRegistration.registerOperationHandler(RemoteDomainControllerRemoveHandler.OPERATION_NAME, RemoteDomainControllerRemoveHandler.INSTANCE, RemoteDomainControllerRemoveHandler.INSTANCE, false);

        ignoredRegistry.registerResources(hostRegistration);

        SnapshotDeleteHandler snapshotDelete = new SnapshotDeleteHandler(configurationPersister.getHostPersister());
        hostRegistration.registerOperationHandler(SnapshotDeleteHandler.OPERATION_NAME, snapshotDelete, snapshotDelete, false);
        SnapshotListHandler snapshotList = new SnapshotListHandler(configurationPersister.getHostPersister());
        hostRegistration.registerOperationHandler(SnapshotListHandler.OPERATION_NAME, snapshotList, snapshotList, false);
        SnapshotTakeHandler snapshotTake = new SnapshotTakeHandler(configurationPersister.getHostPersister());
        hostRegistration.registerOperationHandler(SnapshotTakeHandler.OPERATION_NAME, snapshotTake, snapshotTake, false);

        // Jvms
        final ManagementResourceRegistration jvms = hostRegistration.registerSubModel(JvmResourceDefinition.GLOBAL);

        //Paths
        hostRegistration.registerSubModel(PathResourceDefinition.createSpecified(pathManager));

        //interface
        ManagementResourceRegistration interfaces = hostRegistration.registerSubModel(PathElement.pathElement(INTERFACE), CommonProviders.SPECIFIED_INTERFACE_PROVIDER);
        HostSpecifiedInterfaceAddHandler hsiah = new HostSpecifiedInterfaceAddHandler();
        interfaces.registerOperationHandler(InterfaceAddHandler.OPERATION_NAME, hsiah, hsiah, false);
        HostSpecifiedInterfaceRemoveHandler sirh = new HostSpecifiedInterfaceRemoveHandler();
        interfaces.registerOperationHandler(InterfaceRemoveHandler.OPERATION_NAME, sirh, sirh, false);
        InterfaceCriteriaWriteHandler.UPDATE_RUNTIME.register(interfaces);
        interfaces.registerOperationHandler(SpecifiedInterfaceResolveHandler.OPERATION_NAME, SpecifiedInterfaceResolveHandler.INSTANCE, SpecifiedInterfaceResolveHandler.INSTANCE);

        //server
        ManagementResourceRegistration servers = hostRegistration.registerSubModel(PathElement.pathElement(SERVER_CONFIG), HostDescriptionProviders.SERVER_PROVIDER);
        servers.registerOperationHandler(ServerAddHandler.OPERATION_NAME, ServerAddHandler.INSTANCE, ServerAddHandler.INSTANCE, false, EnumSet.of(Flag.HOST_CONTROLLER_ONLY));
        servers.registerOperationHandler(ServerRemoveHandler.OPERATION_NAME, ServerRemoveHandler.INSTANCE, ServerRemoveHandler.INSTANCE, false, EnumSet.of(Flag.HOST_CONTROLLER_ONLY));
        servers.registerReadWriteAttribute(AUTO_START, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        servers.registerReadWriteAttribute(SOCKET_BINDING_GROUP, null, ServerRestartRequiredServerConfigWriteAttributeHandler.SOCKET_BINDING_GROUP_INSTANCE, Storage.CONFIGURATION);
        servers.registerReadWriteAttribute(SOCKET_BINDING_PORT_OFFSET, null, ServerRestartRequiredServerConfigWriteAttributeHandler.SOCKET_BINDING_PORT_OFFSET_INSTANCE, Storage.CONFIGURATION);
        servers.registerReadWriteAttribute(PRIORITY, null, new WriteAttributeHandlers.IntRangeValidatingHandler(0), Storage.CONFIGURATION);
        servers.registerReadWriteAttribute(CPU_AFFINITY, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1), Storage.CONFIGURATION);
        servers.registerReadWriteAttribute(GROUP, null, ServerRestartRequiredServerConfigWriteAttributeHandler.GROUP_INSTANCE, Storage.CONFIGURATION);

        // Register server runtime operation handlers
        servers.registerMetric(ServerStatusHandler.ATTRIBUTE_NAME, new ServerStatusHandler(serverInventory));
        ServerStartHandler startHandler = new ServerStartHandler(serverInventory);
        servers.registerOperationHandler(ServerStartHandler.OPERATION_NAME, startHandler, startHandler, EnumSet.of(OperationEntry.Flag.HOST_CONTROLLER_ONLY));
        ServerRestartHandler restartHandler = new ServerRestartHandler(serverInventory);
        servers.registerOperationHandler(ServerRestartHandler.OPERATION_NAME, restartHandler, restartHandler,  EnumSet.of(OperationEntry.Flag.HOST_CONTROLLER_ONLY));
        ServerStopHandler stopHandler = new ServerStopHandler(serverInventory);
        servers.registerOperationHandler(ServerStopHandler.OPERATION_NAME, stopHandler, stopHandler, EnumSet.of(OperationEntry.Flag.HOST_CONTROLLER_ONLY));

        //server paths
        servers.registerSubModel(PathResourceDefinition.createSpecifiedNoServices(pathManager));

        //server interfaces
        ManagementResourceRegistration serverInterfaces = servers.registerSubModel(PathElement.pathElement(INTERFACE), CommonProviders.SPECIFIED_INTERFACE_PROVIDER);
        serverInterfaces.registerOperationHandler(InterfaceAddHandler.OPERATION_NAME, SpecifiedInterfaceAddHandler.INSTANCE, SpecifiedInterfaceAddHandler.INSTANCE, false);
        serverInterfaces.registerOperationHandler(InterfaceRemoveHandler.OPERATION_NAME, SpecifiedInterfaceRemoveHandler.INSTANCE, SpecifiedInterfaceRemoveHandler.INSTANCE, false);
        InterfaceCriteriaWriteHandler.CONFIG_ONLY.register(serverInterfaces);

        // Server system Properties
        ManagementResourceRegistration serverSysProps = servers.registerSubModel(PathElement.pathElement(SYSTEM_PROPERTY), HostDescriptionProviders.SERVER_SYSTEM_PROPERTIES_PROVIDER);
        serverSysProps.registerOperationHandler(SystemPropertyAddHandler.OPERATION_NAME, SystemPropertyAddHandler.INSTANCE_WITH_BOOTTIME, SystemPropertyAddHandler.INSTANCE_WITH_BOOTTIME, false);
        serverSysProps.registerOperationHandler(SystemPropertyRemoveHandler.OPERATION_NAME, SystemPropertyRemoveHandler.INSTANCE, SystemPropertyRemoveHandler.INSTANCE, false);
        serverSysProps.registerReadWriteAttribute(VALUE, null, SystemPropertyValueWriteAttributeHandler.INSTANCE, Storage.CONFIGURATION);
        serverSysProps.registerReadWriteAttribute(BOOT_TIME, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);

        // Server jvm
        final ManagementResourceRegistration serverVMs = servers.registerSubModel(JvmResourceDefinition.SERVER);
    }
}
