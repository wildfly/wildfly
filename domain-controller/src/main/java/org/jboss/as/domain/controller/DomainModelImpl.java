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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import org.jboss.as.controller.BasicModelController;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ExtensionContextImpl;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.operations.common.InterfaceAddHandler;
import org.jboss.as.controller.operations.common.NamespaceAddHandler;
import org.jboss.as.controller.operations.common.NamespaceRemoveHandler;
import org.jboss.as.controller.operations.common.PathAddHandler;
import org.jboss.as.controller.operations.common.PathRemoveHandler;
import org.jboss.as.controller.operations.common.SchemaLocationAddHandler;
import org.jboss.as.controller.operations.common.SchemaLocationRemoveHandler;
import org.jboss.as.controller.operations.common.SocketBindingAddHandler;
import org.jboss.as.controller.operations.common.SocketBindingRemoveHandler;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.domain.controller.descriptions.DomainDescriptionProviders;
import org.jboss.as.domain.controller.operations.ProfileAddHandler;
import org.jboss.as.domain.controller.operations.ProfileDescribeHandler;
import org.jboss.as.domain.controller.operations.ProfileRemoveHandler;
import org.jboss.as.domain.controller.operations.ServerGroupAddHandler;
import org.jboss.as.domain.controller.operations.ServerGroupRemoveHandler;
import org.jboss.as.domain.controller.operations.SocketBindingGroupAddHandler;
import org.jboss.as.server.controller.descriptions.ServerDescriptionProviders;
import org.jboss.as.server.operations.ExtensionAddHandler;
import org.jboss.as.server.operations.ExtensionRemoveHandler;
import org.jboss.as.server.operations.SystemPropertyAddHandler;
import org.jboss.as.server.operations.SystemPropertyRemoveHandler;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
class DomainModelImpl extends BasicModelController implements DomainModel {

    protected DomainModelImpl(final ExtensibleConfigurationPersister configurationPersister) {
        super(configurationPersister, DomainDescriptionProviders.ROOT_PROVIDER);
        initialize(getRegistry(), configurationPersister);
        createCoreModel();
    }

    protected DomainModelImpl(final ModelNode model, final ExtensibleConfigurationPersister configurationPersister) {
        super(model, configurationPersister, DomainDescriptionProviders.ROOT_PROVIDER);
        initialize(getRegistry(), configurationPersister);
    }

    private void createCoreModel() {
        // Create roots
        final ModelNode rootModel = getModel();
        rootModel.get(EXTENSION);
        rootModel.get(INTERFACE);
        rootModel.get(PROFILE);
        rootModel.get(SERVER_GROUP);
        rootModel.get(SOCKET_BINDING_GROUP);
        rootModel.get(PATH);
    }

    public ModelNode getDomainModel() {
        return super.getModel().clone();
    }

