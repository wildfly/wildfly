/*
 *
 *  * JBoss, Home of Professional Open Source.
 *  * Copyright 2013, Red Hat, Inc., and individual contributors
 *  * as indicated by the @author tags. See the copyright.txt file in the
 *  * distribution for a full listing of individual contributors.
 *  *
 *  * This is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */
package org.jboss.as.host.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IGNORE_UNUSED_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.SubsystemInformation;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.Resource.ResourceEntry;
import org.jboss.dmr.ModelNode;

/**
 * Utility to inspect what resources should be ignored on a slave according to its server-configs
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class IgnoredNonAffectedServerGroupsUtil {

    private final ExtensionRegistry extensionRegistry;

    private IgnoredNonAffectedServerGroupsUtil(final ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    /**
     * Static factory
     *
     * @param extensionRegistry the extension registry
     * @return the created instance
     */
    public static IgnoredNonAffectedServerGroupsUtil create(final ExtensionRegistry extensionRegistry) {
        return new IgnoredNonAffectedServerGroupsUtil(extensionRegistry);
    }

    /**
     * Used by the slave host when creating the host info dmr sent across to the DC during the registration process
     *
     * @param ignoreUnaffectedServerGroups whether the slave host is set up to ignore config for server groups it does not have servers for
     * @param hostModel the resource containing the host model
     * @param the dmr sent across to theDC
     * @return the modified dmr
     */
    public static ModelNode addCurrentServerGroupsToHostInfoModel(boolean ignoreUnaffectedServerGroups, Resource hostModel, ModelNode model) {
        if (!ignoreUnaffectedServerGroups) {
            return model;
        }
        model.get(IGNORE_UNUSED_CONFIG).set(ignoreUnaffectedServerGroups);
        ModelNode initialServerGroups = new ModelNode();
        initialServerGroups.setEmptyObject();
        for (ResourceEntry entry : hostModel.getChildren(SERVER_CONFIG)) {
            ModelNode serverNode = new ModelNode();
            serverNode.get(GROUP).set(entry.getModel().get(GROUP));
            if (entry.getModel().hasDefined(SOCKET_BINDING_GROUP)) {
                serverNode.get(SOCKET_BINDING_GROUP).set(entry.getModel().get(SOCKET_BINDING_GROUP).asString());
            }
            initialServerGroups.get(entry.getName()).set(serverNode);
        }
        model.get(ModelDescriptionConstants.INITIAL_SERVER_GROUPS).set(initialServerGroups);
        return model;
    }

    /**
     * For the DC to check whether a resource address should be ignored on the slave, if the slave is set up to ignore config not relevant to it
     *
     * @param domainResource the domain root resource
     * @param serverConfigs the server configs the slave is known to have
     * @param pathAddress the address of the resource to check if should be ignored or not
     */
    public boolean ignoreResource(final Resource domainResource, final Collection<ServerConfigInfo> serverConfigs, final PathAddress pathAddress) {
        if (pathAddress.size() != 1) {
            return false;
        }
        boolean ignore = ignoreResourceInternal(domainResource, serverConfigs, pathAddress);
        return ignore;
    }

    /**
     * For the DC to check whether an operation should be ignored on the slave, if the slave is set up to ignore config not relevant to it
     *
     * @param domainResource the domain root resource
     * @param serverConfigs the server configs the slave is known to have
     * @param pathAddress the address of the operation to check if should be ignored or not
     */
    public boolean ignoreOperation(final Resource domainResource, final Collection<ServerConfigInfo> serverConfigs, final PathAddress pathAddress) {
        if (pathAddress.size() == 0) {
            return false;
        }
        boolean ignore = ignoreResourceInternal(domainResource, serverConfigs, pathAddress);
        return ignore;
    }

    /**
     * Gets all the extensions used by a profile's subsystems on the DC
     *
     * @param domainResource the root domain resource
     * @param profileElement the address of the profile element
     */
    public Set<PathElement> getAllExtensionsForProfile(Resource domainResource, PathElement profileElement) {
        Set<String> extensionModuleNames = extensionRegistry.getExtensionModuleNames();
        Set<String> subsystemNamesForProfile = new HashSet<>();
        for (ResourceEntry entry : domainResource.getChild(profileElement).getChildren(SUBSYSTEM)) {
            subsystemNamesForProfile.add(entry.getName());
        }
        Set<PathElement> extensionsForProfile = new HashSet<>();
        for (String extensionModule : extensionModuleNames) {
            Map<String, SubsystemInformation> infos = extensionRegistry.getAvailableSubsystems(extensionModule);
            for (String subsystemName : infos.keySet()) {
                if (subsystemNamesForProfile.contains(subsystemName)) {
                    extensionsForProfile.add(PathElement.pathElement(EXTENSION, extensionModule));
                }
            }
        }
        return extensionsForProfile;
    }

    private boolean ignoreResourceInternal(final Resource domainResource, final Collection<ServerConfigInfo> serverConfigs, final PathAddress pathAddress) {
        String type = pathAddress.getElement(0).getKey();
        switch (type) {
        case PROFILE:
            return ignoreProfile(domainResource, serverConfigs, pathAddress.getElement(0).getValue());
        case SERVER_GROUP:
            return ignoreServerGroup(domainResource, serverConfigs, pathAddress.getElement(0).getValue());
        case EXTENSION:
            return ignoreExtension(domainResource, serverConfigs, pathAddress.getElement(0).getValue());
        case SOCKET_BINDING_GROUP:
            return ignoreSocketBindingGroups(domainResource, serverConfigs, pathAddress.getElement(0).getValue());
        default:
            return false;
        }
    }

    private boolean ignoreProfile(final Resource domainResource, final Collection<ServerConfigInfo> serverConfigs, final String name) {
        Set<String> seenGroups = new HashSet<>();
        for (ServerConfigInfo serverConfig : serverConfigs) {
            if (seenGroups.contains(serverConfig.getServerGroup())) {
                continue;
            }
            seenGroups.add(serverConfig.getServerGroup());
            Resource serverGroupResource = domainResource.getChild(PathElement.pathElement(SERVER_GROUP, serverConfig.getServerGroup()));
            if (serverGroupResource.getModel().get(PROFILE).asString().equals(name)) {
                return false;
            }
        }
        return true;
    }

    private boolean ignoreServerGroup(final Resource domainResource, final Collection<ServerConfigInfo> serverConfigs, final String name) {
        for (ServerConfigInfo serverConfig : serverConfigs) {
            if (serverConfig.getServerGroup().equals(name)) {
                return false;
            }
        }
        return true;
    }

    private boolean ignoreExtension(final Resource domainResource, final Collection<ServerConfigInfo> serverConfigs, final String name) {
        //Should these be the subsystems on the master, as we have it at present, or the ones from the slave?
        Map<String, SubsystemInformation> subsystems = extensionRegistry.getAvailableSubsystems(name);
        for (String subsystem : subsystems.keySet()) {
            for (ResourceEntry profileEntry : domainResource.getChildren(PROFILE)) {
                if (profileEntry.hasChild(PathElement.pathElement(SUBSYSTEM, subsystem))) {
                    if (!ignoreProfile(domainResource, serverConfigs, profileEntry.getName())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean ignoreSocketBindingGroups(final Resource domainResource, final Collection<ServerConfigInfo> serverConfigs, final String name) {
        for (ServerConfigInfo serverConfig : serverConfigs) {
            if (serverConfig.getSocketBindingGroup() != null) {
                if (serverConfig.getSocketBindingGroup().equals(name)) {
                    return false;
                }
            } else {
                Resource serverGroupResource = domainResource.getChild(PathElement.pathElement(SERVER_GROUP, serverConfig.getServerGroup()));
                if (name.equals(serverGroupResource.getModel().get(SOCKET_BINDING_GROUP).asString())) {
                    return false;
                }
            }
        }
        return true;
    }



    /**
     * For use on a slave HC to get all the server groups used by the host
     *
     * @param hostResource the host resource
     * @return the server configs on this host
     */
    public Set<ServerConfigInfo> getServerConfigsOnSlave(Resource hostResource){
        Set<ServerConfigInfo> groups = new HashSet<>();
        for (ResourceEntry entry : hostResource.getChildren(SERVER_CONFIG)) {
            groups.add(new ServerConfigInfoImpl(entry.getName(), entry.getModel()));
        }
        return groups;
    }

    /**
     * Creates a server config info from its name, its server group and its socket binding group
     *
     * @param name the name of the server config
     * @param serverGroup the name of the server group
     * @param socketBindingGroup the name of the socket binding override used by the server config. May be {@code null}
     * @return the server config info
     */
    public static ServerConfigInfo createServerConfigInfo(String name, String serverGroup, String socketBindingGroup) {
        return new ServerConfigInfoImpl(name, serverGroup, socketBindingGroup);
    }

    /**
     * Creates a server config info from it's model representation as created by {@link ServerConfigInfo#toModelNode()}
     *
     * @param model the model
     * @return the server config info
     *
     */
    public static ServerConfigInfo createServerConfigInfo(ModelNode model) {
        String name = model.keys().iterator().next();
        return new ServerConfigInfoImpl(name, model.get(name));
    }

    /**
     * Contains info about a server config
     */
    public interface ServerConfigInfo {
        /**
         * Gets the server config name
         *
         * @return the name
         */
        String getName();

        /**
         * Gets the server config's server group name
         *
         * @return the server group name
         */
        String getServerGroup();

        /**
         * Gets the server config's socket binding group override name
         *
         * @return the socket binding group name. May be {@code null}
         */
        String getSocketBindingGroup();

        /**
         * Serializes the server config to dmr
         *
         * @return the dmr representation of this server config
         */
        ModelNode toModelNode();
    }


    private static class ServerConfigInfoImpl implements ServerConfigInfo {
        private final String name;
        private final String serverGroup;
        private final String socketBindingGroup;

        ServerConfigInfoImpl(String name, ModelNode model) {
            this.name = name;
            this.serverGroup = model.get(GROUP).asString();
            this.socketBindingGroup = model.has(SOCKET_BINDING_GROUP) ? model.get(SOCKET_BINDING_GROUP).asString() : null;
        }

        ServerConfigInfoImpl(String name, String serverGroup, String socketBindingGroup) {
            this.name = name;
            this.serverGroup = serverGroup;
            this.socketBindingGroup = socketBindingGroup;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getServerGroup() {
            return serverGroup;
        }

        @Override
        public String getSocketBindingGroup() {
            return socketBindingGroup;
        }

        @Override
        public ModelNode toModelNode() {
            ModelNode model = new ModelNode();
            model.get(name, GROUP).set(serverGroup);
            if (socketBindingGroup != null) {
                model.get(name, SOCKET_BINDING_GROUP).set(socketBindingGroup);
            }
            return model;
        }
    }
}
