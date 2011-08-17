/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.subsystem.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FIXED_PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INET_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MULTICAST_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MULTICAST_PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.common.PathDescription.RELATIVE_TO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.operations.common.InterfaceAddHandler;
import org.jboss.as.controller.operations.common.SocketBindingGroupRemoveHandler;
import org.jboss.as.controller.operations.common.SystemPropertyAddHandler;
import org.jboss.as.controller.operations.common.SystemPropertyRemoveHandler;
import org.jboss.as.controller.operations.common.SystemPropertyValueWriteAttributeHandler;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.controller.descriptions.ServerDescriptionProviders;
import org.jboss.as.server.operations.SpecifiedPathAddHandler;
import org.jboss.as.server.operations.SpecifiedPathRemoveHandler;
import org.jboss.as.server.services.net.BindingAddHandler;
import org.jboss.as.server.services.net.BindingFixedPortHandler;
import org.jboss.as.server.services.net.BindingGroupAddHandler;
import org.jboss.as.server.services.net.BindingGroupDefaultInterfaceHandler;
import org.jboss.as.server.services.net.BindingGroupPortOffsetHandler;
import org.jboss.as.server.services.net.BindingInterfaceHandler;
import org.jboss.as.server.services.net.BindingMetricHandlers;
import org.jboss.as.server.services.net.BindingMulticastAddressHandler;
import org.jboss.as.server.services.net.BindingMulticastPortHandler;
import org.jboss.as.server.services.net.BindingPortHandler;
import org.jboss.as.server.services.net.BindingRemoveHandler;
import org.jboss.as.server.services.net.SpecifiedInterfaceAddHandler;
import org.jboss.as.server.services.net.SpecifiedInterfaceRemoveHandler;
import org.jboss.dmr.ModelNode;