    protected static void initialize(final ModelNodeRegistration root, final ExtensibleConfigurationPersister configurationPersister) {
        // Global operations
        root.registerOperationHandler(READ_RESOURCE_OPERATION, GlobalOperationHandlers.READ_RESOURCE, CommonProviders.READ_RESOURCE_PROVIDER, true);
        root.registerOperationHandler(READ_ATTRIBUTE_OPERATION, GlobalOperationHandlers.READ_ATTRIBUTE, CommonProviders.READ_ATTRIBUTE_PROVIDER, true);
        root.registerOperationHandler(READ_RESOURCE_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_RESOURCE_DESCRIPTION, CommonProviders.READ_RESOURCE_DESCRIPTION_PROVIDER, true);
        root.registerOperationHandler(READ_CHILDREN_NAMES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_NAMES, CommonProviders.READ_CHILDREN_NAMES_PROVIDER, true);
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
        // root.registerOperationHandler(ReadConfigAsXmlHandler.READ_CONFIG_AS_XML, ReadConfigAsXmlHandler.INSTANCE, ReadConfigAsXmlHandler.INSTANCE, false);

        final ModelNodeRegistration interfaces = root.registerSubModel(PathElement.pathElement(INTERFACE), CommonProviders.NAMED_INTERFACE_PROVIDER);
        interfaces.registerOperationHandler(ADD, InterfaceAddHandler.NAMED_INSTANCE, InterfaceAddHandler.NAMED_INSTANCE, false);
        interfaces.registerOperationHandler(REMOVE, InterfaceAddHandler.NAMED_INSTANCE, InterfaceAddHandler.NAMED_INSTANCE, false);

        final ModelNodeRegistration profile = root.registerSubModel(PathElement.pathElement(PROFILE), DomainDescriptionProviders.PROFILE);
        profile.registerOperationHandler(ADD, ProfileAddHandler.INSTANCE, DomainDescriptionProviders.PROFILE_ADD, false);
        profile.registerOperationHandler(REMOVE, ProfileRemoveHandler.INSTANCE, DomainDescriptionProviders.PROFILE_REMOVE, false);
        profile.registerOperationHandler(DESCRIBE, ProfileDescribeHandler.INSTANCE, DomainDescriptionProviders.PROFILE_DESCRIBE, false);

        final ModelNodeRegistration paths = root.registerSubModel(PathElement.pathElement(PATH), DomainDescriptionProviders.PATH_DESCRIPTION);
        paths.registerOperationHandler(ADD, PathAddHandler.NAMED_INSTANCE, DomainDescriptionProviders.PATH_ADD, false);
        paths.registerOperationHandler(REMOVE, PathRemoveHandler.INSTANCE, DomainDescriptionProviders.PATH_REMOVE, false);

        final ModelNodeRegistration socketBindingGroup = root.registerSubModel(PathElement.pathElement(SOCKET_BINDING_GROUP), DomainDescriptionProviders.SOCKET_BINDING_GROUP);
        socketBindingGroup.registerOperationHandler(ADD, SocketBindingGroupAddHandler.INSTANCE, DomainDescriptionProviders.SOCKET_BINDING_GROUP, false);
        // TODO remove
        final ModelNodeRegistration socketBindings = socketBindingGroup.registerSubModel(PathElement.pathElement(SOCKET_BINDING), DomainDescriptionProviders.SOCKET_BINDING);
        socketBindings.registerOperationHandler(ADD, SocketBindingAddHandler.INSTANCE, DomainDescriptionProviders.SOCKET_BINDING_ADD, false);
        socketBindings.registerOperationHandler(REMOVE, SocketBindingRemoveHandler.INSTANCE, DomainDescriptionProviders.SOCKET_BINDING_REMOVE, false);

        final ModelNodeRegistration serverGroups = root.registerSubModel(PathElement.pathElement(SERVER_GROUP), DomainDescriptionProviders.SERVER_GROUP);
        serverGroups.registerOperationHandler(ADD, ServerGroupAddHandler.INSTANCE, DomainDescriptionProviders.SERVER_GROUP_ADD, false);
        serverGroups.registerOperationHandler(REMOVE, ServerGroupRemoveHandler.INSTANCE, DomainDescriptionProviders.SERVER_GROUP_REMOVE, false);
        // serverGroups.registerReadWriteAttribute(JVM, null, null);

        // Deployments
        final ModelNodeRegistration deployments = root.registerSubModel(PathElement.pathElement(DEPLOYMENT), ServerDescriptionProviders.DEPLOYMENT_PROVIDER);

        // Extensions
        final ModelNodeRegistration extensions = root.registerSubModel(PathElement.pathElement(EXTENSION), CommonProviders.EXTENSION_PROVIDER);
        final ExtensionContext extensionContext = new ExtensionContextImpl(profile, deployments, configurationPersister);
        final ExtensionAddHandler addExtensionHandler = new ExtensionAddHandler(extensionContext);
        extensions.registerOperationHandler(ExtensionAddHandler.OPERATION_NAME, addExtensionHandler, addExtensionHandler, false);
        extensions.registerOperationHandler(ExtensionRemoveHandler.OPERATION_NAME, ExtensionRemoveHandler.INSTANCE, ExtensionRemoveHandler.INSTANCE, false);
    }

    protected ModelNodeRegistration getRegistry() {
        return super.getRegistry();
    }
}
