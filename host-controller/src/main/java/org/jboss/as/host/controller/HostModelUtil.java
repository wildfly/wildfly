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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_CONTROLLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HTTP_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MASTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NATIVE_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTBOUND_CONNECTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRIORITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_TYPES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNNING_SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.util.EnumSet;

import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ExtensionContextImpl;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.operations.common.ExtensionRemoveHandler;
import org.jboss.as.controller.operations.common.InterfaceAddHandler;
import org.jboss.as.controller.operations.common.InterfaceRemoveHandler;
import org.jboss.as.controller.operations.common.JVMHandlers;
import org.jboss.as.controller.operations.common.NamespaceAddHandler;
import org.jboss.as.controller.operations.common.NamespaceRemoveHandler;
import org.jboss.as.controller.operations.common.PathAddHandler;
import org.jboss.as.controller.operations.common.PathRemoveHandler;
import org.jboss.as.controller.operations.common.SchemaLocationAddHandler;
import org.jboss.as.controller.operations.common.SchemaLocationRemoveHandler;
import org.jboss.as.controller.operations.common.SnapshotDeleteHandler;
import org.jboss.as.controller.operations.common.SnapshotListHandler;
import org.jboss.as.controller.operations.common.SnapshotTakeHandler;
import org.jboss.as.controller.operations.common.SystemPropertyAddHandler;
import org.jboss.as.controller.operations.common.SystemPropertyRemoveHandler;
import org.jboss.as.controller.operations.common.SystemPropertyValueWriteAttributeHandler;
import org.jboss.as.controller.operations.common.XmlMarshallingHandler;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.domain.controller.UnregisteredHostChannelRegistry;
import org.jboss.as.domain.management.operations.ConnectionAddHandler;
import org.jboss.as.domain.management.operations.SecurityRealmAddHandler;
import org.jboss.as.host.controller.RemoteDomainConnectionService.RemoteFileRepository;
import org.jboss.as.host.controller.descriptions.HostDescriptionProviders;
import org.jboss.as.host.controller.operations.HostShutdownHandler;
import org.jboss.as.host.controller.operations.HostSpecifiedInterfaceAddHandler;
import org.jboss.as.host.controller.operations.HostSpecifiedInterfaceRemoveHandler;
import org.jboss.as.host.controller.operations.HostXmlMarshallingHandler;
import org.jboss.as.host.controller.operations.HttpManagementAddHandler;
import org.jboss.as.host.controller.operations.HttpManagementAttributeHandlers;
import org.jboss.as.host.controller.operations.IsMasterHandler;
import org.jboss.as.host.controller.operations.LocalDomainControllerAddHandler;
import org.jboss.as.host.controller.operations.LocalDomainControllerRemoveHandler;
import org.jboss.as.host.controller.operations.LocalHostAddHandler;
import org.jboss.as.host.controller.operations.LocalHostControllerInfoImpl;
import org.jboss.as.host.controller.operations.NativeManagementAddHandler;
import org.jboss.as.host.controller.operations.NativeManagementAttributeHandlers;
import org.jboss.as.host.controller.operations.RemoteDomainControllerAddHandler;
import org.jboss.as.host.controller.operations.RemoteDomainControllerRemoveHandler;
import org.jboss.as.host.controller.operations.ServerAddHandler;
import org.jboss.as.host.controller.operations.ServerRemoveHandler;
import org.jboss.as.host.controller.operations.ServerRestartHandler;
import org.jboss.as.host.controller.operations.ServerStartHandler;
import org.jboss.as.host.controller.operations.ServerStatusHandler;
import org.jboss.as.host.controller.operations.ServerStopHandler;
import org.jboss.as.host.controller.operations.StartServersHandler;
import org.jboss.as.server.operations.ExtensionAddHandler;
import org.jboss.as.server.services.net.SpecifiedInterfaceAddHandler;
import org.jboss.as.server.services.net.SpecifiedInterfaceRemoveHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Utility for creating the root element and populating the {@link org.jboss.as.controller.registry.ManagementResourceRegistration}
 * for an individual host's portion of the model.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class HostModelUtil {

    public static void initCoreModel(final ModelNode root) {
        root.get(NAME);
        root.get(NAMESPACES).setEmptyList();
        root.get(SCHEMA_LOCATIONS).setEmptyList();
        root.get(EXTENSION);
        root.get(SYSTEM_PROPERTY);
        root.get(PATH);
        root.get(CORE_SERVICE);
        root.get(SERVER_CONFIG);
        root.get(DOMAIN_CONTROLLER);
        root.get(INTERFACE);
        root.get(JVM);
        root.get(RUNNING_SERVER);
    }

    public static void createHostRegistry(final ManagementResourceRegistration root, final HostControllerConfigurationPersister configurationPersister,
                                          final HostControllerEnvironment environment, final FileRepository localFileRepository,
                                          final LocalHostControllerInfoImpl hostControllerInfo, final ServerInventory serverInventory,
                                          final RemoteFileRepository remoteFileRepository, final DomainController domainController,
                                          final UnregisteredHostChannelRegistry registry) {
        // Add of the host itself
        ManagementResourceRegistration hostRegistration = root.registerSubModel(PathElement.pathElement(HOST), HostDescriptionProviders.HOST_ROOT_PROVIDER);
        LocalHostAddHandler handler = LocalHostAddHandler.getInstance(hostControllerInfo);
        hostRegistration.registerOperationHandler(LocalHostAddHandler.OPERATION_NAME, handler, handler, false, OperationEntry.EntryType.PRIVATE);

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
        root.registerOperationHandler(WRITE_ATTRIBUTE_OPERATION, GlobalOperationHandlers.WRITE_ATTRIBUTE, CommonProviders.WRITE_ATTRIBUTE_PROVIDER, true);

        // Other root resource operations
        XmlMarshallingHandler xmh = new HostXmlMarshallingHandler(configurationPersister.getHostPersister(), hostControllerInfo);
        hostRegistration.registerOperationHandler(XmlMarshallingHandler.OPERATION_NAME, xmh, xmh, false, OperationEntry.EntryType.PUBLIC, flags);
        hostRegistration.registerOperationHandler(NamespaceAddHandler.OPERATION_NAME, NamespaceAddHandler.INSTANCE, NamespaceAddHandler.INSTANCE, false);
        hostRegistration.registerOperationHandler(NamespaceRemoveHandler.OPERATION_NAME, NamespaceRemoveHandler.INSTANCE, NamespaceRemoveHandler.INSTANCE, false);
        hostRegistration.registerOperationHandler(SchemaLocationAddHandler.OPERATION_NAME, SchemaLocationAddHandler.INSTANCE, SchemaLocationAddHandler.INSTANCE, false);
        hostRegistration.registerOperationHandler(SchemaLocationRemoveHandler.OPERATION_NAME, SchemaLocationRemoveHandler.INSTANCE, SchemaLocationRemoveHandler.INSTANCE, false);
        hostRegistration.registerReadWriteAttribute(NAME, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1), Storage.CONFIGURATION);
        hostRegistration.registerReadOnlyAttribute(MASTER, IsMasterHandler.INSTANCE, Storage.RUNTIME);
        StartServersHandler ssh = new StartServersHandler(environment, serverInventory);
        hostRegistration.registerOperationHandler(StartServersHandler.OPERATION_NAME, ssh, ssh, false, OperationEntry.EntryType.PRIVATE);
        HostShutdownHandler hsh = new HostShutdownHandler(domainController);
        hostRegistration.registerOperationHandler(HostShutdownHandler.OPERATION_NAME, hsh, hsh);

        // System Properties
        ManagementResourceRegistration sysProps = hostRegistration.registerSubModel(PathElement.pathElement(SYSTEM_PROPERTY), HostDescriptionProviders.SYSTEM_PROPERTIES_PROVIDER);
        sysProps.registerOperationHandler(SystemPropertyAddHandler.OPERATION_NAME, SystemPropertyAddHandler.INSTANCE_WITH_BOOTTIME, SystemPropertyAddHandler.INSTANCE_WITH_BOOTTIME, false);
        sysProps.registerOperationHandler(SystemPropertyRemoveHandler.OPERATION_NAME, SystemPropertyRemoveHandler.INSTANCE, SystemPropertyRemoveHandler.INSTANCE, false);
        sysProps.registerReadWriteAttribute(VALUE, null, SystemPropertyValueWriteAttributeHandler.INSTANCE, Storage.CONFIGURATION);
        sysProps.registerReadWriteAttribute(BOOT_TIME, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);

        // Central Management
        ManagementResourceRegistration management = hostRegistration.registerSubModel(PathElement.pathElement(CORE_SERVICE, MANAGEMENT), CommonProviders.MANAGEMENT_WITH_INTERFACES_PROVIDER);
        ManagementResourceRegistration securityRealm = management.registerSubModel(PathElement.pathElement(SECURITY_REALM), CommonProviders.MANAGEMENT_SECURITY_REALM_PROVIDER);
        securityRealm.registerOperationHandler(SecurityRealmAddHandler.OPERATION_NAME, SecurityRealmAddHandler.INSTANCE, SecurityRealmAddHandler.INSTANCE, false);

        ManagementResourceRegistration connection = management.registerSubModel(PathElement.pathElement(OUTBOUND_CONNECTION), CommonProviders.MANAGEMENT_OUTBOUND_CONNECTION_PROVIDER);
        connection.registerOperationHandler(ConnectionAddHandler.OPERATION_NAME, ConnectionAddHandler.INSTANCE, ConnectionAddHandler.INSTANCE, false);
        // Management API protocols
        ManagementResourceRegistration managementNative = management.registerSubModel(PathElement.pathElement(MANAGEMENT_INTERFACE, NATIVE_INTERFACE), CommonProviders.NATIVE_MANAGEMENT_PROVIDER);
        NativeManagementAddHandler nmah = new NativeManagementAddHandler(hostControllerInfo);
        managementNative.registerOperationHandler(NativeManagementAddHandler.OPERATION_NAME, nmah, nmah, false);
        managementNative.registerReadWriteAttribute(SECURITY_REALM, null, new NativeManagementAttributeHandlers.NativeManagementAttributeHandler(hostControllerInfo), Storage.CONFIGURATION);

        ManagementResourceRegistration managementHttp = management.registerSubModel(PathElement.pathElement(MANAGEMENT_INTERFACE, HTTP_INTERFACE), CommonProviders.HTTP_MANAGEMENT_PROVIDER);
        HttpManagementAddHandler httpAddHandler = HttpManagementAddHandler.getInstance(hostControllerInfo);
        managementHttp.registerOperationHandler(HttpManagementAddHandler.OPERATION_NAME, httpAddHandler, httpAddHandler, false);
        managementHttp.registerReadWriteAttribute(SECURITY_REALM, null, new HttpManagementAttributeHandlers.HttpManagementAttributeHandler(hostControllerInfo, environment), Storage.CONFIGURATION);

        // hostRegistration.registerReadWriteAttribute(ModelDescriptionConstants.MANAGEMENT_INTERFACE, GlobalOperationHandlers.READ_ATTRIBUTE, ManagementSocketAddHandler.INSTANCE);
        //hostRegistration.registerOperationHandler(ManagementSocketRemoveHandler.OPERATION_NAME, ManagementSocketRemoveHandler.INSTANCE, ManagementSocketRemoveHandler.INSTANCE, false);

        // Other core services
        // TODO get a DumpServicesHandler that works on the domain