/**
 * Allows easy initialization of parts of the model that subsystems frequently need
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ControllerInitializer {

    public static final String INTERFACE_NAME = "test-interface";
    public static final String SOCKET_BINDING_GROUP_NAME = "test-socket-binding-group";
    protected volatile String bindAddress = "localhost";
    protected final Map<String, String> systemProperties = new HashMap<String, String>();
    protected final Map<String, Integer> socketBindings = new HashMap<String, Integer>();
    protected final Map<String, PathInfo> paths = new HashMap<String, PathInfo>();

    /**
     * Adds a system property to the model.
     * This initializes the system property part of the model with the operations to add it.
     *
     * @param key the system property name
     * @param value the system property value
     */
    public void addSystemProperty(String key, String value) {
        if (key == null) {
            throw new IllegalArgumentException("Null key");
        }
        if (value == null) {
            throw new IllegalArgumentException("Null value");
        }
        systemProperties.put(key, value);
    }

    /**
     * Sets the bindAddress that will be used for socket bindings.
     * The default is 'localhost'
     *
     * @param address the default bind address
     */
    public void setBindAddress(String address) {
        if (address == null) {
            throw new IllegalArgumentException("Null address");
        }
        bindAddress = address;
    }

    /**
     * Adds a socket binding to the model.
     * This initializes the interface and socket binding group part of the model with the operations to add it.
     *
     * @param name the socket binding name
     * @param port the socket binding port
     */
    public void addSocketBinding(String name, int port) {
        if (name == null) {
            throw new IllegalArgumentException("Null name");
        }
        if (port < 0) {
            throw new IllegalArgumentException("Null port");
        }

        socketBindings.put(name, port);
    }

    /**
     * Adds a path to the model
     * This initializes the path part of the model with the operations to add it.
     *
     * @param name the name of the path
     * @param path the absolute path, or the name of a path (if used with {@code relativeTo}
     * @param relativeTo a path relative to {@code path}
     */
    public void addPath(String name, String path, String relativeTo) {
        if (name == null) {
            throw new IllegalArgumentException("Null name");
        }
        if (path == null) {
            throw new IllegalArgumentException("Null path");
        }

        PathInfo pathInfo = new PathInfo(name, path, relativeTo);
        paths.put(name, pathInfo);
    }

    /**
     * Called by framework to set up the model
     *
     * @param rootResource the root model resource
     * @param rootRegistration the root model registry
     */
    protected void initializeModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
        initializeSystemPropertiesModel(rootResource, rootRegistration);
        initializeSocketBindingsModel(rootResource, rootRegistration);
        initializePathsModel(rootResource, rootRegistration);
    }

    /**
     * Called by framework to get the additional boot operations
     *
     * @return the additional boot operations
     */
    protected List<ModelNode> initializeBootOperations(){
        List<ModelNode> ops = new ArrayList<ModelNode>();
        initializeSystemPropertiesOperations(ops);
        initializePathsOperations(ops);
        initializeSocketBindingsOperations(ops);
        return ops;
    }

    /**
     * Initializes the system properties part of the model
     *
     * @param rootResource the root model resource
     * @param rootRegistration the root model registry
     */
    protected void initializeSystemPropertiesModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
        if (systemProperties.size() == 0) {
            return;
        }
        rootResource.getModel().get(SYSTEM_PROPERTY);
        ManagementResourceRegistration sysProps = rootRegistration.registerSubModel(PathElement.pathElement(SYSTEM_PROPERTY), ServerDescriptionProviders.SYSTEM_PROPERTIES_PROVIDER);
        sysProps.registerOperationHandler(SystemPropertyAddHandler.OPERATION_NAME, SystemPropertyAddHandler.INSTANCE_WITHOUT_BOOTTIME, SystemPropertyAddHandler.INSTANCE_WITHOUT_BOOTTIME, false);
        sysProps.registerOperationHandler(SystemPropertyRemoveHandler.OPERATION_NAME, SystemPropertyRemoveHandler.INSTANCE, SystemPropertyRemoveHandler.INSTANCE, false);
        sysProps.registerReadWriteAttribute(VALUE, null, SystemPropertyValueWriteAttributeHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
    }

    /**
     * Initializes the interface, socket binding group and socket binding part of the model
     *
     * @param rootResource the root model resource
     * @param rootRegistration the root model registry
     */
    protected void initializeSocketBindingsModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
        if (socketBindings.size() == 0) {
            return;
        }

        rootResource.getModel().get(INTERFACE);
        rootResource.getModel().get(SOCKET_BINDING_GROUP);
        ManagementResourceRegistration interfaces = rootRegistration.registerSubModel(PathElement.pathElement(INTERFACE), CommonProviders.SPECIFIED_INTERFACE_PROVIDER);
        interfaces.registerOperationHandler(SpecifiedInterfaceAddHandler.OPERATION_NAME, SpecifiedInterfaceAddHandler.INSTANCE, SpecifiedInterfaceAddHandler.INSTANCE, false);
        interfaces.registerOperationHandler(SpecifiedInterfaceRemoveHandler.OPERATION_NAME, SpecifiedInterfaceRemoveHandler.INSTANCE, SpecifiedInterfaceRemoveHandler.INSTANCE, false);

        // Sockets
        ManagementResourceRegistration socketGroup = rootRegistration.registerSubModel(PathElement.pathElement(SOCKET_BINDING_GROUP), ServerDescriptionProviders.SOCKET_BINDING_GROUP_PROVIDER);
        socketGroup.registerOperationHandler(BindingGroupAddHandler.OPERATION_NAME, BindingGroupAddHandler.INSTANCE, BindingGroupAddHandler.INSTANCE, false);
        socketGroup.registerOperationHandler(SocketBindingGroupRemoveHandler.OPERATION_NAME, SocketBindingGroupRemoveHandler.INSTANCE, SocketBindingGroupRemoveHandler.INSTANCE, false);
        socketGroup.registerReadWriteAttribute(PORT_OFFSET, null, BindingGroupPortOffsetHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        socketGroup.registerReadWriteAttribute(DEFAULT_INTERFACE, null, BindingGroupDefaultInterfaceHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        ManagementResourceRegistration socketBinding = socketGroup.registerSubModel(PathElement.pathElement(SOCKET_BINDING), CommonProviders.SOCKET_BINDING_PROVIDER);
        socketBinding.registerOperationHandler(BindingAddHandler.OPERATION_NAME, BindingAddHandler.INSTANCE, BindingAddHandler.INSTANCE, false);
        socketBinding.registerOperationHandler(BindingRemoveHandler.OPERATION_NAME, BindingRemoveHandler.INSTANCE, BindingRemoveHandler.INSTANCE, false);
        socketBinding.registerMetric(BindingMetricHandlers.BoundHandler.ATTRIBUTE_NAME, BindingMetricHandlers.BoundHandler.INSTANCE);
        socketBinding.registerMetric(BindingMetricHandlers.BoundAddressHandler.ATTRIBUTE_NAME, BindingMetricHandlers.BoundAddressHandler.INSTANCE);
        socketBinding.registerMetric(BindingMetricHandlers.BoundPortHandler.ATTRIBUTE_NAME, BindingMetricHandlers.BoundPortHandler.INSTANCE);
        socketBinding.registerReadWriteAttribute(INTERFACE, null, BindingInterfaceHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        socketBinding.registerReadWriteAttribute(PORT, null, BindingPortHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        socketBinding.registerReadWriteAttribute(FIXED_PORT, null, BindingFixedPortHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        socketBinding.registerReadWriteAttribute(MULTICAST_ADDRESS, null, BindingMulticastAddressHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        socketBinding.registerReadWriteAttribute(MULTICAST_PORT, null, BindingMulticastPortHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
    }

    /**
     * Initializes the interface, socket binding group and socket binding part of the model
     *
     * @param rootResource the root model resource
     * @param rootRegistration the root model registry
     */
    protected void initializePathsModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
        if (paths.size() == 0) {
            return;
        }
        rootResource.getModel().get(PATH);
        ManagementResourceRegistration paths = rootRegistration.registerSubModel(PathElement.pathElement(PATH), CommonProviders.SPECIFIED_PATH_PROVIDER);
        paths.registerOperationHandler(SpecifiedPathAddHandler.OPERATION_NAME, SpecifiedPathAddHandler.INSTANCE, SpecifiedPathAddHandler.INSTANCE, false);
        paths.registerOperationHandler(SpecifiedPathRemoveHandler.OPERATION_NAME, SpecifiedPathRemoveHandler.INSTANCE, SpecifiedPathRemoveHandler.INSTANCE, false);
    }

    /**
     * Creates the additional add system property operations
     *
     * @param ops the operations list to add our ops to
     */
    protected void initializeSystemPropertiesOperations(List<ModelNode> ops) {
        for (Map.Entry<String, String> prop : systemProperties.entrySet()) {
            ModelNode op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement(SYSTEM_PROPERTY, prop.getKey())).toModelNode());
            op.get(VALUE).set(prop.getValue());
            ops.add(op);
        }
    }

    /**
     * Creates the additional add interface, socket binding group and socket binding operations
     *
     * @param ops the operations list to add our ops to
     */
    protected void initializeSocketBindingsOperations(List<ModelNode> ops) {
        if (socketBindings.size() == 0) {
            return;
        }

        //Add the interface
        ModelNode criteria = new ModelNode();
        criteria.add(INET_ADDRESS, bindAddress);
        ModelNode op = InterfaceAddHandler.getAddInterfaceOperation(
                PathAddress.pathAddress(PathElement.pathElement(INTERFACE, INTERFACE_NAME)).toModelNode(),
                criteria);
        ops.add(op);

        //Add the socket binding group
        op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, SOCKET_BINDING_GROUP_NAME)).toModelNode());
        op.get(DEFAULT_INTERFACE).set(INTERFACE_NAME);
        ops.add(op);


        for (Map.Entry<String, Integer> binding : socketBindings.entrySet()) {
            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, SOCKET_BINDING_GROUP_NAME),
                    PathElement.pathElement(SOCKET_BINDING, binding.getKey())).toModelNode());
            op.get(PORT).set(binding.getValue());
            ops.add(op);
        }
    }

    protected void initializePathsOperations(List<ModelNode> ops) {
        if (paths.size() == 0) {
            return;
        }

        for (PathInfo path : paths.values()) {
            ModelNode op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement(PATH, path.getName())).toModelNode());
            op.get(PATH).set(path.getPath());
            if (path.getRelativeTo() != null) {
                op.get(RELATIVE_TO).set(path.getRelativeTo());
            }
            ops.add(op);
        }
    }

    private static class PathInfo {
        private final String name;
        private final String path;
        private final String relativeTo;

        public PathInfo(String name, String path, String relativeTo) {
            this.name = name;
            this.path = path;
            this.relativeTo = relativeTo;
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }

        public String getRelativeTo() {
            return relativeTo;
        }
    }
}
