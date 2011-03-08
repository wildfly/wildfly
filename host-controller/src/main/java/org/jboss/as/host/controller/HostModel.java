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

package org.jboss.as.host.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CPU_AFFINITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_CONTROLLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HTTP_API;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NATIVE_API;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRIORITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_TYPES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNNING_SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import org.jboss.as.controller.BasicTransactionalModelController;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ExtensionContextImpl;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.operations.common.InterfaceAddHandler;
import org.jboss.as.controller.operations.common.InterfaceRemoveHandler;
import org.jboss.as.controller.operations.common.JVMHandlers;
import org.jboss.as.controller.operations.common.NamespaceAddHandler;
import org.jboss.as.controller.operations.common.NamespaceRemoveHandler;
import org.jboss.as.controller.operations.common.PathAddHandler;
import org.jboss.as.controller.operations.common.PathRemoveHandler;
import org.jboss.as.controller.operations.common.SchemaLocationAddHandler;
import org.jboss.as.controller.operations.common.SchemaLocationRemoveHandler;
import org.jboss.as.controller.operations.common.SystemPropertyAddHandler;
import org.jboss.as.controller.operations.common.SystemPropertyRemoveHandler;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.host.controller.descriptions.HostDescriptionProviders;
import org.jboss.as.host.controller.operations.ExtensionAddHandler;
import org.jboss.as.host.controller.operations.ExtensionRemoveHandler;
import org.jboss.as.host.controller.operations.LocalDomainControllerAddHandler;
import org.jboss.as.host.controller.operations.LocalDomainControllerRemoveHandler;
import org.jboss.as.host.controller.operations.RemoteDomainControllerAddHandler;
import org.jboss.as.host.controller.operations.RemoteDomainControllerRemoveHandler;
import org.jboss.as.host.controller.operations.ServerAddHandler;
import org.jboss.as.host.controller.operations.ServerRemoveHandler;
import org.jboss.as.host.controller.operations.ServerRestartHandler;
import org.jboss.as.host.controller.operations.ServerStartHandler;
import org.jboss.as.host.controller.operations.ServerStopHandler;
import org.jboss.as.host.controller.operations.SpecifiedInterfaceAddHandler;
import org.jboss.as.host.controller.operations.SpecifiedInterfaceRemoveHandler;
import org.jboss.as.server.operations.HttpManagementAddHandler;
import org.jboss.as.server.operations.NativeManagementAddHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Kabir Khan
 */
public class HostModel extends BasicTransactionalModelController {

