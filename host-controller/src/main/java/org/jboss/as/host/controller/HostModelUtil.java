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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONNECTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONNECTIONS;
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALMS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

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
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.domain.management.operations.ConnectionAddHandler;
import org.jboss.as.domain.management.operations.SecurityRealmAddHandler;
import org.jboss.as.host.controller.descriptions.HostDescriptionProviders;
import org.jboss.as.host.controller.operations.HttpManagementAddHandler;
import org.jboss.as.host.controller.operations.IsMasterHandler;
import org.jboss.as.host.controller.operations.LocalDomainControllerAddHandler;
import org.jboss.as.host.controller.operations.LocalDomainControllerRemoveHandler;
import org.jboss.as.host.controller.operations.LocalHostAddHandler;
import org.jboss.as.host.controller.operations.NativeManagementAddHandler;
import org.jboss.as.host.controller.operations.RemoteDomainControllerAddHandler;
import org.jboss.as.host.controller.operations.RemoteDomainControllerRemoveHandler;
import org.jboss.as.host.controller.operations.ServerAddHandler;
import org.jboss.as.host.controller.operations.ServerRemoveHandler;
import org.jboss.as.server.operations.ExtensionAddHandler;
import org.jboss.as.server.services.net.SpecifiedInterfaceAddHandler;
import org.jboss.as.server.services.net.SpecifiedInterfaceRemoveHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Utility for creating the root element and populating the {@link ModelNodeRegistration}
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
        root.get(MANAGEMENT).get(SECURITY_REALMS).get(SECURITY_REALM);
        root.get(MANAGEMENT).get(CONNECTIONS).get(CONNECTION);
        root.get(MANAGEMENT_INTERFACE);
        root.get(SERVER_CONFIG);
        root.get(DOMAIN_CONTROLLER);
        root.get(INTERFACE);
        root.get(JVM);
        root.get(RUNNING_SERVER);
    }

    /**
     * Create and return the base bootstrap host registry to allow the initial host registration.
     *
     * @return the ModelNodeRegistration
     */
    public static ModelNodeRegistration createBootstrapHostRegistry(ModelNodeRegistration hostRegistry, DomainModelProxy domainModelProxy) {
        final ModelNodeRegistration root = ModelNodeRegistration.Factory.create(HostDescriptionProviders.BOOTSTRAP_PROVIDER);

        ModelNodeRegistration hostRegistration = root.registerSubModel(PathElement.pathElement(HOST), HostDescriptionProviders.HOST_ROOT_PROVIDER);
        LocalHostAddHandler handler = LocalHostAddHandler.getInstance(hostRegistry, domainModelProxy);
        hostRegistration.registerOperationHandler(LocalHostAddHandler.OPERATION_NAME, handler, handler, false, OperationEntry.EntryType.PRIVATE);

        return root;
    }

    public static ModelNodeRegistration createHostRegistry(final ExtensibleConfigurationPersister configurationPersister, HostControllerEnvironment environment, DomainModelProxy domainModelProxy) {

        final ModelNodeRegistration root = ModelNodeRegistration.Factory.create(HostDescriptionProviders.ROOT_PROVIDER);
        // Global operations
        root.registerOperationHandler(GlobalOperationHandlers.ResolveAddressOperationHandler.OPERATION_NAME, GlobalOperationHandlers.RESOLVE, GlobalOperationHandlers.RESOLVE, false, OperationEntry.EntryType.PRIVATE);
        root.registerOperationHandler(READ_RESOURCE_OPERATION, GlobalOperationHandlers.READ_RESOURCE, CommonProviders.READ_RESOURCE_PROVIDER, true);
        root.registerOperationHandler(READ_ATTRIBUTE_OPERATION, GlobalOperationHandlers.READ_ATTRIBUTE, CommonProviders.READ_ATTRIBUTE_PROVIDER, true);
        root.registerOperationHandler(READ_RESOURCE_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_RESOURCE_DESCRIPTION, CommonProviders.READ_RESOURCE_DESCRIPTION_PROVIDER, true);
        root.registerOperationHandler(READ_CHILDREN_NAMES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_NAMES, CommonProviders.READ_CHILDREN_NAMES_PROVIDER, true);
        root.registerOperationHandler(READ_CHILDREN_TYPES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_TYPES, CommonProviders.READ_CHILDREN_TYPES_PROVIDER, true);
        root.registerOperationHandler(READ_CHILDREN_RESOURCES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_RESOURCES, CommonProviders.READ_CHILDREN_RESOURCES_PROVIDER, true);
        root.registerOperationHandler(READ_OPERATION_NAMES_OPERATION, GlobalOperationHandlers.READ_OPERATION_NAMES, CommonProviders.READ_OPERATION_NAMES_PROVIDER, true);
        root.registerOperationHandler(READ_OPERATION_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_OPERATION_DESCRIPTION, CommonProviders.READ_OPERATION_PROVIDER, true);
        root.registerOperationHandler(WRITE_ATTRIBUTE_OPERATION, GlobalOperationHandlers.WRITE_ATTRIBUTE, CommonProviders.WRITE_ATTRIBUTE_PROVIDER, true);

        // Other root resource operations
        XmlMarshallingHandler xmh = new XmlMarshallingHandler(configurationPersister);
        root.registerOperationHandler(XmlMarshallingHandler.OPERATION_NAME, xmh, xmh, false);
        root.registerOperationHandler(NamespaceAddHandler.OPERATION_NAME, NamespaceAddHandler.INSTANCE, NamespaceAddHandler.INSTANCE, false);
        root.registerOperationHandler(NamespaceRemoveHandler.OPERATION_NAME, NamespaceRemoveHandler.INSTANCE, NamespaceRemoveHandler.INSTANCE, false);
        root.registerOperationHandler(SchemaLocationAddHandler.OPERATION_NAME, SchemaLocationAddHandler.INSTANCE, SchemaLocationAddHandler.INSTANCE, false);
        root.registerOperationHandler(SchemaLocationRemoveHandler.OPERATION_NAME, SchemaLocationRemoveHandler.INSTANCE, SchemaLocationRemoveHandler.INSTANCE, false);
        root.registerReadWriteAttribute(NAME, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1), Storage.CONFIGURATION);
        root.registerReadOnlyAttribute(MASTER, IsMasterHandler.INSTANCE, Storage.RUNTIME);

        // System Properties
        ModelNodeRegistration sysProps = root.registerSubModel(PathElement.pathElement(SYSTEM_PROPERTY), HostDescriptionProviders.SYSTEM_PROPERTIES_PROVIDER);
        sysProps.registerOperationHandler(SystemPropertyAddHandler.OPERATION_NAME, SystemPropertyAddHandler.INSTANCE_WITH_BOOTTIME, SystemPropertyAddHandler.INSTANCE_WITH_BOOTTIME, false);
        sysProps.registerOperationHandler(SystemPropertyRemoveHandler.OPERATION_NAME, SystemPropertyRemoveHandler.INSTANCE, SystemPropertyRemoveHandler.INSTANCE, false);
        sysProps.registerReadWriteAttribute(VALUE, null, SystemPropertyValueWriteAttributeHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        sysProps.registerReadWriteAttribute(BOOT_TIME, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), AttributeAccess.Storage.CONFIGURATION);

        // Central Management
        // TODO - Need to split the provider.
        ModelNodeRegistration securityRealms = root.registerSubModel(PathElement.pathElement(MANAGEMENT, SECURITY_REALMS), CommonProviders.NATIVE_MANAGEMENT_PROVIDER);
        ModelNodeRegistration securityRealm = securityRealms.registerSubModel(PathElement.pathElement(SECURITY_REALM), CommonProviders.NATIVE_MANAGEMENT_PROVIDER);
        securityRealm.registerOperationHandler(SecurityRealmAddHandler.OPERATION_NAME, SecurityRealmAddHandler.INSTANCE, SecurityRealmAddHandler.INSTANCE, false);

        ModelNodeRegistration connections = root.registerSubModel(PathElement.pathElement(MANAGEMENT, CONNECTIONS), CommonProviders.NATIVE_MANAGEMENT_PROVIDER);
        ModelNodeRegistration connection = connections.registerSubModel(PathElement.pathElement(CONNECTION), CommonProviders.NATIVE_MANAGEMENT_PROVIDER);
        connection.registerOperationHandler(ConnectionAddHandler.OPERATION_NAME, ConnectionAddHandler.INSTANCE, ConnectionAddHandler.INSTANCE, false);
        // Management API protocols
        ModelNodeRegistration managementNative = root.registerSubModel(PathElement.pathElement(MANAGEMENT_INTERFACE, NATIVE_INTERFACE), CommonProviders.MANAGEMENT_INTERFACE_PROVIDER);
        managementNative.registerOperationHandler(NativeManagementAddHandler.OPERATION_NAME, NativeManagementAddHandler.INSTANCE, NativeManagementAddHandler.INSTANCE, false);

        ModelNodeRegistration managementHttp = root.registerSubModel(PathElement.pathElement(MANAGEMENT_INTERFACE, HTTP_INTERFACE), CommonProviders.MANAGEMENT_INTERFACE_PROVIDER);

        HttpManagementAddHandler httpAddHandler = HttpManagementAddHandler.getInstance(environment);
        managementHttp.registerOperationHandler(HttpManagementAddHandler.OPERATION_NAME, httpAddHandler, httpAddHandler, false);

        // root.registerReadWriteAttribute(ModelDescriptionConstants.MANAGEMENT_INTERFACE, GlobalOperationHandlers.READ_ATTRIBUTE, ManagementSocketAddHandler.INSTANCE);
        //root.registerOperationHandler(ManagementSocketRemoveHandler.OPERATION_NAME, ManagementSocketRemoveHandler.INSTANCE, ManagementSocketRemoveHandler.INSTANCE, false);

        LocalDomainControllerAddHandler localDcAddHandler = LocalDomainControllerAddHandler.getInstance(domainModelProxy, environment);
        root.registerOperationHandler(LocalDomainControllerAddHandler.OPERATION_NAME, localDcAddHandler, localDcAddHandler, false);
        root.registerOperationHandler(LocalDomainControllerRemoveHandler.OPERATION_NAME, LocalDomainControllerRemoveHandler.INSTANCE, LocalDomainControllerRemoveHandler.INSTANCE, false);
        RemoteDomainControllerAddHandler remoteDcAddHandler = RemoteDomainControllerAddHandler.getInstance(domainModelProxy, environment);
        root.registerOperationHandler(RemoteDomainControllerAddHandler.OPERATION_NAME, remoteDcAddHandler, remoteDcAddHandler, false);
        root.registerOperationHandler(RemoteDomainControllerRemoveHandler.OPERATION_NAME, RemoteDomainControllerRemoveHandler.INSTANCE, RemoteDomainControllerRemoveHandler.INSTANCE, false);
        SnapshotDeleteHandler snapshotDelete = new SnapshotDeleteHandler(configurationPersister);
        root.registerOperationHandler(SnapshotDeleteHandler.OPERATION_NAME, snapshotDelete, snapshotDelete, false);
        SnapshotListHandler snapshotList = new SnapshotListHandler(configurationPersister);
        root.registerOperationHandler(SnapshotListHandler.OPERATION_NAME, snapshotList, snapshotList, false);
        SnapshotTakeHandler snapshotTake = new SnapshotTakeHandler(configurationPersister);
        root.registerOperationHandler(SnapshotTakeHandler.OPERATION_NAME, snapshotTake, snapshotTake, false);

        //Extensions
        ModelNodeRegistration extensions = root.registerSubModel(PathElement.pathElement(EXTENSION), CommonProviders.EXTENSION_PROVIDER);
        ExtensionContext extensionContext = new ExtensionContextImpl(root, null, configurationPersister);
        ExtensionAddHandler addExtensionHandler = new ExtensionAddHandler(extensionContext);
        extensions.registerOperationHandler(ExtensionAddHandler.OPERATION_NAME, addExtensionHandler, addExtensionHandler, false);
        extensions.registerOperationHandler(ExtensionRemoveHandler.OPERATION_NAME, ExtensionRemoveHandler.INSTANCE, ExtensionRemoveHandler.INSTANCE, false);

        // Jvms
        final ModelNodeRegistration jvms = root.registerSubModel(PathElement.pathElement(JVM), CommonProviders.JVM_PROVIDER);
        JVMHandlers.register(jvms);

        //Paths
        ModelNodeRegistration paths = root.registerSubModel(PathElement.pathElement(PATH), CommonProviders.SPECIFIED_PATH_PROVIDER);
        paths.registerOperationHandler(PathAddHandler.OPERATION_NAME, PathAddHandler.SPECIFIED_INSTANCE, PathAddHandler.SPECIFIED_INSTANCE, false);
        paths.registerOperationHandler(PathRemoveHandler.OPERATION_NAME, PathRemoveHandler.INSTANCE, PathRemoveHandler.INSTANCE, false);

        //interface
        ModelNodeRegistration interfaces = root.registerSubModel(PathElement.pathElement(INTERFACE), CommonProviders.SPECIFIED_INTERFACE_PROVIDER);
        interfaces.registerOperationHandler(InterfaceAddHandler.OPERATION_NAME, SpecifiedInterfaceAddHandler.INSTANCE, SpecifiedInterfaceAddHandler.INSTANCE, false);
        interfaces.registerOperationHandler(InterfaceRemoveHandler.OPERATION_NAME, SpecifiedInterfaceRemoveHandler.INSTANCE, SpecifiedInterfaceRemoveHandler.INSTANCE, false);

        //server
        ModelNodeRegistration servers = root.registerSubModel(PathElement.pathElement(SERVER_CONFIG), HostDescriptionProviders.SERVER_PROVIDER);
        servers.registerOperationHandler(ServerAddHandler.OPERATION_NAME, ServerAddHandler.INSTANCE, ServerAddHandler.INSTANCE, false);
        servers.registerOperationHandler(ServerRemoveHandler.OPERATION_NAME, ServerRemoveHandler.INSTANCE, ServerRemoveHandler.INSTANCE, false);
        servers.registerReadWriteAttribute(AUTO_START, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        servers.registerReadWriteAttribute(SOCKET_BINDING_GROUP, null, WriteAttributeHandlers.WriteAttributeOperationHandler.INSTANCE, Storage.CONFIGURATION);
        servers.registerReadWriteAttribute(SOCKET_BINDING_PORT_OFFSET, null, new WriteAttributeHandlers.IntRangeValidatingHandler(0), Storage.CONFIGURATION);
        servers.registerReadWriteAttribute(PRIORITY, null, new WriteAttributeHandlers.IntRangeValidatingHandler(0), Storage.CONFIGURATION);
        servers.registerReadWriteAttribute(CPU_AFFINITY, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1), Storage.CONFIGURATION);
        //server paths
        ModelNodeRegistration serverPaths = servers.registerSubModel(PathElement.pathElement(PATH), CommonProviders.SPECIFIED_INTERFACE_PROVIDER);
        serverPaths.registerOperationHandler(PathAddHandler.OPERATION_NAME, PathAddHandler.SPECIFIED_INSTANCE, PathAddHandler.SPECIFIED_INSTANCE, false);
        serverPaths.registerOperationHandler(PathRemoveHandler.OPERATION_NAME, PathRemoveHandler.INSTANCE, PathRemoveHandler.INSTANCE, false);
        //server interfaces
        ModelNodeRegistration serverInterfaces = servers.registerSubModel(PathElement.pathElement(INTERFACE), CommonProviders.SPECIFIED_INTERFACE_PROVIDER);
        serverInterfaces.registerOperationHandler(InterfaceAddHandler.OPERATION_NAME, SpecifiedInterfaceAddHandler.INSTANCE, SpecifiedInterfaceAddHandler.INSTANCE, false);
        serverInterfaces.registerOperationHandler(InterfaceRemoveHandler.OPERATION_NAME, SpecifiedInterfaceRemoveHandler.INSTANCE, SpecifiedInterfaceRemoveHandler.INSTANCE, false);
        // Server system Properties
        ModelNodeRegistration serverSysProps = servers.registerSubModel(PathElement.pathElement(SYSTEM_PROPERTY), HostDescriptionProviders.SERVER_SYSTEM_PROPERTIES_PROVIDER);
        serverSysProps.registerOperationHandler(SystemPropertyAddHandler.OPERATION_NAME, SystemPropertyAddHandler.INSTANCE_WITH_BOOTTIME, SystemPropertyAddHandler.INSTANCE_WITH_BOOTTIME, false);
        serverSysProps.registerOperationHandler(SystemPropertyRemoveHandler.OPERATION_NAME, SystemPropertyRemoveHandler.INSTANCE, SystemPropertyRemoveHandler.INSTANCE, false);
        serverSysProps.registerReadWriteAttribute(VALUE, null, SystemPropertyValueWriteAttributeHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        serverSysProps.registerReadWriteAttribute(BOOT_TIME, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), AttributeAccess.Storage.CONFIGURATION);

        // Server jvm
        final ModelNodeRegistration serverVMs = servers.registerSubModel(PathElement.pathElement(JVM), JVMHandlers.SERVER_MODEL_PROVIDER);
        JVMHandlers.register(serverVMs, true);

        //TODO register the rest of the server values

        return root;
    }
}