//        ManagementResourceRegistration serviceContainer = hostRegistration.registerSubModel(PathElement.pathElement(CORE_SERVICE, SERVICE_CONTAINER), CommonProviders.SERVICE_CONTAINER_PROVIDER);
//        serviceContainer.registerOperationHandler(DumpServicesHandler.OPERATION_NAME, DumpServicesHandler.INSTANCE, DumpServicesHandler.INSTANCE, false);


        LocalDomainControllerAddHandler localDcAddHandler = LocalDomainControllerAddHandler.getInstance(root, hostControllerInfo,
                environment, configurationPersister, localFileRepository, domainController, registry);
        hostRegistration.registerOperationHandler(LocalDomainControllerAddHandler.OPERATION_NAME, localDcAddHandler, localDcAddHandler, false);
        hostRegistration.registerOperationHandler(LocalDomainControllerRemoveHandler.OPERATION_NAME, LocalDomainControllerRemoveHandler.INSTANCE, LocalDomainControllerRemoveHandler.INSTANCE, false);
        RemoteDomainControllerAddHandler remoteDcAddHandler = RemoteDomainControllerAddHandler.getInstance(root, hostControllerInfo,
                configurationPersister, remoteFileRepository);
        hostRegistration.registerOperationHandler(RemoteDomainControllerAddHandler.OPERATION_NAME, remoteDcAddHandler, remoteDcAddHandler, false);
        hostRegistration.registerOperationHandler(RemoteDomainControllerRemoveHandler.OPERATION_NAME, RemoteDomainControllerRemoveHandler.INSTANCE, RemoteDomainControllerRemoveHandler.INSTANCE, false);
        SnapshotDeleteHandler snapshotDelete = new SnapshotDeleteHandler(configurationPersister.getHostPersister());
        hostRegistration.registerOperationHandler(SnapshotDeleteHandler.OPERATION_NAME, snapshotDelete, snapshotDelete, false);
        SnapshotListHandler snapshotList = new SnapshotListHandler(configurationPersister.getHostPersister());
        hostRegistration.registerOperationHandler(SnapshotListHandler.OPERATION_NAME, snapshotList, snapshotList, false);
        SnapshotTakeHandler snapshotTake = new SnapshotTakeHandler(configurationPersister.getHostPersister());
        hostRegistration.registerOperationHandler(SnapshotTakeHandler.OPERATION_NAME, snapshotTake, snapshotTake, false);

        //Extensions
        ManagementResourceRegistration extensions = hostRegistration.registerSubModel(PathElement.pathElement(EXTENSION), CommonProviders.EXTENSION_PROVIDER);
        ExtensionContext extensionContext = new ExtensionContextImpl(hostRegistration, null, configurationPersister);
        ExtensionAddHandler addExtensionHandler = new ExtensionAddHandler(extensionContext);
        extensions.registerOperationHandler(ExtensionAddHandler.OPERATION_NAME, addExtensionHandler, addExtensionHandler, false);
        extensions.registerOperationHandler(ExtensionRemoveHandler.OPERATION_NAME, ExtensionRemoveHandler.INSTANCE, ExtensionRemoveHandler.INSTANCE, false);

        // Jvms
        final ManagementResourceRegistration jvms = hostRegistration.registerSubModel(PathElement.pathElement(JVM), CommonProviders.JVM_PROVIDER);
        JVMHandlers.register(jvms);

        //Paths
        ManagementResourceRegistration paths = hostRegistration.registerSubModel(PathElement.pathElement(PATH), CommonProviders.SPECIFIED_PATH_PROVIDER);
        paths.registerOperationHandler(PathAddHandler.OPERATION_NAME, PathAddHandler.SPECIFIED_INSTANCE, PathAddHandler.SPECIFIED_INSTANCE, false);
        paths.registerOperationHandler(PathRemoveHandler.OPERATION_NAME, PathRemoveHandler.INSTANCE, PathRemoveHandler.INSTANCE, false);

        //interface
        ManagementResourceRegistration interfaces = hostRegistration.registerSubModel(PathElement.pathElement(INTERFACE), CommonProviders.SPECIFIED_INTERFACE_PROVIDER);
        HostSpecifiedInterfaceAddHandler hsiah = new HostSpecifiedInterfaceAddHandler(hostControllerInfo);
        interfaces.registerOperationHandler(InterfaceAddHandler.OPERATION_NAME, hsiah, hsiah, false);
        HostSpecifiedInterfaceRemoveHandler sirh = new HostSpecifiedInterfaceRemoveHandler(hostControllerInfo);
        interfaces.registerOperationHandler(InterfaceRemoveHandler.OPERATION_NAME, sirh, sirh, false);

        //server
        ManagementResourceRegistration servers = hostRegistration.registerSubModel(PathElement.pathElement(SERVER_CONFIG), HostDescriptionProviders.SERVER_PROVIDER);
        servers.registerOperationHandler(ServerAddHandler.OPERATION_NAME, ServerAddHandler.INSTANCE, ServerAddHandler.INSTANCE, false);
        servers.registerOperationHandler(ServerRemoveHandler.OPERATION_NAME, ServerRemoveHandler.INSTANCE, ServerRemoveHandler.INSTANCE, false);
        servers.registerReadWriteAttribute(AUTO_START, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        servers.registerReadWriteAttribute(SOCKET_BINDING_GROUP, null, WriteAttributeHandlers.WriteAttributeOperationHandler.INSTANCE, Storage.CONFIGURATION);
        servers.registerReadWriteAttribute(SOCKET_BINDING_PORT_OFFSET, null, new WriteAttributeHandlers.IntRangeValidatingHandler(0), Storage.CONFIGURATION);
        servers.registerReadWriteAttribute(PRIORITY, null, new WriteAttributeHandlers.IntRangeValidatingHandler(0), Storage.CONFIGURATION);
        servers.registerReadWriteAttribute(CPU_AFFINITY, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1), Storage.CONFIGURATION);

        // Register server runtime operation handlers
        servers.registerMetric(ServerStatusHandler.ATTRIBUTE_NAME, new ServerStatusHandler(serverInventory));
        ServerStartHandler startHandler = new ServerStartHandler(serverInventory);
        servers.registerOperationHandler(ServerStartHandler.OPERATION_NAME, startHandler, startHandler, false);
        ServerRestartHandler restartHandler = new ServerRestartHandler(serverInventory);
        servers.registerOperationHandler(ServerRestartHandler.OPERATION_NAME, restartHandler, restartHandler, false);
        ServerStopHandler stopHandler = new ServerStopHandler(serverInventory);
        servers.registerOperationHandler(ServerStopHandler.OPERATION_NAME, stopHandler, stopHandler, false);

        //server paths
        ManagementResourceRegistration serverPaths = servers.registerSubModel(PathElement.pathElement(PATH), CommonProviders.SPECIFIED_INTERFACE_PROVIDER);
        serverPaths.registerOperationHandler(PathAddHandler.OPERATION_NAME, PathAddHandler.SPECIFIED_INSTANCE, PathAddHandler.SPECIFIED_INSTANCE, false);
        serverPaths.registerOperationHandler(PathRemoveHandler.OPERATION_NAME, PathRemoveHandler.INSTANCE, PathRemoveHandler.INSTANCE, false);
        //server interfaces
        ManagementResourceRegistration serverInterfaces = servers.registerSubModel(PathElement.pathElement(INTERFACE), CommonProviders.SPECIFIED_INTERFACE_PROVIDER);
        serverInterfaces.registerOperationHandler(InterfaceAddHandler.OPERATION_NAME, SpecifiedInterfaceAddHandler.INSTANCE, SpecifiedInterfaceAddHandler.INSTANCE, false);
        serverInterfaces.registerOperationHandler(InterfaceRemoveHandler.OPERATION_NAME, SpecifiedInterfaceRemoveHandler.INSTANCE, SpecifiedInterfaceRemoveHandler.INSTANCE, false);
        // Server system Properties
        ManagementResourceRegistration serverSysProps = servers.registerSubModel(PathElement.pathElement(SYSTEM_PROPERTY), HostDescriptionProviders.SERVER_SYSTEM_PROPERTIES_PROVIDER);
        serverSysProps.registerOperationHandler(SystemPropertyAddHandler.OPERATION_NAME, SystemPropertyAddHandler.INSTANCE_WITH_BOOTTIME, SystemPropertyAddHandler.INSTANCE_WITH_BOOTTIME, false);
        serverSysProps.registerOperationHandler(SystemPropertyRemoveHandler.OPERATION_NAME, SystemPropertyRemoveHandler.INSTANCE, SystemPropertyRemoveHandler.INSTANCE, false);
        serverSysProps.registerReadWriteAttribute(VALUE, null, SystemPropertyValueWriteAttributeHandler.INSTANCE, Storage.CONFIGURATION);
        serverSysProps.registerReadWriteAttribute(BOOT_TIME, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);

        // Server jvm
        final ManagementResourceRegistration serverVMs = servers.registerSubModel(PathElement.pathElement(JVM), JVMHandlers.SERVER_MODEL_PROVIDER);
        JVMHandlers.register(serverVMs, true);

        //TODO register the rest of the server values
    }

    public static void registerServerInventory(final ServerInventory serverInventory) {

    }
}
