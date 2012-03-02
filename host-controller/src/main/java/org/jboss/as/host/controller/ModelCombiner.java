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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHENTICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHORIZATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BOOT_TIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LDAP_CONNECTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL_DESTINATION_OUTBOUND_SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_SUBSYSTEM_ENDPOINT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_IDENTITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT_OPTIONS;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

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
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.extension.ExtensionAddHandler;
import org.jboss.as.controller.operations.common.InterfaceAddHandler;
import org.jboss.as.controller.operations.common.NamespaceAddHandler;
import org.jboss.as.controller.operations.common.PathAddHandler;
import org.jboss.as.controller.operations.common.SchemaLocationAddHandler;
import org.jboss.as.controller.operations.common.SocketBindingAddHandler;
import org.jboss.as.controller.operations.common.SystemPropertyAddHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.host.controller.ManagedServer.ManagedServerBootConfiguration;
import org.jboss.as.process.DefaultJvmUtils;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.server.ServerEnvironment;
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
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
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

    /**
     * Create and verify the configuration before trying to start the process.
     *
     * @return the process boot configuration
     */
    public ManagedServerBootConfiguration createConfiguration() {
        return new ProcessedBootConfiguration(getServerLaunchCommand(), getBootUpdates(),
                 getServerLaunchEnvironment(), isManagementSubsystemEndpoint(), environment);
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
        addProfileName(updates);
        addSchemaLocations(updates);
        addExtensions(updates);
        addPaths(updates);
        addSystemProperties(updates);
        addVault(updates);
        addManagementSecurityRealms(updates);
        addManagementConnections(updates);
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

        command.add("-D[" + ManagedServer.getServerProcessName(serverName) + "]");

        JvmOptionsBuilderFactory.getInstance().addOptions(jvmElement, command);

        //These need to go in on the command-line
        command.add("-D" + RMI_CLIENT_INTERVAL + "=" + SecurityActions.getSystemProperty(RMI_CLIENT_INTERVAL, DEFAULT_RMI_INTERVAL));
        command.add("-D" + RMI_SERVER_INTERVAL + "=" + SecurityActions.getSystemProperty(RMI_SERVER_INTERVAL, DEFAULT_RMI_INTERVAL));

        Map<String, String> bootTimeProperties = getAllSystemProperties(true);
        // Add in properties passed in to the ProcessController command line
        for (Map.Entry<String, String> hostProp : environment.getHostSystemProperties().entrySet()) {
            if (!bootTimeProperties.containsKey(hostProp.getKey())) {
                bootTimeProperties.put(hostProp.getKey(), hostProp.getValue());
            }
        }
        for (Entry<String, String> entry : bootTimeProperties.entrySet()) {
            String property = entry.getKey();
            if (!"org.jboss.boot.log.file".equals(property) && !"logging.configuration".equals(property)) {
                final StringBuilder sb = new StringBuilder("-D");
                sb.append(property);
                sb.append('=');
                sb.append(entry.getValue() == null ? "true" : entry.getValue());
                command.add(sb.toString());
            }
        }
        // Determine the directory grouping type
        final DirectoryGrouping directoryGrouping = DirectoryGrouping.fromModel(hostModel);
        // Write the paths out to the command
        final String logDir = addPathProperty(directoryGrouping, command, bootTimeProperties, ServerEnvironment.SERVER_LOG_DIR, environment.getDomainLogDir(), "log");
        addPathProperty(directoryGrouping, command, bootTimeProperties, ServerEnvironment.SERVER_TEMP_DIR, environment.getDomainTempDir(), "tmp");
        addPathProperty(directoryGrouping, command, bootTimeProperties, ServerEnvironment.SERVER_DATA_DIR, environment.getDomainDataDir(), "data");

        command.add("-Dorg.jboss.boot.log.file=" + getAbsolutePath(new File(logDir), "boot.log"));
        // TODO: make this better
        String loggingConfiguration = System.getProperty("logging.configuration");
        if (loggingConfiguration == null) {
            loggingConfiguration = "file:" + getAbsolutePath(environment.getDomainConfigurationDir(), "logging.properties");
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

    private void addVault(List<ModelNode> updates) {
        if (hostModel.get(CORE_SERVICE).isDefined()) {
            addVault(updates, hostModel.get(CORE_SERVICE).get(VAULT));
        }
    }

    private void addVault(List<ModelNode> updates, ModelNode vaultNode) {
        if (vaultNode.isDefined()) {
            ModelNode vault = new ModelNode();
            ModelNode codeNode = vaultNode.get(Attribute.CODE.getLocalName());
            if (codeNode.isDefined()) {
                vault.get(Attribute.CODE.getLocalName()).set(codeNode.asString());
            }
            vault.get(OP).set(ADD);
            ModelNode vaultAddress = new ModelNode();
            vaultAddress.add(CORE_SERVICE, VAULT);
            vault.get(OP_ADDR).set(vaultAddress);

            ModelNode optionsNode = vaultNode.get(VAULT_OPTIONS);
            if (optionsNode.isDefined()) {
                vault.get(VAULT_OPTIONS).set(optionsNode);
            }
            updates.add(vault);
        }
    }

    private void addManagementSecurityRealms(List<ModelNode> updates) {
        if (hostModel.get(CORE_SERVICE, MANAGEMENT, SECURITY_REALM).isDefined()) {
            ModelNode securityRealms = hostModel.get(CORE_SERVICE, MANAGEMENT, SECURITY_REALM);
            Set<String> keys = securityRealms.keys();
            for (String current : keys) {
                ModelNode addOp = new ModelNode();
                ModelNode realmAddress = new ModelNode();
                realmAddress.add(CORE_SERVICE, MANAGEMENT).add(SECURITY_REALM, current);
                addOp.get(OP).set(ADD);
                addOp.get(OP_ADDR).set(realmAddress);
                updates.add(addOp);

                ModelNode currentRealm = securityRealms.get(current);
                if (currentRealm.hasDefined(SERVER_IDENTITY)) {
                    addManagementComponentComponent(currentRealm, realmAddress, SERVER_IDENTITY, updates);
                }
                if (currentRealm.hasDefined(AUTHENTICATION)) {
                    addManagementComponentComponent(currentRealm, realmAddress, AUTHENTICATION, updates);
                }
                if (currentRealm.hasDefined(AUTHORIZATION)) {
                    addManagementComponentComponent(currentRealm, realmAddress, AUTHORIZATION, updates);
                }
            }
        }
    }

    private void addManagementComponentComponent(ModelNode realm, ModelNode parentAddress, String key, List<ModelNode> updates) {
        for (String currentComponent : realm.get(key).keys()) {
            ModelNode addComponent = new ModelNode();
            // First take the properties to pass over.
            addComponent.set(realm.get(key, currentComponent));

            // Now convert it to an operation by adding a name and address.
            ModelNode identityAddress = parentAddress.clone().add(key, currentComponent);
            addComponent.get(OP).set(ADD);
            addComponent.get(OP_ADDR).set(identityAddress);

            updates.add(addComponent);
        }
    }

    private void addManagementConnections(List<ModelNode> updates) {
        if (hostModel.get(CORE_SERVICE, MANAGEMENT, LDAP_CONNECTION).isDefined()) {
            ModelNode baseAddress = new ModelNode();
            baseAddress.add(CORE_SERVICE, MANAGEMENT);

            ModelNode connections = hostModel.get(CORE_SERVICE, MANAGEMENT, LDAP_CONNECTION);
            for (String connectionName : connections.keys()) {
                ModelNode addConnection = new ModelNode();
                // First take the properties to pass over.
                addConnection.set(connections.get(connectionName));

                // Now convert it to an operation by adding a name and address.
                ModelNode identityAddress = baseAddress.clone().add(LDAP_CONNECTION, connectionName);
                addConnection.get(OP).set(ADD);
                addConnection.get(OP_ADDR).set(identityAddress);

                updates.add(addConnection);
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

            HostFileRepository remoteRepository = null;
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

    /**
     * Adds the absolute path to command.
     *
     * @param directoryGrouping the directory group type.
     * @param command           the command to add the arguments to.
     * @param properties        the properties where the path may already be defined.
     * @param propertyName      the name of the property.
     * @param rootDir           the root directory;
     * @param subDirName        the subdirectory of the path.
     *
     * @return the absolute path that was added.
     */
    private String addPathProperty(final DirectoryGrouping directoryGrouping, final List<String> command, final Map<String, String> properties, final String propertyName, final File rootDir, final String subDirName) {
        final String result;
        final String value = properties.get(propertyName);
        if (value == null) {
            switch (directoryGrouping) {
                case BY_TYPE:
                    result = getAbsolutePath(rootDir, subDirName, "servers", serverName);
                    break;
                case BY_SERVER:
                default:
                    result = getAbsolutePath(rootDir, serverName, subDirName);
                    break;
            }
            properties.put(propertyName, result);
        } else {
            result = new File(value).getAbsolutePath();
        }
        command.add(String.format("-D%s=%s", propertyName, result));
        return result;
    }

    static String getAbsolutePath(final File root, final String... paths) {
        File path = root;
        for(String segment : paths) {
            path = new File(path, segment);
        }
        return path.getAbsolutePath();
    }

    static class ProcessedBootConfiguration implements ManagedServerBootConfiguration {

        List<String> command;
        List<ModelNode> bootUpdates;
        Map<String, String> environment;
        boolean managementSubsystemEndpoint;
        HostControllerEnvironment hostControllerEnvironment;

        ProcessedBootConfiguration(List<String> command, List<ModelNode> bootUpdates, Map<String, String> environment,
                                   boolean managementSubsystemEndpoint, HostControllerEnvironment hostControllerEnvironment) {
            this.command = command;
            this.bootUpdates = bootUpdates;
            this.environment = environment;
            this.managementSubsystemEndpoint = managementSubsystemEndpoint;
            this.hostControllerEnvironment = hostControllerEnvironment;
        }

        @Override
        public List<ModelNode> getBootUpdates() {
            return bootUpdates;
        }

        @Override
        public Map<String, String> getServerLaunchEnvironment() {
            return environment;
        }

        @Override
        public List<String> getServerLaunchCommand() {
            return command;
        }

        @Override
        public HostControllerEnvironment getHostControllerEnvironment() {
            return hostControllerEnvironment;
        }

        @Override
        public boolean isManagementSubsystemEndpoint() {
            return managementSubsystemEndpoint;
        }
    }

}
