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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BOOT_TIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL_DESTINATION_OUTBOUND_SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_SUBSYSTEM_ENDPOINT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import java.io.File;
import java.lang.management.ManagementFactory;
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
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.extension.ExtensionAddHandler;
import org.jboss.as.controller.operations.common.InterfaceAddHandler;
import org.jboss.as.controller.operations.common.NamespaceAddHandler;
import org.jboss.as.controller.operations.common.PathAddHandler;
import org.jboss.as.controller.operations.common.SchemaLocationAddHandler;
import org.jboss.as.controller.operations.common.SocketBindingAddHandler;
import org.jboss.as.controller.operations.common.SystemPropertyAddHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.host.controller.ManagedServer.ManagedServerBootConfiguration;
import org.jboss.as.process.DefaultJvmUtils;
import org.jboss.as.server.jmx.PluggableMBeanServer;
import org.jboss.as.server.services.net.BindingGroupAddHandler;
import org.jboss.as.server.services.net.LocalDestinationOutboundSocketBindingAddHandler;
import org.jboss.as.server.services.net.RemoteDestinationOutboundSocketBindingAddHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Combines the relevant parts of the domain-level and host-level models to
 * determine the jvm launch command and boot-time updates needed to start
 * an application server instance.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class ModelCombiner implements ManagedServerBootConfiguration {

    private static final String RMI_CLIENT_INTERVAL = "sun.rmi.dgc.client.gcInterval";
    private static final String RMI_SERVER_INTERVAL = "sun.rmi.dgc.server.gcInterval";
    private static final String DEFAULT_RMI_INTERVAL = "3600000";

    private static final ModelNode EMPTY = new ModelNode();
    static {
        EMPTY.setEmptyList();
        EMPTY.protect();
    }

    private final String serverName;
    private final ModelNode domainModel;
    private final ModelNode hostModel;
    private final ModelNode serverModel;
    private final ModelNode serverGroup;
    private final String profileName;
    private final JvmElement jvmElement;
    private final HostControllerEnvironment environment;
    private final DomainController domainController;
    private final boolean managementSubsystemEndpoint;

    ModelCombiner(final String serverName, final ModelNode domainModel, final ModelNode hostModel, final DomainController domainController,
                  final HostControllerEnvironment environment) {
        this.serverName = serverName;
        this.domainModel = domainModel;
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

        boolean managementSubsystemEndpoint = false;
        if (serverGroup.hasDefined(MANAGEMENT_SUBSYSTEM_ENDPOINT)) {
            managementSubsystemEndpoint = serverGroup.get(MANAGEMENT_SUBSYSTEM_ENDPOINT).asBoolean();
        }
        this.managementSubsystemEndpoint = managementSubsystemEndpoint;

        final String jvmName = serverVMName != null ? serverVMName : groupVMName;
        final ModelNode hostVM = jvmName != null ? hostModel.get(JVM, jvmName) : null;
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
            throw MESSAGES.undefinedSocketBinding(serverName);
        }

        List<ModelNode> updates = new ArrayList<ModelNode>();

        addNamespaces(updates);
        addServerName(updates);
        addProfileName(updates);
        addSchemaLocations(updates);
        addExtensions(updates);
        addPaths(updates);
        addSystemProperties(updates);
        addInterfaces(updates);
        addSocketBindings(updates, portOffSet, socketBindingRef);
        addSubsystems(updates);
        addDeployments(updates);

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

        //These need to go in on the command-line
        command.add("-D" + RMI_CLIENT_INTERVAL + "=" + SecurityActions.getSystemProperty(RMI_CLIENT_INTERVAL, DEFAULT_RMI_INTERVAL));
        command.add("-D" + RMI_SERVER_INTERVAL + "=" + SecurityActions.getSystemProperty(RMI_SERVER_INTERVAL, DEFAULT_RMI_INTERVAL));

        Map<String, String> bootTimeProperties = getAllSystemProperties(true);
        // Add in properties passed in to the ProcessController command line
        bootTimeProperties.putAll(environment.getHostSystemProperties());
        for (Entry<String, String> entry : bootTimeProperties.entrySet()) {
            final StringBuilder sb = new StringBuilder("-D");
            sb.append(entry.getKey());
            sb.append('=');
            sb.append(entry.getValue() == null ? "true" : entry.getValue());
            command.add(sb.toString());
        }

        command.add("-Dorg.jboss.boot.log.file=" + environment.getDomainBaseDir().getAbsolutePath() + "/servers/" + serverName + "/log/boot.log");
        // TODO: make this better
        String loggingConfiguration = System.getProperty("logging.configuration");
        if (loggingConfiguration == null) {
            loggingConfiguration = "file:" + environment.getDomainConfigurationDir().getAbsolutePath() + "/logging.properties";
        }
        command.add("-Dlogging.configuration=" + loggingConfiguration);
        command.add("-jar");
        command.add("jboss-modules.jar");
        command.add("-mp");
        command.add("modules");
        command.add("-jaxpmodule");
        command.add("javax.xml.jaxp-provider");
        command.add("org.jboss.as.server");

        return command;
    }

    @Override
    public boolean isManagementSubsystemEndpoint() {
        return managementSubsystemEndpoint;
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
                map.put(prop.getName(), NamespaceAddHandler.getAddNamespaceOperation(EMPTY, prop.getName(), prop.getValue().asString()));
            }
        }
    }

    private void addServerName(List<ModelNode> updates) {
        updates.add(Util.getWriteAttributeOperation(EMPTY, NAME, serverName));
    }

    private void addProfileName(List<ModelNode> updates) {
        updates.add(Util.getWriteAttributeOperation(EMPTY, PROFILE_NAME, profileName));
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
                map.put(prop.getName(), SchemaLocationAddHandler.getAddSchemaLocationOperation(EMPTY, prop.getName(), prop.getValue().asString()));
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
        addPaths(paths, serverModel.get(PATH));

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

    private void addSystemProperties(List<ModelNode> updates) {
        Map<String, String> props = getAllSystemProperties(false);

        for (Entry<String, String> entry : props.entrySet()) {
            ModelNode address = new ModelNode();
            address.add(SYSTEM_PROPERTY, entry.getKey());
            ModelNode op = Util.getEmptyOperation(SystemPropertyAddHandler.OPERATION_NAME, address);
            if (entry.getValue() != null) {
                op.get(VALUE).set(entry.getValue());
            }
            updates.add(op);
        }
    }

    private Map<String, String> getAllSystemProperties(boolean boottimeOnly){
        Map<String, String> props = new HashMap<String, String>();

        addSystemProperties(domainModel, props, boottimeOnly);
        addSystemProperties(serverGroup, props, boottimeOnly);
        addSystemProperties(hostModel, props, boottimeOnly);
        addSystemProperties(serverModel, props, boottimeOnly);

        return props;
    }

    private void addSystemProperties(final ModelNode source, final Map<String, String> props, boolean boottimeOnly) {
        if (source.hasDefined(SYSTEM_PROPERTY)) {
            for (Property prop : source.get(SYSTEM_PROPERTY).asPropertyList()) {
                ModelNode propResource = prop.getValue();
                if (boottimeOnly && !propResource.get(BOOT_TIME).asBoolean()) {
                    continue;
                }
                String val = propResource.hasDefined(VALUE) ? propResource.get(VALUE).asString() : null;
                props.put(prop.getName(), val);
            }
        }
    }

    private void addInterfaces(List<ModelNode> updates) {
        final Map<String, ModelNode> interfaces = new LinkedHashMap<String, ModelNode>();
        addInterfaces(interfaces, domainModel.get(INTERFACE));
        addInterfaces(interfaces, hostModel.get(INTERFACE));
        addInterfaces(interfaces, hostModel.get(SERVER_CONFIG, serverName, INTERFACE));

        for (Entry<String, ModelNode> entry : interfaces.entrySet()) {
            updates.add(InterfaceAddHandler.getAddInterfaceOperation(pathAddress(PathElement.pathElement(INTERFACE, entry.getKey())), entry.getValue()));
        }
    }

    private void addInterfaces(Map<String, ModelNode> map, ModelNode iface) {
        if (iface.isDefined()) {
            for (Property prop : iface.asPropertyList()) {
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
            throw MESSAGES.undefinedSocketBindingGroup(bindingRef);
        }
        final ModelNode groupAddress = pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, bindingRef));
        final ModelNode groupAdd = BindingGroupAddHandler.getOperation(groupAddress, group);
        groupAdd.get(PORT_OFFSET).set(portOffSet);
        updates.add(groupAdd);
        mergeBindingGroups(updates, groups, bindingRef, group, processed);
    }

    private void mergeBindingGroups(List<ModelNode> updates, Map<String, ModelNode> groups, final String groupName, ModelNode group, Set<String> processed) {
        addSocketBindings(updates, group, groupName, group.get(DEFAULT_INTERFACE));
        if(group.has(INCLUDES) && group.get(INCLUDES).isDefined()) {
            for(final ModelNode include : group.get(INCLUDES).asList()) {
                final String ref = include.asString();
                if(processed.add(ref)) {
                    final ModelNode includedGroup = groups.get(ref);
                    if(group == null) {
                        throw MESSAGES.undefinedSocketBindingGroup(ref);
                    }
                    mergeBindingGroups(updates, groups, groupName, includedGroup, processed);
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
        // outbound-socket-binding (for local destination)
        if (group.hasDefined(LOCAL_DESTINATION_OUTBOUND_SOCKET_BINDING)) {
            for(final Property localDestinationOutboundSocketBindings : group.get(LOCAL_DESTINATION_OUTBOUND_SOCKET_BINDING).asPropertyList()) {
                final String outboundSocketBindingName = localDestinationOutboundSocketBindings.getName();
                final ModelNode binding = localDestinationOutboundSocketBindings.getValue();
                if(! binding.isDefined()) {
                    continue;
                }
                // add the local destination outbound socket binding add operation
                updates.add(LocalDestinationOutboundSocketBindingAddHandler.getOperation(pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, groupName),
                        PathElement.pathElement(LOCAL_DESTINATION_OUTBOUND_SOCKET_BINDING, outboundSocketBindingName)), binding));
            }
        }
        // outbound-socket-binding (for remote destination)
        if (group.hasDefined(REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING)) {
            for(final Property remoteDestinationOutboundSocketBindings : group.get(REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING).asPropertyList()) {
                final String outboundSocketBindingName = remoteDestinationOutboundSocketBindings.getName();
                final ModelNode binding = remoteDestinationOutboundSocketBindings.getValue();
                if(! binding.isDefined()) {
                    continue;
                }
                // add the local destination outbound socket binding add operation
                updates.add(RemoteDestinationOutboundSocketBindingAddHandler.getOperation(pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, groupName),
                        PathElement.pathElement(ModelDescriptionConstants.REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING, outboundSocketBindingName)), binding));
            }
        }
    }

    private void addSubsystems(List<ModelNode> updates) {
        ModelNode node = domainController.getProfileOperations(profileName);
        updates.addAll(node.asList());
    }

    private void addDeployments(List<ModelNode> updates) {
        if (serverGroup.hasDefined(DEPLOYMENT)) {

            FileRepository remoteRepository = null;
            if (! domainController.getLocalHostInfo().isMasterDomainController()) {
                remoteRepository = domainController.getRemoteFileRepository();
            }

            for (Property deployment : serverGroup.get(DEPLOYMENT).asPropertyList()) {
                String name = deployment.getName();
                ModelNode details = deployment.getValue();

                ModelNode domainDeployment = domainModel.require(DEPLOYMENT).require(name);
                ModelNode deploymentContent = domainDeployment.require(CONTENT).clone();

                if (remoteRepository != null) {
                    // Make sure we have a copy of the deployment in the local repo
                    for (ModelNode content : deploymentContent.asList()) {
                        if ((content.hasDefined(HASH))) {
                            byte[] hash = content.require(HASH).asBytes();
                            File[] files = domainController.getLocalFileRepository().getDeploymentFiles(hash);
                            if (files == null || files.length == 0) {
                                remoteRepository.getDeploymentFiles(hash);
                            }
                        }
                    }
                }

                PathAddress addr = PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT, name));
                ModelNode addOp = Util.getEmptyOperation(ADD, addr.toModelNode());
                addOp.get(RUNTIME_NAME).set(details.get(RUNTIME_NAME));
                addOp.get(CONTENT).set(deploymentContent);
                addOp.get(ENABLED).set(!details.hasDefined(ENABLED) || details.get(ENABLED).asBoolean());

                updates.add(addOp);
            }
        }
    }

    private ModelNode pathAddress(PathElement...elements) {
        return PathAddress.pathAddress(elements).toModelNode();
    }
}