    /**
     * @param configurationPersister
     */
    public HostModel(final ExtensibleConfigurationPersister configurationPersister) {
        super(createCoreModel(), configurationPersister, HostDescriptionProviders.ROOT_PROVIDER);

        // Register the operation handlers
        ModelNodeRegistration root = getRegistry();
        // Global operations
        root.registerOperationHandler(GlobalOperationHandlers.ResolveAddressOperationHandler.OPERATION_NAME, GlobalOperationHandlers.RESOLVE, GlobalOperationHandlers.RESOLVE, false, OperationEntry.EntryType.PRIVATE);
        root.registerOperationHandler(READ_RESOURCE_OPERATION, GlobalOperationHandlers.READ_RESOURCE, CommonProviders.READ_RESOURCE_PROVIDER, true);
        root.registerOperationHandler(READ_ATTRIBUTE_OPERATION, GlobalOperationHandlers.READ_ATTRIBUTE, CommonProviders.READ_ATTRIBUTE_PROVIDER, true);
        root.registerOperationHandler(READ_RESOURCE_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_RESOURCE_DESCRIPTION, CommonProviders.READ_RESOURCE_DESCRIPTION_PROVIDER, true);
        root.registerOperationHandler(READ_CHILDREN_NAMES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_NAMES, CommonProviders.READ_CHILDREN_NAMES_PROVIDER, true);
        root.registerOperationHandler(READ_CHILDREN_TYPES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_TYPES, CommonProviders.READ_CHILDREN_TYPES_PROVIDER, true);
        root.registerOperationHandler(READ_OPERATION_NAMES_OPERATION, GlobalOperationHandlers.READ_OPERATION_NAMES, CommonProviders.READ_OPERATION_NAMES_PROVIDER, true);
        root.registerOperationHandler(READ_OPERATION_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_OPERATION_DESCRIPTION, CommonProviders.READ_OPERATION_PROVIDER, true);
        root.registerOperationHandler(WRITE_ATTRIBUTE_OPERATION, GlobalOperationHandlers.WRITE_ATTRIBUTE, CommonProviders.WRITE_ATTRIBUTE_PROVIDER, true);

        // Other root resource operations
        root.registerOperationHandler(NamespaceAddHandler.OPERATION_NAME, NamespaceAddHandler.INSTANCE, NamespaceAddHandler.INSTANCE, false);
        root.registerOperationHandler(NamespaceRemoveHandler.OPERATION_NAME, NamespaceRemoveHandler.INSTANCE, NamespaceRemoveHandler.INSTANCE, false);
        root.registerOperationHandler(SchemaLocationAddHandler.OPERATION_NAME, SchemaLocationAddHandler.INSTANCE, SchemaLocationAddHandler.INSTANCE, false);
        root.registerOperationHandler(SchemaLocationRemoveHandler.OPERATION_NAME, SchemaLocationRemoveHandler.INSTANCE, SchemaLocationRemoveHandler.INSTANCE, false);
        root.registerOperationHandler(SystemPropertyAddHandler.OPERATION_NAME, SystemPropertyAddHandler.INSTANCE, SystemPropertyAddHandler.INSTANCE, false);
        root.registerOperationHandler(SystemPropertyRemoveHandler.OPERATION_NAME, SystemPropertyRemoveHandler.INSTANCE, SystemPropertyRemoveHandler.INSTANCE, false);
        root.registerReadWriteAttribute(NAME, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1), Storage.CONFIGURATION);
        // Management API protocols
        ModelNodeRegistration managementNative = root.registerSubModel(PathElement.pathElement(MANAGEMENT, NATIVE_API), CommonProviders.MANAGEMENT_PROVIDER);
        managementNative.registerOperationHandler(NativeManagementAddHandler.OPERATION_NAME, NativeManagementAddHandler.INSTANCE, NativeManagementAddHandler.INSTANCE, false);

        ModelNodeRegistration managementHttp = root.registerSubModel(PathElement.pathElement(MANAGEMENT, HTTP_API), CommonProviders.MANAGEMENT_PROVIDER);
        managementHttp.registerOperationHandler(HttpManagementAddHandler.OPERATION_NAME, HttpManagementAddHandler.INSTANCE, HttpManagementAddHandler.INSTANCE, false);
        // root.registerReadWriteAttribute(ModelDescriptionConstants.MANAGEMENT, GlobalOperationHandlers.READ_ATTRIBUTE, ManagementSocketAddHandler.INSTANCE);
        //root.registerOperationHandler(ManagementSocketRemoveHandler.OPERATION_NAME, ManagementSocketRemoveHandler.INSTANCE, ManagementSocketRemoveHandler.INSTANCE, false);
        root.registerOperationHandler(LocalDomainControllerAddHandler.OPERATION_NAME, LocalDomainControllerAddHandler.INSTANCE, LocalDomainControllerAddHandler.INSTANCE, false);
        root.registerOperationHandler(LocalDomainControllerRemoveHandler.OPERATION_NAME, LocalDomainControllerRemoveHandler.INSTANCE, LocalDomainControllerRemoveHandler.INSTANCE, false);
        root.registerOperationHandler(RemoteDomainControllerAddHandler.OPERATION_NAME, RemoteDomainControllerAddHandler.INSTANCE, RemoteDomainControllerAddHandler.INSTANCE, false);
        root.registerOperationHandler(RemoteDomainControllerRemoveHandler.OPERATION_NAME, RemoteDomainControllerRemoveHandler.INSTANCE, RemoteDomainControllerRemoveHandler.INSTANCE, false);

