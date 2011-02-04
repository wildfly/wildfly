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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CRITERIA;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE;
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import org.jboss.as.host.controller.NewManagedServer.ManagedServerBootConfiguration;
import org.jboss.as.host.controller.operations.ExtensionAddHandler;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.operations.SocketBindingGroupAddHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
class ModelCombiner implements ManagedServerBootConfiguration {

    private static final ModelNode EMPTY = new ModelNode();
    static {
        EMPTY.setEmptyList();
        EMPTY.protect();
    }

    final int portOffSet;
    final String serverName;
    final ModelNode domainModel;
    final ModelNode hostModel;
    final ModelNode serverModel;
    final String profileName;
    final HostControllerEnvironment environment;
    final NewDomainControllerConnection domainControllerConnection;

    ModelCombiner(final String serverName, final ModelNode domainModel, final ModelNode hostModel, final HostControllerEnvironment environment, final NewDomainControllerConnection domainControllerConnection) {
        this.serverName = serverName;
        this.domainModel = domainModel;
        this.hostModel = hostModel;
        this.serverModel = hostModel.require(SERVER).require(serverName);
        this.domainControllerConnection = domainControllerConnection;
        this.environment = environment;

        String serverGroupName = serverModel.require(GROUP).asString();
        this.profileName = domainModel.require(SERVER_GROUP).require(serverGroupName).require(PROFILE).asString();
        this.portOffSet = serverModel.get(SOCKET_BINDING_PORT_OFFSET).asInt();
    }

    public List<ModelNode> getBootUpdates() {

        //System.out.println("DOMAIN");
        //System.out.println(domainModel);
        //System.out.println("HOST");
        //System.out.println(hostModel);

        final String socketBindingRef = serverModel.get(SOCKET_BINDING_GROUP).asString();

        List<ModelNode> updates = new ArrayList<ModelNode>();
        addNamespaces(updates);
        addServerName(updates);
        addSchemaLocations(updates);
        addExtensions(updates);
        addPaths(updates);
        addInterfaces(updates);
        addSocketBindings(updates, socketBindingRef);
        //TODO I think it is ok to not do
        //  management
        //  domain-controller

        addSubsystems(updates);
        //TODO deployments

        //System.out.println(updates);

        return updates;
    }

    /** {@inheritDoc} */
    public HostControllerEnvironment getHostControllerEnvironment() {
        return environment;
    }

    /** {@inheritDoc} */
    public int getPortOffSet() {
        return portOffSet;
    }

    /** {@inheritDoc} */
    public List<String> getServerLaunchCommand() {
        final List<String> command = new ArrayList<String>();

        command.add(environment.getDefaultJVM().toString());
        command.add("-Dorg.jboss.boot.log.file=domain/servers/" + serverName + "/log/boot.log");
        // TODO: make this better
        command.add("-Dlogging.configuration=file:" + new File("").getAbsolutePath() + "/domain/configuration/logging.properties");
        command.add("-jar");
        command.add("jboss-modules.jar");
        command.add("-mp");
        command.add("modules");
        command.add("-logmodule");
        command.add("org.jboss.logmanager");
        command.add("org.jboss.as.server");

        return command;
    }

    /** {@inheritDoc} */
    public Map<String, String> getServerLaunchEnvironment() {
        final Map<String, String> env = new HashMap<String, String>();
        addStandardProperties(serverName, environment, env);
        return env;
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

    static final String SOCKET_BINDING_GROUP_NAME = "default";

    private void addSocketBindings(List<ModelNode> updates, String bindingRef) {
        final Set<String> processed = new HashSet<String>();
        final Map<String, ModelNode> groups = new LinkedHashMap<String, ModelNode>();
        if(domainModel.hasDefined(SOCKET_BINDING_GROUP)) {
            for (Property prop : domainModel.get(SOCKET_BINDING_GROUP).asPropertyList()) {
                groups.put(prop.getName(), prop.getValue());
            }
        }
        final ModelNode group = groups.get(bindingRef);
        final ModelNode groupAddress = pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, SOCKET_BINDING_GROUP_NAME));
        updates.add(SocketBindingGroupAddHandler.getOperation(groupAddress, group));
        mergeBindingGroups(updates, groups, group, processed, group.get(INTERFACE));
    }

    private void mergeBindingGroups(List<ModelNode> updates, Map<String, ModelNode> groups, ModelNode group, Set<String> processed, ModelNode parentInterface) {
        addSocketBindings(updates, group, group.get(DEFAULT_INTERFACE));
        if(group.has(INCLUDE) && group.get(INCLUDE).isDefined()) {
            for(final ModelNode include : group.get(INCLUDE).asList()) {
                final String ref = include.asString();
                if(processed.add(ref)) {
                    final ModelNode includedGroup = groups.get(ref);
                    final ModelNode defaultInterface = group.hasDefined(INTERFACE) ? group.get(INTERFACE) : parentInterface;
                    addSocketBindings(updates, includedGroup, defaultInterface);
                }
            }
        }
    }

    private void addSocketBindings(List<ModelNode> updates, ModelNode group, ModelNode defaultInterface) {
        for(final Property socketBinding : group.get(SOCKET_BINDING).asPropertyList()) {
            final String name = socketBinding.getName();
            final ModelNode binding = socketBinding.getValue();
            if(! binding.isDefined()) {
                continue;
            }
            if(!binding.get(DEFAULT_INTERFACE).isDefined()) {
                binding.get(DEFAULT_INTERFACE).set(defaultInterface);
            }
            updates.add(SocketBindingAddHandler.getOperation(pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, SOCKET_BINDING_GROUP_NAME),
                    PathElement.pathElement(SOCKET_BINDING, name)), binding));
        }
    }

    private void addSubsystems(List<ModelNode> updates) {
        ModelNode node = domainControllerConnection.getProfileOperations(profileName);
        updates.addAll(node.asList());
    }

    private ModelNode pathAddress(PathElement...elements) {
        return PathAddress.pathAddress(elements).toModelNode();
    }


    /**
     * Equivalent to default JAVA_OPTS in < AS 7 run.conf file
     *
     * TODO externalize this somewhere if doing this at all is the right thing
     *
     * @param sysProps
     */
    static void addStandardProperties(final String serverName, final HostControllerEnvironment environment, Map<String, String> sysProps) {
        //
        if (!sysProps.containsKey("sun.rmi.dgc.client.gcInterval")) {
            sysProps.put("sun.rmi.dgc.client.gcInterval","3600000");
        }
        if (!sysProps.containsKey("sun.rmi.dgc.server.gcInterval")) {
            sysProps.put("sun.rmi.dgc.server.gcInterval","3600000");
        }

        sysProps.put(HostControllerEnvironment.HOME_DIR, environment.getHomeDir().getAbsolutePath());
        String key = ServerEnvironment.SERVER_BASE_DIR;
        if (sysProps.get(key) == null) {
            File serverBaseDir = new File(environment.getDomainServersDir(), serverName);
            sysProps.put(key, serverBaseDir.getAbsolutePath());
        }
        // Servers should use the host controller's deployment content repo
        key = ServerEnvironment.SERVER_DEPLOY_DIR;
        if (sysProps.get(key) == null) {
            File serverDeploymentDir = environment.getDomainDeploymentDir();
            sysProps.put(key, serverDeploymentDir.getAbsolutePath());
        }

        key = ServerEnvironment.SERVER_SYSTEM_DEPLOY_DIR;
        if (sysProps.get(key) == null) {
            File serverDeploymentDir = environment.getDomainSystemDeploymentDir();
            sysProps.put(key, serverDeploymentDir.getAbsolutePath());
        }
    }
}
