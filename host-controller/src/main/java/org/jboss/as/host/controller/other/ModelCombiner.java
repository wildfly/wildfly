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
package org.jboss.as.host.controller.other;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CRITERIA;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.InterfaceAddHandler;
import org.jboss.as.controller.operations.common.NamespaceAddHandler;
import org.jboss.as.controller.operations.common.PathAddHandler;
import org.jboss.as.controller.operations.common.SchemaLocationAddHandler;
import org.jboss.as.controller.operations.common.SocketBindingAddHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.host.controller.operations.ExtensionAddHandler;
import org.jboss.as.server.operations.SocketBindingGroupAddHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
class ModelCombiner {

    private static final ModelNode EMPTY = new ModelNode();
    static {
        EMPTY.setEmptyList();
        EMPTY.protect();
    }

    final String serverName;
    final ModelNode domainModel;
    final ModelNode hostModel;
    final ModelNode serverModel;
    final String profileName;

    ModelCombiner(String serverName, ModelNode domainModel, ModelNode hostModel) {
        this.serverName = serverName;
        this.domainModel = domainModel;
        this.hostModel = hostModel;
        this.serverModel = hostModel.require(SERVER).require(serverName);

        String serverGroupName = serverModel.require(GROUP).asString();
        this.profileName = domainModel.require(SERVER_GROUP).require(serverGroupName).require(PROFILE).asString();


    }

    List<ModelNode> createUpdates(){

        System.out.println("DOMAIN");
        System.out.println(domainModel);
        System.out.println("HOST");
        System.out.println(hostModel);


        List<ModelNode> updates = new ArrayList<ModelNode>();
        addNamespaces(updates);
        addServerName(updates);
        addSchemaLocations(updates);
        addExtensions(updates);
        addPaths(updates);
        addInterfaces(updates);
        addSocketBindings(updates);
        //TODO I think it is ok to not do
        //  management
        //  domain-controller

        //TODO deployments

        return updates;
    }

    private void addNamespaces(List<ModelNode> updates) {
        final Map<String, ModelNode> map = new LinkedHashMap<String, ModelNode>();
        addNamespaces(map, domainModel.get(NAMESPACES));
        addNamespaces(map, hostModel.get(NAMESPACES));
        updates.addAll(map.values());
    }

    private void addNamespaces(Map<String, ModelNode> map, ModelNode namespaces) {
        if (namespaces.isDefined()) {
            for (Property prop : namespaces.asPropertyList()) {
                final ModelNode ns = new ModelNode();
                ns.get(prop.getName()).set(prop.getValue());
                map.put(prop.getName(), NamespaceAddHandler.getAddNamespaceOperation(EMPTY, ns));
            }
        }
    }

    private void addServerName(List<ModelNode> updates) {
        updates.add(Util.getWriteAttributeOperation(EMPTY, NAME, serverName));
    }

    private void addSchemaLocations(List<ModelNode> updates) {
        final Map<String, ModelNode> map = new LinkedHashMap<String, ModelNode>();
        addSchemaLocations(map, domainModel.get(SCHEMA_LOCATIONS));
        addSchemaLocations(map, hostModel.get(SCHEMA_LOCATIONS));
        updates.addAll(map.values());
    }

    private void addSchemaLocations(Map<String, ModelNode> map, ModelNode namespaces) {
        if (namespaces.isDefined()) {
            for (Property prop : namespaces.asPropertyList()) {
                final ModelNode sl = new ModelNode();
                sl.get(prop.getName()).set(prop.getValue());
                map.put(prop.getName(), SchemaLocationAddHandler.getAddSchemaLocationOperation(EMPTY, sl));
            }
        }
    }

    private void addExtensions(List<ModelNode> updates) {
        final Set<String> extensionNames = new LinkedHashSet<String>();
        addExtensions(extensionNames, domainModel.get(EXTENSION));
        addExtensions(extensionNames, hostModel.get(EXTENSION));

        for (String name : extensionNames) {
            updates.add(ExtensionAddHandler.getAddExtensionOperation(pathAddress(PathElement.pathElement(EXTENSION, name))));
        }
    }

    private void addExtensions(Set<String> extensionNames, ModelNode extensions) {
        if (extensions.isDefined()) {
            extensionNames.addAll(extensions.keys());
        }
    }

    private void addPaths(List<ModelNode> updates) {
        final Map<String, ModelNode> paths = new LinkedHashMap<String, ModelNode>();
        addPaths(paths, domainModel.get(PATH));
        addPaths(paths, hostModel.get(PATH));

        for (Entry<String, ModelNode> entry : paths.entrySet()) {
            updates.add(PathAddHandler.getAddPathOperation(pathAddress(PathElement.pathElement(PATH, entry.getKey())), entry.getValue().get(PATH), entry.getValue().get(RELATIVE_TO)));
        }
    }

    private void addPaths(Map<String, ModelNode> map, ModelNode path) {
        if (path.isDefined()) {
            for (Property prop : path.asPropertyList()) {
                //TODO merge rather than replace existing?
                map.put(prop.getName(), prop.getValue());
            }
        }
    }

    private void addInterfaces(List<ModelNode> updates) {
        final Map<String, ModelNode> interfaces = new LinkedHashMap<String, ModelNode>();
        addInterfaces(interfaces, domainModel.get(INTERFACE));
        addInterfaces(interfaces, hostModel.get(INTERFACE));

        for (Entry<String, ModelNode> entry : interfaces.entrySet()) {
            updates.add(InterfaceAddHandler.getAddInterfaceOperation(pathAddress(PathElement.pathElement(INTERFACE, entry.getKey())), entry.getValue().get(CRITERIA)));
        }
    }

    private void addInterfaces(Map<String, ModelNode> map, ModelNode iface) {
        if (iface.isDefined()) {
            for (Property prop : iface.asPropertyList()) {
                //TODO merge rather than replace existing?
                map.put(prop.getName(), prop.getValue());
            }
        }
    }

    private void addSocketBindings(List<ModelNode> updates) {
        final Map<String, ModelNode> bindings = new LinkedHashMap<String, ModelNode>();
        addSocketBindings(bindings, domainModel.get(SOCKET_BINDING_GROUP));
        addSocketBindings(bindings, hostModel.get(SOCKET_BINDING_GROUP));

        for (Entry<String, ModelNode> entry : bindings.entrySet()) {
            updates.add(SocketBindingGroupAddHandler.getOperation(pathAddress(PathElement.pathElement(INTERFACE, entry.getKey())), entry.getValue()));
            final ModelNode binding = entry.getValue().get(SOCKET_BINDING);
            if (binding.isDefined()) {
                for (Property bindingEntry : binding.asPropertyList()) {
                    updates.add(SocketBindingAddHandler.getOperation(
                            pathAddress(PathElement.pathElement(INTERFACE, entry.getKey()),
                                    PathElement.pathElement(SOCKET_BINDING, bindingEntry.getName())),
                            bindingEntry.getValue()));
                }
            }
        }
    }

    private void addSocketBindings(Map<String, ModelNode> map, ModelNode binding) {
        if (binding.isDefined()) {
            for (Property prop : binding.asPropertyList()) {
                //TODO merge rather than replace existing?
                map.put(prop.getName(), prop.getValue());
            }
        }
    }

    private ModelNode pathAddress(PathElement...elements) {
        return PathAddress.pathAddress(elements).toModelNode();
    }
}