        //Extensions
        ModelNodeRegistration extensions = root.registerSubModel(PathElement.pathElement(EXTENSION), CommonProviders.EXTENSION_PROVIDER);
        ExtensionContext extensionContext = new ExtensionContextImpl(getRegistry(), null, configurationPersister);
        ExtensionAddHandler addExtensionHandler = new ExtensionAddHandler(extensionContext);
        extensions.registerOperationHandler(ExtensionAddHandler.OPERATION_NAME, addExtensionHandler, addExtensionHandler, false);
        extensions.registerOperationHandler(ExtensionRemoveHandler.OPERATION_NAME, ExtensionRemoveHandler.INSTANCE, ExtensionRemoveHandler.INSTANCE, false);

        //TODO register system properties description provider
        //TODO register management description provider
        //TODO register domain controller description provider
        //TODO register jvm description provider

        final ModelNodeRegistration jvms = root.registerSubModel(PathElement.pathElement(JVM), CommonProviders.JVM_PROVIDER);
        JVMHandlers.register(jvms);

        //TODO register the rest of the root operations?

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
        servers.registerReadWriteAttribute(START, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        servers.registerReadWriteAttribute(SOCKET_BINDING_GROUP, null, WriteAttributeHandlers.WriteAttributeOperationHandler.INSTANCE, Storage.CONFIGURATION);
        servers.registerReadWriteAttribute(SOCKET_BINDING_PORT_OFFSET, null, new WriteAttributeHandlers.IntRangeValidatingHandler(1), Storage.CONFIGURATION);
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
        //TODO register server system properties description provider
        //TODO register server jvm description provider
        final ModelNodeRegistration serverVMs = servers.registerSubModel(PathElement.pathElement(JVM), CommonProviders.JVM_PROVIDER);
        JVMHandlers.register(serverVMs);

        //TODO register the rest of the server values

        registerInternalOperations();
    }

    private static ModelNode createCoreModel() {
        ModelNode root = new ModelNode();
        root.get(NAME);
        root.get(NAMESPACES).setEmptyList();
        root.get(SCHEMA_LOCATIONS).setEmptyList();
        root.get(EXTENSION);
        root.get(PATH);
        root.get(SYSTEM_PROPERTY);
        root.get(MANAGEMENT);
        root.get(SERVER_CONFIG);
        root.get(DOMAIN_CONTROLLER);
        root.get(INTERFACE);
        root.get(JVM);
        root.get(RUNNING_SERVER);
        return root;
    }

    /**
     * Get the undelying host model.
     *
     * @return the host model
     */
    protected ModelNode getHostModel() {
        return super.getModel().clone();
    }

    void registerProxy(final ProxyController controller) {
        final PathElement element = controller.getProxyNodeAddress().getLastElement();
        getRegistry().registerProxyController(element, controller);
        getModel().get(element.getKey(), element.getValue());
    }

    void unregisterProxy(final String serverName) {
        PathElement element = PathElement.pathElement(RUNNING_SERVER, serverName);
        getModel().get(element.getKey()).remove(element.getValue());
        getRegistry().unregisterProxyController(element);
    }

    void setHostController(final HostController hc) {
        // TODO consider getting rid of the HostController/HostModel split
        ServerStartHandler startHandler = new ServerStartHandler(hc);
        ServerRestartHandler restartHandler = new ServerRestartHandler(hc);
        ServerStopHandler stopHandler = new ServerStopHandler(hc);
        ModelNodeRegistration registry = getRegistry();
        registry.registerOperationHandler(ServerStartHandler.OPERATION_NAME, startHandler, startHandler, false);
        registry.registerOperationHandler(ServerRestartHandler.OPERATION_NAME, restartHandler, restartHandler, false);
        registry.registerOperationHandler(ServerStopHandler.OPERATION_NAME, stopHandler, stopHandler, false);
    }
}
