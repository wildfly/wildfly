/**
 *
 */
package org.jboss.as.domain.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTO_START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CRITERIA;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.SystemPropertyAddHandler;
import org.jboss.as.controller.operations.common.SystemPropertyRemoveHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentFullReplaceHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Logic for creating a server-level operation that realizes the effect
 * of a domain or host level change on the server.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ServerOperationResolver {

    private enum DomainKey {

        UNKNOWN(null),
        EXTENSION("extension"),
        PATH("path"),
        PROFILE("profile"),
        INTERFACE("interface"),
        SOCKET_BINDING_GROUP("socket-binding-group"),
        DEPLOYMENT("deployment"),
        SERVER_GROUP("server-group"),
        HOST("host");

        private final String name;

        DomainKey(final String name) {
            this.name = name;
        }

        private static final Map<String, DomainKey> MAP;

        static {
            final Map<String, DomainKey> map = new HashMap<String, DomainKey>();
            for (DomainKey element : values()) {
                final String name = element.name;
                if (name != null) map.put(name, element);
            }
            MAP = map;
        }

        public static DomainKey forName(String localName) {
            final DomainKey element = MAP.get(localName);
            return element == null ? UNKNOWN : element;
        }

    }

    private enum HostKey {

        UNKNOWN(null),
        PATH("path"),
        MANAGEMENT("management"),
        INTERFACE("interface"),
        JVM("jvm"),
        SERVER("server"),
        RUNNING_SERVER("running-server");

        private final String name;

        HostKey(final String name) {
            this.name = name;
        }

        private static final Map<String, HostKey> MAP;

        static {
            final Map<String, HostKey> map = new HashMap<String, HostKey>();
            for (HostKey element : values()) {
                final String name = element.name;
                if (name != null) map.put(name, element);
            }
            MAP = map;
        }

        public static HostKey forName(String localName) {
            final HostKey element = MAP.get(localName);
            return element == null ? UNKNOWN : element;
        }

    }

    private final String localHostName;

    ServerOperationResolver(final String localHostName) {
        this.localHostName = localHostName;
    }

    Map<Set<ServerIdentity>, ModelNode> getServerOperations(ModelNode operation, PathAddress address, ModelNode domain, ModelNode host) {

        if (address.size() == 0) {
            return resolveDomainRootOperation(operation, domain, host);
        }
        else {
            DomainKey domainKey = DomainKey.forName(address.getElement(0).getKey());
            switch (domainKey) {
                case EXTENSION: {
                    Set<ServerIdentity> allServers = getAllRunningServers(host);
                    return Collections.singletonMap(allServers, operation);
                }
                case DEPLOYMENT: {
                    return Collections.emptyMap();
                }
                case PATH: {
                    return getServerPathOperations(operation, address, host, true);
                }
                case PROFILE: {
                    return getServerProfileOperations(operation, address, domain, host);
                }
                case INTERFACE: {
                    return getServerInterfaceOperations(operation, address, host, true);
                }
                case SOCKET_BINDING_GROUP: {
                    return getServerSocketBindingGroupOperations(operation, address, domain, host);
                }
                case SERVER_GROUP: {
                    return getServerGroupOperations(operation, address, domain, host);
                }
                case HOST: {
                    return getServerHostOperations(operation, address, domain, host);
                }
                default:
                    throw new IllegalStateException(String.format("Unexpected initial path key %s", address.getElement(0).getKey()));
            }
        }
    }

    private Set<ServerIdentity> getAllRunningServers(ModelNode hostModel) {
        return getServersForGroup(null, hostModel);
    }

    private Set<ServerIdentity> getServersForGroup(String groupName, ModelNode hostModel) {
        Set<ServerIdentity> result;
        if (hostModel.hasDefined(SERVER_CONFIG)) {
            result = new HashSet<ServerIdentity>();
            for (Property prop : hostModel.get(SERVER_CONFIG).asPropertyList()) {
                ModelNode server = prop.getValue();

                String serverGroupName = server.require(GROUP).asString();
                if (groupName != null && !groupName.equals(serverGroupName)) {
                    continue;
                }

                if (server.hasDefined(AUTO_START) && !server.get(AUTO_START).asBoolean()) {
                    continue;
                }

                ServerIdentity groupedServer = new ServerIdentity(localHostName, serverGroupName, prop.getName());
                result.add(groupedServer);
            }
        }
        else {
            result = Collections.emptySet();
        }
        return result;
    }

    private Set<ServerIdentity> getServersForType(String type, String ref, ModelNode domainModel, ModelNode hostModel) {
        Set<String> groups = getGroupsForType(type, ref, domainModel);
        Set<ServerIdentity> allServers = new HashSet<ServerIdentity>();
        for (String group : groups) {
            allServers.addAll(getServersForGroup(group, hostModel));
        }
        return allServers;
    }

    private Set<String> getGroupsForType(String type, String ref, ModelNode domainModel) {
        Set<String> groups;
        if (domainModel.hasDefined(SERVER_GROUP)) {
            groups = new HashSet<String>();
            for (Property prop : domainModel.get(SERVER_GROUP).asPropertyList()) {
                ModelNode serverGroup = prop.getValue();
                if (ref.equals(serverGroup.get(type).asString())) {
                    groups.add(prop.getName());
                }
            }
        }
        else {
            groups = Collections.emptySet();
        }
        return groups;
    }

    private Map<Set<ServerIdentity>, ModelNode> getServerProfileOperations(ModelNode operation, PathAddress address,
            ModelNode domain, ModelNode host) {
        if (address.size() == 1) {
            return Collections.emptyMap();
        }
        String profileName = address.getElement(0).getValue();
        Set<String> relatedProfiles = getRelatedElements(PROFILE, profileName, domain);
        Set<ServerIdentity> allServers = new HashSet<ServerIdentity>();
        for (String profile : relatedProfiles) {
            allServers.addAll(getServersForType(PROFILE, profile, domain, host));
        }
        ModelNode serverOp = operation.clone();
        PathAddress serverAddress = address.subAddress(1);
        serverOp.get(OP_ADDR).set(serverAddress.toModelNode());
        return Collections.singletonMap(allServers, serverOp);
    }
    private Map<Set<ServerIdentity>, ModelNode> getServerInterfaceOperations(ModelNode operation, PathAddress address,
            ModelNode hostModel, boolean forDomain) {
        String pathName = address.getElement(0).getValue();
        Map<Set<ServerIdentity>, ModelNode> result;
        if (forDomain && hostModel.hasDefined(INTERFACE) && hostModel.get(INTERFACE).keys().contains(pathName)) {
            // Host will take precedence; ignore the domain
            result = Collections.emptyMap();
        } else if (ADD.equals(operation.get(OP).asString()) && ! operation.has(CRITERIA)) {
            // don't create named interfaces
            result = Collections.emptyMap();
        } else if (hostModel.hasDefined(SERVER_CONFIG)) {
            Set<ServerIdentity> servers = new HashSet<ServerIdentity>();
            for (Property prop : hostModel.get(SERVER_CONFIG).asPropertyList()) {
                ModelNode server = prop.getValue();

                String serverGroupName = server.require(GROUP).asString();

                if (server.hasDefined(INTERFACE) && server.get(INTERFACE).keys().contains(pathName)) {
                    // Server takes precedence; ignore domain
                    continue;
                }

                if (server.hasDefined(AUTO_START) && !server.get(AUTO_START).asBoolean()) {
                    continue;
                }

                ServerIdentity groupedServer = new ServerIdentity(localHostName, serverGroupName, prop.getName());
                servers.add(groupedServer);
            }

            ModelNode serverOp = operation.clone();
            serverOp.get(OP_ADDR).setEmptyList().add(INTERFACE, pathName);
            result = Collections.singletonMap(servers, serverOp);
        }
        else {
            result = Collections.emptyMap();
        }
        return result;
    }

    private Map<Set<ServerIdentity>, ModelNode> getServerPathOperations(ModelNode operation, PathAddress address,
            ModelNode hostModel, boolean forDomain) {
        String pathName = address.getElement(0).getValue();
        Map<Set<ServerIdentity>, ModelNode> result;
        if (forDomain && hostModel.hasDefined(PATH) && hostModel.get(PATH).keys().contains(pathName)) {
            // Host will take precedence; ignore the domain
            result = Collections.emptyMap();
        } else if (ADD.equals(operation.get(OP).asString()) && ! operation.hasDefined(PATH)) {
            // don't push named only paths
            result = Collections.emptyMap();
        }
        else if (hostModel.hasDefined(SERVER_CONFIG)) {
            Set<ServerIdentity> servers = new HashSet<ServerIdentity>();
            for (Property prop : hostModel.get(SERVER_CONFIG).asPropertyList()) {
                ModelNode server = prop.getValue();

                String serverGroupName = server.require(GROUP).asString();

                if (server.hasDefined(PATH) && server.get(PATH).keys().contains(pathName)) {
                    // Server takes precedence; ignore domain
                    continue;
                }

                if (server.hasDefined(AUTO_START) && !server.get(AUTO_START).asBoolean()) {
                    continue;
                }

                ServerIdentity groupedServer = new ServerIdentity(localHostName, serverGroupName, prop.getName());
                servers.add(groupedServer);
            }

            ModelNode serverOp = operation.clone();
            serverOp.get(OP_ADDR).setEmptyList().add(PATH, pathName);
            result = Collections.singletonMap(servers, serverOp);
        }
        else {
            result = Collections.emptyMap();
        }
        return result;
    }

    private Map<Set<ServerIdentity>, ModelNode> getServerSocketBindingGroupOperations(ModelNode operation,
            PathAddress address, ModelNode domain, ModelNode host) {
        String bindingGroupName = address.getElement(0).getValue();
        Set<String> relatedBindingGroups = getRelatedElements(SOCKET_BINDING_GROUP, bindingGroupName, domain);
        Set<ServerIdentity> result = new HashSet<ServerIdentity>();
        for (String bindingGroup : relatedBindingGroups) {
            result.addAll(getServersForType(SOCKET_BINDING_GROUP,bindingGroup, domain, host));
        }
        for (Iterator<ServerIdentity> iter = result.iterator(); iter.hasNext();) {
            ServerIdentity gs = iter.next();
            ModelNode server = host.get(SERVER_CONFIG, gs.getServerName());
            if (server.hasDefined(SOCKET_BINDING_GROUP) && !bindingGroupName.equals(server.get(SOCKET_BINDING_GROUP).asString())) {
                iter.remove();
            }
        }
        ModelNode serverOp = operation.clone();
        return Collections.singletonMap(result, serverOp);
    }

    private Set<String> getRelatedElements(String containerType, String parent, ModelNode domainModel) {
        Set<String> result = new HashSet<String>();
        result.add(parent);
        Set<String> checked = new HashSet<String>();
        checked.add(parent);

        // Ignore any peers the target element includes
        ModelNode targetContainer = domainModel.get(containerType, parent);
        if (targetContainer.hasDefined(INCLUDES)) {
            for (ModelNode include : targetContainer.get(INCLUDES).asList()) {
                checked.add(include.asString());
            }
        }

        List<Property> allContainers = domainModel.get(containerType).asPropertyList();
        while (checked.size() < allContainers.size()) {
            for (Property prop : allContainers) {
                String name = prop.getName();
                if (!checked.contains(name)) {
                    ModelNode container = prop.getValue();
                    if (!container.hasDefined(INCLUDES)) {
                        checked.add(name);
                    }
                    else {
                        boolean allKnown = true;
                        for (ModelNode include : container.get(INCLUDES).asList()) {
                            String includeName = include.asString();
                            if (result.contains(includeName)) {
                                result.add(includeName);
                                break;
                            }
                            else if (!checked.contains(includeName)) {
                                allKnown = false;
                                break;
                            }
                        }
                        if (allKnown) {
                            checked.add(name);
                        }
                    }
                }
            }
        }
        return result;
    }

    private Map<Set<ServerIdentity>, ModelNode> getServerGroupOperations(ModelNode operation, PathAddress address,
            ModelNode domain, ModelNode host) {
        Map<Set<ServerIdentity>, ModelNode> result = null;
        if (address.size() > 1) {
            String type = address.getElement(1).getKey();
            if (JVM.equals(type)) {
                // TODO need to reflect that affected servers are out of date. Perhaps an op for this?
                result = Collections.emptyMap();
            }
            else if (DEPLOYMENT.equals(type)) {
                String groupName = address.getElement(0).getValue();
                Set<ServerIdentity> servers = getServersForGroup(groupName, host);
                ModelNode serverOp = operation.clone();
                if (ADD.equals(serverOp.get(OP).asString())) {
                    // The op is missing the runtime-name and hash values that the server will need
                    ModelNode domainDeployment = domain.get(DEPLOYMENT, address.getElement(1).getValue());
                    serverOp.get(RUNTIME_NAME).set(domainDeployment.get(RUNTIME_NAME));
                    serverOp.get(HASH).set(domainDeployment.get(HASH));
                }
                PathAddress serverAddress = address.subAddress(1);
                serverOp.get(OP_ADDR).set(serverAddress.toModelNode());
                result = Collections.singletonMap(servers, serverOp);
            }
        }

        if (result == null) {
            String opName = operation.require(OP).asString();
            if (SystemPropertyAddHandler.OPERATION_NAME.equals(opName) ||
                    SystemPropertyRemoveHandler.OPERATION_NAME.equals(opName)) {

                String propName = operation.require(NAME).asString();
                Set<ServerIdentity> servers = null;
                // See if overridden at the host
                if (!hasSystemProperty(host, propName)) {
                    if (host.hasDefined(SERVER_CONFIG)) {
                        servers = new HashSet<ServerIdentity>();
                        String groupName = address.getElement(0).getValue();
                        for (Property serverProp : host.get(SERVER_CONFIG).asPropertyList()) {
                            ModelNode server = serverProp.getValue();
                            if (groupName.equals(server.require(GROUP).asString())
                                    && !hasSystemProperty(server, propName)
                                    && (!server.hasDefined(AUTO_START) || server.get(AUTO_START).asBoolean())) {
                                servers.add(new ServerIdentity(localHostName, groupName, serverProp.getName()));
                            }
                        }
                    }
                }

                if (servers != null && servers.size() > 0) {
                    ModelNode serverOp = operation.clone();
                    serverOp.get(OP_ADDR).setEmptyList();
                    result = Collections.singletonMap(servers, serverOp);
                }
            }
        }
        if (result == null) {
            result = Collections.emptyMap();
        }
        return result;
    }

    private Map<Set<ServerIdentity>, ModelNode> resolveDomainRootOperation(ModelNode operation, ModelNode domain, ModelNode host) {
        Map<Set<ServerIdentity>, ModelNode> result = null;
        String opName = operation.require(OP).asString();
        if (SystemPropertyAddHandler.OPERATION_NAME.equals(opName) || SystemPropertyRemoveHandler.OPERATION_NAME.equals(opName)) {
            String propName = operation.require(NAME).asString();
            Set<ServerIdentity> servers = null;
            // See if overridden at the host
            if (!hasSystemProperty(host, propName)) {
                if (host.hasDefined(SERVER_CONFIG)) {
                    servers = new HashSet<ServerIdentity>();
                    for (Property serverProp : host.get(SERVER_CONFIG).asPropertyList()) {
                        ModelNode server = serverProp.getValue();
                        if (!hasSystemProperty(server, propName)
                                && (!server.hasDefined(AUTO_START) || server.get(AUTO_START).asBoolean())) {
                            String groupName = server.require(GROUP).asString();
                            ModelNode serverGroup = domain.get(GROUP, groupName);
                            if (!hasSystemProperty(serverGroup, propName)) {
                                servers.add(new ServerIdentity(localHostName, groupName, serverProp.getName()));
                            }
                        }
                    }
                }
            }

            if (servers != null && servers.size() > 0) {
                ModelNode serverOp = operation.clone();
                serverOp.get(OP_ADDR).setEmptyList();
                result = Collections.singletonMap(servers, serverOp);
            }
        }
        else if (DeploymentFullReplaceHandler.OPERATION_NAME.equals(opName)) {
            String propName = operation.require(NAME).asString();
            Set<String> groups = getServerGroupsForDeployment(propName, domain);
            Set<ServerIdentity> allServers = new HashSet<ServerIdentity>();
            for (String group : groups) {
                allServers.addAll(getServersForGroup(group, host));
            }
            return Collections.singletonMap(allServers, operation);
        }

        if (result == null) {
            result = Collections.emptyMap();
        }

        return result;
    }

    private Set<String> getServerGroupsForDeployment(String deploymentName, ModelNode domainModel) {
        Set<String> groups;
        if (domainModel.hasDefined(SERVER_GROUP)) {
            groups = new HashSet<String>();
            for (Property prop : domainModel.get(SERVER_GROUP).asPropertyList()) {
                ModelNode serverGroup = prop.getValue();
                if (serverGroup.hasDefined(DEPLOYMENT) && serverGroup.get(DEPLOYMENT).hasDefined(deploymentName)) {
                    groups.add(prop.getName());
                }
            }
        }
        else {
            groups = Collections.emptySet();
        }
        return groups;
    }

    private boolean hasSystemProperty(ModelNode resource, String propName) {
        boolean result = false;
        if (resource.hasDefined(SYSTEM_PROPERTIES)) {
            for (Property prop : resource.get(SYSTEM_PROPERTIES).asPropertyList()) {
                if (propName.equals(prop.getName()))
                    return true;
            }
        }
        return result;
    }

    private Map<Set<ServerIdentity>, ModelNode> getServerHostOperations(ModelNode operation, PathAddress address,
            ModelNode domain, ModelNode host) {
        if (address.size() == 1) {
            return resolveHostRootOperation(operation, host);
        }
        else {
            HostKey hostKey = HostKey.forName(address.getElement(1).getKey());
            address = address.subAddress(1); // Get rid of the host=hostName
            switch (hostKey) {
                case PATH: {
                    return getServerPathOperations(operation, address, host, false);
                }
                case MANAGEMENT: {
                    // TODO does server need to know about change?
                    return Collections.emptyMap();
                }
                case INTERFACE: {
                    return getServerInterfaceOperations(operation, address, host, false);
                }
                case JVM: {
                    // TODO does server need to know about change?
                    return Collections.emptyMap();
                }
                case SERVER: {
                    return resolveServerConfigOperation(operation, address, host);
                }
                case RUNNING_SERVER:
                default:
                    throw new IllegalStateException(String.format("Unexpected initial path key %s", address.getElement(0).getKey()));
            }
        }
    }

    private Map<Set<ServerIdentity>, ModelNode> resolveHostRootOperation(ModelNode operation, ModelNode host) {
        Map<Set<ServerIdentity>, ModelNode> result = null;
        String opName = operation.require(OP).asString();
        if (WRITE_ATTRIBUTE_OPERATION.equals(opName)) {
            // TODO add an op notifying server of host's name?
            result = Collections.emptyMap();
        }
        else if (SystemPropertyAddHandler.OPERATION_NAME.equals(opName) || SystemPropertyRemoveHandler.OPERATION_NAME.equals(opName)) {
            String propName = operation.require(NAME).asString();
            Set<ServerIdentity> servers = null;
            if (host.hasDefined(SERVER_CONFIG)) {
                servers = new HashSet<ServerIdentity>();
                for (Property serverProp : host.get(SERVER_CONFIG).asPropertyList()) {
                    ModelNode server = serverProp.getValue();
                    if (!hasSystemProperty(server, propName)
                            && (!server.hasDefined(AUTO_START) || server.get(AUTO_START).asBoolean())) {
                        String groupName = server.require(GROUP).asString();
                        servers.add(new ServerIdentity(localHostName, groupName, serverProp.getName()));
                    }
                }
            }

            if (servers != null && servers.size() > 0) {
                ModelNode serverOp = operation.clone();
                serverOp.get(OP_ADDR).setEmptyList();
                result = Collections.singletonMap(servers, serverOp);
            }
        }
        if (result == null) {
            result = Collections.emptyMap();
        }
        return result;
    }

    private Map<Set<ServerIdentity>, ModelNode> resolveServerConfigOperation(ModelNode operation, PathAddress address,
            ModelNode host) {
        Map<Set<ServerIdentity>, ModelNode> result;
        ModelNode serverOp = null;
        if (address.size() > 2) {
            String type = address.getElement(2).getKey();
            if (PATH.equals(type) || INTERFACE.equals(type)) {
                serverOp = operation.clone();
                PathAddress serverAddress = address.subAddress(2);
                serverOp.get(OP_ADDR).set(serverAddress.toModelNode());
            }
        }
        else {
            String opName = operation.require(OP).asString();
            if (SystemPropertyAddHandler.OPERATION_NAME.equals(opName) || SystemPropertyRemoveHandler.OPERATION_NAME.equals(opName)) {
                serverOp = operation.clone();
                serverOp.get(OP_ADDR).setEmptyList();
            }
            // TODO - deal with "add", "remove" and changing "auto-start" attribute
        }

        if (serverOp == null) {
            result = Collections.emptyMap();
        }
        else {
            String serverName = address.getElement(0).getValue();
            ModelNode serverNode = host.get(SERVER_CONFIG, serverName);
            ServerIdentity gs = new ServerIdentity(localHostName, serverNode.require(GROUP).asString(), serverName);
            Set<ServerIdentity> set = Collections.singleton(gs);
            result = Collections.singletonMap(set, serverOp);
        }
        return result;
    }
}
