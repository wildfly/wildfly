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
package org.jboss.as.host.controller.mgmt;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.AttachmentKey;
import org.jboss.as.controller.OperationContext.ResultAction;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.TransformingProxyController;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.operations.ReadMasterDomainModelUtil;
import org.jboss.as.host.controller.IgnoredNonAffectedServerGroupsUtil.ServerConfigInfo;
import org.jboss.dmr.ModelNode;
import org.jboss.util.collection.ConcurrentSet;

/**
 * Registry for the DC to keep track of what data is missing on a slave HC, and to piggyback that information to the slave if changes to the domain model
 * pull in data missing in the slave
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DomainControllerRuntimeIgnoreTransformationRegistry {

    public static final String MISSING_DOMAIN_RESOURCES = "missing-domain-resources";


    public static final AttachmentKey<NewSlaveInformation> NEW_SLAVE_INFO = AttachmentKey.create(NewSlaveInformation.class);


    ConcurrentMap<String, DomainControllerRuntimeIgnoreTransformationEntry> hostEntries = new ConcurrentHashMap<String, DomainControllerRuntimeIgnoreTransformationEntry>();
    ConcurrentMap<String, ConcurrentSet<PathElement>> hostKnownAddresses = new ConcurrentHashMap<String, ConcurrentSet<PathElement>>();

    public DomainControllerRuntimeIgnoreTransformationRegistry() {
    }

    /**
     * Called when a host registration request is initiated
     *
     * @param name the name of the host
     */
    public void initializeHost(String name) {
        hostKnownAddresses.put(name, new ConcurrentSet<PathElement>());
    }

    //These are for use by DomainControllerService
    /**
     * Called when a host has been registered
     *
     * @param name the host name
     * @param hostEntry the entry for a host
     */
    public void registerHost(String name, DomainControllerRuntimeIgnoreTransformationEntry hostEntry) {
        hostEntries.put(name, hostEntry);
        hostEntry.setKnownRootAddresses(hostKnownAddresses.get(name));
    }

    /**
     * Called when a host has been unregistered
     */
    public void unregisterHost(String name) {
        hostEntries.remove(name);
        hostKnownAddresses.remove(name);
    }

    //These are for use by ReadMasterDomainHandler and DomainSlaveHandler, and must be called with the domain controller lock taken
    /**
     * Called when missing data is piggy-backed to a slave host following a change to a server group at domain level, or when changes to a host's
     * server config needs missing data.
     *
     * @param host the host name
     * @param pathElements the root domain resource address elements that the slave now knows about
     */
    public void addKnownDataForSlave(String host, Set<PathElement> pathElements) {
        hostKnownAddresses.get(host).addAll(pathElements);
    }

    /**
     * Used to piggy back missing domain information to a slave host. The missing information will have been put into the operation attachments but
     * {@link #changeServerGroupProfile(OperationContext, PathAddress, String)} or {@link #changeServerGroupSocketBindingGroup(OperationContext, PathAddress, String)}
     *
     * @param context the operation context
     * @param transformingProxyController the transforming proxy controller for the host
     * @param hostName the name of the host
     * @param operation the operation being pushed out to the slave host
     * @return the modified operation
     */
    public ModelNode piggyBackMissingInformationOnHeader(final OperationContext context, final TransformingProxyController transformingProxyController, final String hostName, final ModelNode operation) throws OperationFailedException {
        NewSlaveInformation info = getAttachment(context, false);
        if (info == null) {
            return operation;
        }
        Set<PathElement> missingResources = info.missingResources.get(hostName);
        if (missingResources == null) {
            return operation;
        }

        final ReadMasterDomainModelUtil readUtil = ReadMasterDomainModelUtil.readMasterDomainResourcesForPiggyBackFollowingDomainControllerChange(context, missingResources, transformingProxyController.getTransformers(), hostName, this);
        if (readUtil.getDescribedResources().size() > 0) {
            operation.get(OPERATION_HEADERS, MISSING_DOMAIN_RESOURCES).set(readUtil.getDescribedResources());
        }

        context.addStep(new OperationStepHandler() {
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                context.completeStep(new OperationContext.ResultHandler() {
                    public void handleResult(ResultAction resultAction, OperationContext context, ModelNode operation) {
                        if (resultAction == ResultAction.KEEP) {
                            //Add the piggy-backed root resources to the known resources
                            addKnownDataForSlave(hostName, readUtil.getNewKnownRootResources());
                        }
                    }
                });
            }
        }, Stage.DOMAIN);

        return operation;
    }

    /**
     * Updates a host's missing resources
     *
     * @param context the operation context
     */
    public void updateKnownResources(OperationContext context){
        NewSlaveInformation info = getAttachment(context, false);
        if (info == null) {
            return;
        }
        for (Map.Entry<String, Set<PathElement>> entry : info.missingResources.entrySet()) {
            hostKnownAddresses.get(entry.getKey()).addAll(entry.getValue());
        }
    }

    //These are for use by the master ServerGroup operations handlers

    /**
     * Used when changing a server group's profile to piggy back the missing profile and extensions if not known on the affected slaves.
     * The data gets stored in an operation attachment for use when
     *
     * @param context the operation context
     * @param serverGroupAddress the server group address
     * @param newProfile the name of the new profile
     */
    public void changeServerGroupProfile(OperationContext context, PathAddress serverGroupAddress, String newProfile) {
        if (hostEntries.size() > 0) {
            Resource domainResource = context.readResource(PathAddress.EMPTY_ADDRESS);
            for (Map.Entry<String, DomainControllerRuntimeIgnoreTransformationEntry> entry : hostEntries.entrySet()) {
                if (!entry.getValue().ignoreResource(domainResource, serverGroupAddress)) {
                    PathElement profileAddress = PathElement.pathElement(PROFILE, newProfile);
                    if (!hostKnownAddresses.get(entry.getKey()).contains(profileAddress)) {
                        NewSlaveInformation newSlaveInformation = getAttachment(context, true);
                        newSlaveInformation.addMissingResource(entry.getKey(), profileAddress);
                    }
                }
            }
        }
    }

    /**
     * Used when changing a server group's socket binding group to piggy back the missing socket binding group if not known on the affected slaves
     *
     * @param context the operation context
     * @param serverGroupAddress the server group address
     * @param newSocketBindingGroup the name of the new socket binding group
     */
    public void changeServerGroupSocketBindingGroup(OperationContext context, PathAddress serverGroupAddress, String newSocketBindingGroup) {
        if (hostEntries.size() > 0) {
            Resource domainResource = context.readResource(PathAddress.EMPTY_ADDRESS);
            for (Map.Entry<String, DomainControllerRuntimeIgnoreTransformationEntry> entry : hostEntries.entrySet()) {
                if (!entry.getValue().ignoreResource(domainResource, serverGroupAddress)) {
                    PathElement socketBindingGroupAddress = PathElement.pathElement(SOCKET_BINDING_GROUP, newSocketBindingGroup);
                    if (!hostKnownAddresses.get(entry.getKey()).contains(socketBindingGroupAddress)) {
                        NewSlaveInformation newSlaveInformation = getAttachment(context, true);
                        newSlaveInformation.addMissingResource(entry.getKey(), socketBindingGroupAddress);
                    }
                }
            }
        }
    }

    /**
     * Check if a server group is one of a host's known resources
     *
     * @param domainRoot the root domain resource
     * @param hostName the name of the host
     * @param serverGroupName the name of the server group
     */
    public boolean isServerGroupKnown(Resource domainRoot, String hostName, String serverGroupName) {
        DomainControllerRuntimeIgnoreTransformationEntry hostIgnore = hostEntries.get(hostName);
        return !hostIgnore.ignoreResource(domainRoot, PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, serverGroupName)));
    }

    /**
     * Check if a socket binding group is one of a host's known resources
     *
     * @param domainRoot the root domain resource
     * @param hostName the name of the host
     * @param socketBindingGroup the name of the socket binding group
     */
    public boolean isSocketBindingGroupKnown(Resource domainRoot, String hostName, String socketBindingGroup) {
        DomainControllerRuntimeIgnoreTransformationEntry hostIgnore = hostEntries.get(hostName);
        return !hostIgnore.ignoreResource(domainRoot, PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, socketBindingGroup)));
    }

    /**
     * Check if a profile is one of a host's known resources
     *
     * @param domainRoot the root domain resource
     * @param hostName the name of the host
     * @param profile the name of the profile
     */
    public boolean isProfileKnown(Resource domainRoot, String hostName, String profile) {
        DomainControllerRuntimeIgnoreTransformationEntry hostIgnore = hostEntries.get(hostName);
        return !hostIgnore.ignoreResource(domainRoot, PathAddress.pathAddress(PathElement.pathElement(PROFILE, profile)));
    }

    /**
     * Gets all the unknown extensions for a profile's subsystems on a host
     *
     * @param domainResource the root domain resource
     * @param hostName the name of the host
     * @param profileElement the profile address to check
     * @return the unknown extensions
     */
    public Set<PathElement> getUnknownExtensionsForProfile(Resource domainRoot, String hostName, String profile) {
        DomainControllerRuntimeIgnoreTransformationEntry hostIgnore = hostEntries.get(hostName);
        return hostIgnore.getUnknownExtensionsForProfile(domainRoot, PathElement.pathElement(PROFILE, profile));

    }

    /**
     * Adds/changes a server config info for a host
     *
     * @param hostName the name of the host
     * @param serverInfo the new/changed server config info
     */
    public void updateSlaveServerConfig(String hostName, ServerConfigInfo serverInfo) {
        DomainControllerRuntimeIgnoreTransformationEntry hostIgnore = hostEntries.get(hostName);
        hostIgnore.updateSlaveServerConfig(serverInfo);
    }

    private NewSlaveInformation getAttachment(OperationContext context, boolean create) {
        NewSlaveInformation info = context.getAttachment(NEW_SLAVE_INFO);
        if (info == null && create) {
            info = new NewSlaveInformation();
            NewSlaveInformation old = context.attachIfAbsent(NEW_SLAVE_INFO, info);
            if (old != null) {
                return old;
            }
        }
        return info;
    }

    private class NewSlaveInformation {
        private final Map<String, Set<PathElement>> missingResources = Collections.synchronizedMap(new HashMap<String, Set<PathElement>>());

        void addMissingResource(String host, PathElement address) {
            Set<PathElement> hostMissingResources = missingResources.get(host);
            if (hostMissingResources == null) {
                hostMissingResources = Collections.synchronizedSet(new HashSet<PathElement>());
                missingResources.put(host, hostMissingResources);
            }
            hostMissingResources.add(address);
        }
    }
}
