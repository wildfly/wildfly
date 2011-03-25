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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
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
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.host.controller.ManagedServer.ManagedServerBootConfiguration;
import org.jboss.as.host.controller.operations.ExtensionAddHandler;
import org.jboss.as.process.DefaultJvmUtils;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.operations.sockets.BindingGroupAddHandler;
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

    final String serverName;
    final ModelNode domainModel;
    final ModelNode hostModel;
    final ModelNode serverModel;
    final ModelNode serverGroup;
    final String profileName;
    final JvmElement jvmElement;
    final HostControllerEnvironment environment;
    final DomainController domainController;

    ModelCombiner(final String serverName, final ModelNode hostModel, final DomainController domainController, final HostControllerEnvironment environment) {
        this.serverName = serverName;
        this.domainModel = domainController.getDomainModel();
        this.hostModel = hostModel;
        this.serverModel = hostModel.require(SERVER_CONFIG).require(serverName);
        this.domainController = domainController;
        this.environment = environment;

        final String serverGroupName = serverModel.require(GROUP).asString();
        this.serverGroup = domainModel.require(SERVER_GROUP).require(serverGroupName);
        this.profileName = serverGroup.require(PROFILE).asString();

        String serverVMName = null;
        ModelNode serverVM = null;
        if(serverModel.hasDefined(JVM)) {
            for(final String jvm : serverModel.get(JVM).keys()) {
                serverVMName = jvm;
                serverVM = serverModel.get(JVM, jvm);
                break;
            }
        }
        String groupVMName = null;
        ModelNode groupVM = null;
        if(serverGroup.hasDefined(JVM)) {
            for(final String jvm : serverGroup.get(JVM).keys()) {
                groupVMName = jvm;
                groupVM = serverGroup.get(JVM, jvm);
                break;
            }
        }

        final String jvmName = serverVMName != null ? serverVMName : groupVMName;
        if(jvmName == null) {
            throw new IllegalStateException("Neither " + Element.SERVER_GROUP.getLocalName() +
                    " nor " + Element.SERVER.getLocalName() + " has declared a JVM configuration; one or the other must");
        }

        final ModelNode hostVM = hostModel.get(JVM, jvmName);
        this.jvmElement = new JvmElement(jvmName, hostVM, groupVM, serverVM);
    }

    @Override
    public List<ModelNode> getBootUpdates() {

        int portOffSet = 0;
        String socketBindingRef = null;

        if(serverGroup.hasDefined(SOCKET_BINDING_GROUP)) {
            socketBindingRef = serverGroup.get(SOCKET_BINDING_GROUP).asString();
        }
        if(serverModel.hasDefined(SOCKET_BINDING_GROUP)) {
            socketBindingRef = serverModel.get(SOCKET_BINDING_GROUP).asString();
        }
        if(serverGroup.hasDefined(SOCKET_BINDING_PORT_OFFSET)) {
            portOffSet = serverGroup.get(SOCKET_BINDING_PORT_OFFSET).asInt();
        }
        if(serverModel.hasDefined(SOCKET_BINDING_PORT_OFFSET)) {
            portOffSet = serverModel.get(SOCKET_BINDING_PORT_OFFSET).asInt();
        }
        if(socketBindingRef == null) {
            throw new IllegalArgumentException("undefined socket binding group for server " + serverName);
        }

        List<ModelNode> updates = new ArrayList<ModelNode>();
        addNamespaces(updates);
        addServerName(updates);
        addSchemaLocations(updates);
        addExtensions(updates);
        addPaths(updates);
        addInterfaces(updates);
        addSocketBindings(updates, portOffSet, socketBindingRef);

        addSubsystems(updates);
        //TODO deployments
        // TODO  system properties
        return updates;
    }

    /** {@inheritDoc} */
    @Override
    public HostControllerEnvironment getHostControllerEnvironment() {
        return environment;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getServerLaunchCommand() {
        final List<String> command = new ArrayList<String>();

        command.add(getJavaCommand());

        JvmOptionsBuilderFactory.getInstance().addOptions(jvmElement, command);

        for(Entry<String, String> property : jvmElement.getSystemProperties().entrySet()) {
            final StringBuilder sb = new StringBuilder("-D");
            sb.append(property.getKey());
            sb.append('=');
            sb.append(property.getValue() == null ? "true" : property.getValue());
            command.add(sb.toString());
        }

        command.add("-Dorg.jboss.boot.log.file=domain/servers/" + serverName + "/log/boot.log");
        // TODO: make this better
        command.add("-Dlogging.configuration=file:" + new File("").getAbsolutePath() + "/domain/configuration/logging.properties");
        command.add("-jar");
        command.add("jboss-modules.jar");
        command.add("-mp");
        command.add("modules");
        command.add("-logmodule");
        command.add("org.jboss.logmanager");
        command.add("-jaxpmodule");
        command.add("javax.xml.jaxp-provider");
        command.add("org.jboss.as.server");

        return command;
    }

    private String getJavaCommand() {
        String javaHome = jvmElement.getJavaHome();
        if (javaHome == null) {
            if(environment.getDefaultJVM() != null) {
                String defaultJvm = environment.getDefaultJVM().getAbsolutePath();
                if (!defaultJvm.equals("java") || (defaultJvm.equals("java") && System.getenv("JAVA_HOME") != null)) {
                    return defaultJvm;
                }
            }
            javaHome = DefaultJvmUtils.getCurrentJvmHome();
        }

        return DefaultJvmUtils.findJavaExecutable(javaHome);
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getServerLaunchEnvironment() {
        final Map<String, String> env = new HashMap<String, String>();
        addStandardProperties(serverName, environment, env);
        for(final Entry<String, String> property : jvmElement.getEnvironmentVariables().entrySet()) {
            env.put(property.getKey(), property.getValue());
        }
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
                map.put(prop.getName(), NamespaceAddHandler.getAddNamespaceOperation(EMPTY, prop));
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
                map.put(prop.getName(), SchemaLocationAddHandler.getAddSchemaLocationOperation(EMPTY, prop));
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

    private void addSocketBindings(List<ModelNode> updates, int portOffSet, String bindingRef) {
        final Set<String> processed = new HashSet<String>();
        final Map<String, ModelNode> groups = new LinkedHashMap<String, ModelNode>();
        if(domainModel.hasDefined(SOCKET_BINDING_GROUP)) {
            for (Property prop : domainModel.get(SOCKET_BINDING_GROUP).asPropertyList()) {
                ModelNode node = prop.getValue().clone();
                if (portOffSet > 0) {
                    node.get(PORT_OFFSET).set(portOffSet);
                }
                groups.put(prop.getName(), node);
            }
        }
        final ModelNode group = groups.get(bindingRef);
        if(group == null) {
            throw new IllegalArgumentException("undefined socket binding group " + bindingRef);
        }
        final ModelNode groupAddress = pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, bindingRef));
        final ModelNode groupAdd = BindingGroupAddHandler.getOperation(groupAddress, group);
        groupAdd.get(PORT_OFFSET).set(portOffSet);
        updates.add(groupAdd);
        mergeBindingGroups(updates, groups, bindingRef, group, processed, group.get(INTERFACE));
    }

    private void mergeBindingGroups(List<ModelNode> updates, Map<String, ModelNode> groups, final String groupName, ModelNode group, Set<String> processed, ModelNode parentInterface) {
        addSocketBindings(updates, group, groupName, group.get(DEFAULT_INTERFACE));
        if(group.has(INCLUDE) && group.get(INCLUDE).isDefined()) {
            for(final ModelNode include : group.get(INCLUDE).asList()) {
                final String ref = include.asString();
                if(processed.add(ref)) {
                    final ModelNode includedGroup = groups.get(ref);
                    final ModelNode defaultInterface = group.hasDefined(INTERFACE) ? group.get(INTERFACE) : parentInterface;
                    addSocketBindings(updates, includedGroup, groupName, defaultInterface);
                }
            }
        }
    }

    private void addSocketBindings(List<ModelNode> updates, ModelNode group, final String groupName, ModelNode defaultInterface) {
        for(final Property socketBinding : group.get(SOCKET_BINDING).asPropertyList()) {
            final String name = socketBinding.getName();
            final ModelNode binding = socketBinding.getValue();
            if(! binding.isDefined()) {
                continue;
            }
            if(!binding.get(DEFAULT_INTERFACE).isDefined()) {
                binding.get(DEFAULT_INTERFACE).set(defaultInterface);
            }
            updates.add(SocketBindingAddHandler.getOperation(pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, groupName),
                    PathElement.pathElement(SOCKET_BINDING, name)), binding));
        }
    }

    private void addSubsystems(List<ModelNode> updates) {
        ModelNode node = domainController.getProfileOperations(profileName);
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
