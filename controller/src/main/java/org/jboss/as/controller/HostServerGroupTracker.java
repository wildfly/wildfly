/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT_OVERLAY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_CONTROLLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FULL_REPLACE_DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOTE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UPLOAD_DEPLOYMENT_BYTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UPLOAD_DEPLOYMENT_STREAM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UPLOAD_DEPLOYMENT_URL;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.access.HostEffect;
import org.jboss.as.controller.access.ServerGroupEffect;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Tracks what server groups are associated with various model resources.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
class HostServerGroupTracker {

    private static final Set<String> UPLOAD_OPS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(UPLOAD_DEPLOYMENT_BYTES, UPLOAD_DEPLOYMENT_STREAM,
                    UPLOAD_DEPLOYMENT_URL)));

    static class HostServerGroupEffect implements HostEffect, ServerGroupEffect {

        private static final Set<String> EMPTY = Collections.emptySet();

        private final PathAddress address;
        private final Set<String> serverGroupEffects;
        private final Set<String> hostEffects;
        private final boolean unassigned;

        private static HostServerGroupEffect forGlobal(PathAddress address) {
            return new HostServerGroupEffect(address, (Set<String>) null, null, false);
        }

        private static HostServerGroupEffect forDomain(PathAddress address,
                                                       Set<String> serverGroupEffects) {
            return new HostServerGroupEffect(address,
                    serverGroupEffects == null ? EMPTY : serverGroupEffects, null, false);
        }

        private static HostServerGroupEffect forUnassignedDomain(PathAddress address) {
            return new HostServerGroupEffect(address, EMPTY, null, true);
        }

        private static HostServerGroupEffect forServerGroup(PathAddress address, String serverGroup) {
            return new HostServerGroupEffect(address, Collections.singleton(serverGroup), null, false);
        }

        private static HostServerGroupEffect forHost(PathAddress address, Set<String> serverGroupEffects, String hostEffect) {
            return new HostServerGroupEffect(address, serverGroupEffects, hostEffect, false);
        }

        private static HostServerGroupEffect forNonLocalHost(PathAddress address, String hostEffect) {
            return new HostServerGroupEffect(address, (Set<String>) null, hostEffect, false);
        }

        private static HostServerGroupEffect forUnassignedHost(PathAddress address, String hostEffect) {
            return new HostServerGroupEffect(address, (Set<String>) null, hostEffect, true);
        }

        private static HostServerGroupEffect forWildCardServerConfig(PathAddress address, String hostEffect) {
            return new HostServerGroupEffect(address, EMPTY, hostEffect, true);
        }


        static HostServerGroupEffect forServer(PathAddress address, String serverGroupEffect, String hostEffect) {
            assert serverGroupEffect != null : "serverGroupEffect is null";
            return new HostServerGroupEffect(address, Collections.singleton(serverGroupEffect), hostEffect, false);
        }


        private HostServerGroupEffect(PathAddress address,
                                      Set<String> serverGroupEffects, String hostEffect, boolean unassigned) {
            this.address = address;
            this.serverGroupEffects = serverGroupEffects;
            this.unassigned = unassigned;
            this.hostEffects = hostEffect == null ? null : Collections.singleton(hostEffect);
        }

        @Override
        public PathAddress getResourceAddress() {
            return address;
        }

        @Override
        public boolean isServerGroupEffectGlobal() {
            return serverGroupEffects == null;
        }

        @Override
        public boolean isServerGroupEffectUnassigned() {
            return unassigned;
        }

        @Override
        public Set<String> getAffectedServerGroups() {
            return serverGroupEffects;
        }

        @Override
        public boolean isHostEffectGlobal() {
            return hostEffects == null;
        }

        @Override
        public Set<String> getAffectedHosts() {
            return hostEffects;
        }
    }

    private boolean requiresMapping = true;
    private final Map<String, Set<String>> profilesToGroups = new HashMap<String, Set<String>>();
    private final Map<String, Set<String>> socketsToGroups = new HashMap<String, Set<String>>();
    private final Map<String, Set<String>> deploymentsToGroups = new HashMap<String, Set<String>>();
    private final Map<String, Set<String>> overlaysToGroups = new HashMap<String, Set<String>>();
    private final Map<String, Set<String>> hostsToGroups = new HashMap<String, Set<String>>();

    HostServerGroupEffect getHostServerGroupEffects(PathAddress address, ModelNode operation, Resource root) {

        final int addrSize = address.size();
        if (addrSize > 0) {

            PathElement firstElement = address.getElement(0);
            String type = firstElement.getKey();
            // Not a switch to ease EAP 6 backport
            if (HOST.equals(type)) {
                String hostName = firstElement.getValue();
                if (addrSize > 1) {
                    PathElement secondElement = address.getElement(1);
                    String lvlone = secondElement.getKey();
                    if (SERVER_CONFIG.equals(lvlone) || SERVER.equals(lvlone)) {
                        Resource hostResource = root.getChild(firstElement);
                        if (hostResource != null) {
                            String serverGroup = null;
                            Resource serverConfig = hostResource.getChild(PathElement.pathElement(SERVER_CONFIG, secondElement.getValue()));
                            if (serverConfig != null) { // may be null if hostName is not the local host
                                ModelNode model = serverConfig.getModel();
                                if (model.hasDefined(GROUP)) {
                                    serverGroup = model.get(GROUP).asString();
                                }
                            }
                            if (serverGroup == null && address.size() == 2 && SERVER_CONFIG.equals(lvlone)) {
                                if (secondElement.isWildcard()) {
                                    // https://issues.jboss.org/browse/WFLY-2299
                                    return HostServerGroupEffect.forWildCardServerConfig(address, hostName);
                                } else if (ADD.equals(operation.require(OP).asString())) {
                                    serverGroup = operation.get(GROUP).asString();
                                }
                            }

                            if (serverGroup != null) {
                                return HostServerGroupEffect.forServer(address, serverGroup, hostName);
                            } // else may be null if hostName is not the local host.
                              // We checked it's not a server-config add so assume it's a read and just provide the
                              // forHost response, which will be acceptable for a read for any server group scoped role
                        } // else not the local host. Can only be a read, so just use the forHost response,
                          // which will be acceptable for a read for any server group scoped role
                        return HostServerGroupEffect.forNonLocalHost(address, hostName);
                    }
                }
                return getHostEffect(address, hostName, root);
            } else if (PROFILE.equals(type)) {
                return getDomainEffect(address, firstElement.getValue(), profilesToGroups, root);
            } else if (SOCKET_BINDING_GROUP.equals(type)) {
                return getDomainEffect(address, firstElement.getValue(), socketsToGroups, root);
            } else if (SERVER_GROUP.equals(type)) {
                // WFLY-2190 make add/remove global. So, s-g-s-r can't remove its own server group
                // and can't add it. This helps the console, but since there ideally would be validation
                // that all groups mapped to a s-g-s-r actually exist, it's reasonable to say the group
                // must exist (so no need for an :add) and can't be removed
                String opName = operation.require(OP).asString();
                if (addrSize > 1 || (!ADD.equals(opName) && !REMOVE.equals(opName))) {
                    return HostServerGroupEffect.forServerGroup(address, firstElement.getValue());
                } // else drop into  HostServerGroupEffect.forGlobal(address);
            } else if (DEPLOYMENT.equals(type)) {
                return getDomainEffect(address, firstElement.getValue(), deploymentsToGroups, root);
            } else if (DEPLOYMENT_OVERLAY.equals(type)) {
                return getDomainEffect(address, firstElement.getValue(), overlaysToGroups, root);
            }
        } else {
            // WFLY-1916 -- need special handling for deployment related ops
            String opName = operation.require(OP).asString();
            if (FULL_REPLACE_DEPLOYMENT.equals(opName)) {
                // The name of the deployment being replaced is what matters
                if (operation.hasDefined(NAME)) {
                    return getDomainEffect(address, operation.get(NAME).asString(),
                            deploymentsToGroups, root);
                }
            } else if (UPLOAD_OPS.contains(opName)) {
                // Treat this like an unmapped deployment
                return HostServerGroupEffect.forUnassignedDomain(address);
            }
        }

        return HostServerGroupEffect.forGlobal(address);
    }

    synchronized void invalidate() {
        requiresMapping = true;
        profilesToGroups.clear();
        socketsToGroups.clear();
        deploymentsToGroups.clear();
        overlaysToGroups.clear();
        hostsToGroups.clear();
    }

    private synchronized HostServerGroupEffect getDomainEffect(PathAddress address, String key,
                                                               Map<String, Set<String>> map, Resource root) {
        if (requiresMapping) {
            map(root);
            requiresMapping = false;
        }
        Set<String> mapped = map.get(key);
        return mapped != null ? HostServerGroupEffect.forDomain(address, mapped)
                              : HostServerGroupEffect.forUnassignedDomain(address);
    }

    private synchronized HostServerGroupEffect getHostEffect(PathAddress address, String host, Resource root)  {
        if (requiresMapping) {
            map(root);
            requiresMapping = false;
        }
        Set<String> mapped = hostsToGroups.get(host);
        if (mapped == null) {
            // Unassigned host. Treat like an unassigned profile or socket-binding-group;
            // i.e. available to all server group scoped roles.
            // Except -- WFLY-2085 -- the master HC is not open to all s-g-s-rs
            Resource hostResource = root.getChild(PathElement.pathElement(HOST, host));
            if (hostResource != null) {
                ModelNode dcModel = hostResource.getModel().get(DOMAIN_CONTROLLER);
                if (!dcModel.hasDefined(REMOTE)) {
                    mapped = Collections.emptySet(); // prevents returning HostServerGroupEffect.forUnassignedHost(address, host)
                }
            }
        }
        return mapped == null ? HostServerGroupEffect.forUnassignedHost(address, host)
                : HostServerGroupEffect.forHost(address, mapped, host);

    }

    /** Only call with monitor for 'this' held */
    private void map(Resource root) {

        for (Resource.ResourceEntry serverGroup : root.getChildren(SERVER_GROUP)) {
            String serverGroupName = serverGroup.getName();
            ModelNode serverGroupModel = serverGroup.getModel();
            String profile = serverGroupModel.require(PROFILE).asString();
            store(serverGroupName, profile, profilesToGroups);
            String socketBindingGroup = serverGroupModel.require(SOCKET_BINDING_GROUP).asString();
            store(serverGroupName, socketBindingGroup, socketsToGroups);

            for (Resource.ResourceEntry deployment : serverGroup.getChildren(DEPLOYMENT)) {
                store(serverGroupName, deployment.getName(), deploymentsToGroups);
            }

            for (Resource.ResourceEntry overlay : serverGroup.getChildren(DEPLOYMENT_OVERLAY)) {
                store(serverGroupName, overlay.getName(), overlaysToGroups);
            }

        }

        for (Resource.ResourceEntry host : root.getChildren(HOST)) {
            String hostName = host.getPathElement().getValue();
            for (Resource.ResourceEntry serverConfig : host.getChildren(SERVER_CONFIG)) {
                ModelNode serverConfigModel = serverConfig.getModel();
                String serverGroupName = serverConfigModel.require(GROUP).asString();
                store(serverGroupName, hostName, hostsToGroups);
            }
        }
    }

    private static void store(String serverGroup, String key, Map<String, Set<String>> map) {
        Set<String> set = map.get(key);
        if (set == null) {
            set = new HashSet<String>();
            map.put(key, set);
        }
        set.add(serverGroup);
    }
}
