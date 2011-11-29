/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.controller.operations;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_MODEL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.controller.ServerIdentity;
import org.jboss.as.domain.controller.operations.coordination.DomainServerUtils;
import static org.jboss.as.domain.controller.operations.coordination.DomainServerUtils.getRelatedElements;
import static org.jboss.as.domain.controller.operations.coordination.DomainServerUtils.getServersForGroup;
import static org.jboss.as.domain.controller.operations.coordination.DomainServerUtils.getServersForType;
import org.jboss.as.server.operations.ServerRestartRequiredHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

/**
 * Step handler responsible for taking in a domain model and updating the local domain model to match.
 *
 * @author John Bailey
 */
public class ApplyRemoteMasterDomainModelHandler implements OperationStepHandler, DescriptionProvider {
    public static final String OPERATION_NAME = "apply-remote-domain-model";

    private final Set<String> appliedExensions = new HashSet<String>();
    private final FileRepository fileRepository;

    private final ExtensionContext extensionContext;

    private final LocalHostControllerInfo localHostInfo;

    public ApplyRemoteMasterDomainModelHandler(final ExtensionContext extensionContext, final FileRepository fileRepository, final LocalHostControllerInfo localHostInfo) {
        this.extensionContext = extensionContext;
        this.fileRepository = fileRepository;
        this.localHostInfo = localHostInfo;
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        // We get the model as a list of resources descriptions
        final ModelNode domainModel = operation.get(DOMAIN_MODEL);

        final ModelNode startRoot = Resource.Tools.readModel(context.getRootResource());

        final Set<String> ourServerGroups = getOurServerGroups(context);
        final Map<String, Set<byte[]>> deploymentHashes = new HashMap<String, Set<byte[]>>();
        final Set<String> relevantDeployments = new HashSet<String>();

        final Resource rootResource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        clearDomain(rootResource);

        for (final ModelNode resourceDescription : domainModel.asList()) {
            final PathAddress resourceAddress = PathAddress.pathAddress(resourceDescription.require("domain-resource-address"));
            final Resource resource = getResource(resourceAddress, rootResource, context);
            if (resourceAddress.size() == 1 && resourceAddress.getElement(0).getKey().equals(EXTENSION)) {
                final String module = resourceAddress.getElement(0).getValue();
                if (!appliedExensions.contains(module)) {
                    appliedExensions.add(module);
                    initializeExtension(module);
                }
            }
            resource.writeModel(resourceDescription.get("domain-resource-model"));

            // Track deployment hashes and server group deployments so we can pull over the content we need
            if (resourceAddress.size() == 1
                    && resourceAddress.getElement(0).getKey().equals(DEPLOYMENT)) {
                ModelNode model = resource.getModel();
                String id = resourceAddress.getElement(0).getValue();
                if (model.hasDefined(CONTENT)) {
                    for (ModelNode contentItem : model.get(CONTENT).asList()) {
                        if (contentItem.hasDefined(HASH)) {
                            Set<byte[]> hashes = deploymentHashes.get(id);
                            if (hashes == null) {
                                hashes = new HashSet<byte[]>();
                                deploymentHashes.put(id, hashes);
                            }
                            hashes.add(contentItem.get(HASH).asBytes());
                        }
                    }
                }

            } else if (resourceAddress.size() == 2
                    && resourceAddress.getElement(0).getKey().equals(SERVER_GROUP)
                    && ourServerGroups.contains(resourceAddress.getElement(0).getValue())
                    && resourceAddress.getElement(1).getKey().equals(DEPLOYMENT)) {
                relevantDeployments.add(resourceAddress.getElement(1).getValue());
            }
        }

        // Make sure we have all needed deployment content
        for (String id : relevantDeployments) {
            Set<byte[]> hashes = deploymentHashes.remove(id);
            if (hashes != null) {
                for (byte[] hash : hashes) {
                    fileRepository.getDeploymentFiles(hash);
                }
            }
        }

        if (!context.isBooting()) {
            final ModelNode endRoot = Resource.Tools.readModel(context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS));
            final Set<ServerIdentity> affectedServers = new HashSet<ServerIdentity>();
            final ModelNode hostModel = endRoot.require(HOST).asPropertyList().iterator().next().getValue();
            final ModelNode existingHostModel = startRoot.require(HOST).asPropertyList().iterator().next().getValue();

            final Map<String, ProxyController> serverProxies = getServerProxies(context);

            final ModelNode startExtensions = startRoot.get(EXTENSION);
            final ModelNode finishExtensions = endRoot.get(EXTENSION);
            if (!startExtensions.equals(finishExtensions)) {
                // This affects all servers
                affectedServers.addAll(DomainServerUtils.getAllRunningServers(hostModel, localHostInfo.getLocalHostName(), serverProxies));
            }

            final ModelNode startPaths = startRoot.get(PATH);
            final ModelNode endPaths = endRoot.get(PATH);
            final Map<String, ModelNode> existingPaths = new HashMap<String, ModelNode>();
            if (startPaths.isDefined()) for (Property property : startPaths.asPropertyList()) {
                existingPaths.put(property.getName(), property.getValue());
            }
            if (endPaths.isDefined()) for (Property path : endPaths.asPropertyList()) {
                final String pathName = path.getName();
                if (existingPaths.containsKey(pathName)) {
                    if (!path.getValue().equals(existingPaths.get(pathName))) {
                        affectedServers.addAll(getServersAffectedByPath(pathName, hostModel, true));
                    }
                    existingPaths.remove(pathName);
                } else {
                    affectedServers.addAll(getServersAffectedByPath(pathName, hostModel, true));
                }
            }
            for (Map.Entry<String, ModelNode> entry : existingPaths.entrySet()) {
                affectedServers.addAll(getServersAffectedByPath(entry.getKey(), existingHostModel, true));
            }

            final ModelNode startSysProps = startRoot.get(SYSTEM_PROPERTY);
            final ModelNode endSysProps = endRoot.get(SYSTEM_PROPERTY);
            final Map<String, ModelNode> existingProps = new HashMap<String, ModelNode>();
            if (startSysProps.isDefined()) for (Property property : startSysProps.asPropertyList()) {
                existingProps.put(property.getName(), property.getValue());
            }
            if (endSysProps.isDefined()) for (Property property : endSysProps.asPropertyList()) {
                if (existingProps.containsKey(property.getName())) {
                    if (!property.getValue().equals(existingProps.get(property.getName()))) {
                        affectedServers.addAll(getServerAffectedBySystemProperty(property.getName(), true, endRoot, null, hostModel));
                    }
                    existingProps.remove(property.getName());
                } else {
                    affectedServers.addAll(getServerAffectedBySystemProperty(property.getName(), true, endRoot, null, hostModel));
                }
            }
            for (Map.Entry<String, ModelNode> entry : existingProps.entrySet()) {
                affectedServers.addAll(getServerAffectedBySystemProperty(entry.getKey(), true, startRoot, null, existingHostModel));
            }

            final ModelNode startProfiles = startRoot.get(PROFILE);
            final ModelNode endProfiles = endRoot.get(PROFILE);
            final Map<String, ModelNode> existingProfiles = new HashMap<String, ModelNode>();
            if (startProfiles.isDefined()) for (Property profile : startProfiles.asPropertyList()) {
                existingProfiles.put(profile.getName(), profile.getValue());
            }
            if (endProfiles.isDefined()) for (Property profile : endProfiles.asPropertyList()) {
                if (existingProfiles.containsKey(profile.getName())) {
                    if (!profile.getValue().equals(existingProfiles.get(profile.getName()))) {
                        affectedServers.addAll(getServerAffectedByProfile(profile.getName(), endRoot, hostModel, serverProxies));
                    }
                    existingProfiles.remove(profile.getName());
                } else {
                    affectedServers.addAll(getServerAffectedByProfile(profile.getName(), endRoot, hostModel, serverProxies));
                }
            }
            for (Map.Entry<String, ModelNode> entry : existingProfiles.entrySet()) {
                affectedServers.addAll(getServerAffectedByProfile(entry.getKey(), startRoot, existingHostModel, serverProxies));
            }

            final ModelNode startInterfaces = startRoot.get(INTERFACE);
            final ModelNode endInterfaces = endRoot.get(INTERFACE);
            final Map<String, ModelNode> existingInterfaces = new HashMap<String, ModelNode>();
            if (startInterfaces.isDefined()) for (Property interfaceProp : startInterfaces.asPropertyList()) {
                existingInterfaces.put(interfaceProp.getName(), interfaceProp.getValue());
            }
            if (endInterfaces.isDefined()) for (Property interfaceProp : endInterfaces.asPropertyList()) {
                if (existingInterfaces.containsKey(interfaceProp.getName())) {
                    if (!interfaceProp.getValue().equals(existingInterfaces.get(interfaceProp.getName()))) {
                        affectedServers.addAll(getServersAffectedByInterface(interfaceProp.getName(), hostModel, true));
                    }
                    existingInterfaces.remove(interfaceProp.getName());
                } else {
                    affectedServers.addAll(getServersAffectedByInterface(interfaceProp.getName(), hostModel, true));
                }
            }
            for (Map.Entry<String, ModelNode> entry : existingInterfaces.entrySet()) {
                affectedServers.addAll(getServersAffectedByInterface(entry.getKey(), existingHostModel, true));
            }

            final ModelNode startBindingGroups = startRoot.get(SOCKET_BINDING_GROUP);
            final ModelNode endBindingGroups = endRoot.get(SOCKET_BINDING_GROUP);
            final Map<String, ModelNode> existingBindingGroups = new HashMap<String, ModelNode>();
            if (startBindingGroups.isDefined()) for (Property bindingGroup : startBindingGroups.asPropertyList()) {
                existingBindingGroups.put(bindingGroup.getName(), bindingGroup.getValue());
            }
            if (endBindingGroups.isDefined()) for (Property bindingGroup : endBindingGroups.asPropertyList()) {
                if (existingBindingGroups.containsKey(bindingGroup.getName())) {
                    if (!bindingGroup.getValue().equals(existingBindingGroups.get(bindingGroup.getName()))) {
                        affectedServers.addAll(getServersAffectedBySocketBindingGroup(bindingGroup.getName(), endRoot, hostModel, serverProxies));
                    }
                    existingBindingGroups.remove(bindingGroup.getName());
                } else {
                    affectedServers.addAll(getServersAffectedBySocketBindingGroup(bindingGroup.getName(), endRoot, hostModel, serverProxies));
                }
            }
            for (Map.Entry<String, ModelNode> entry : existingBindingGroups.entrySet()) {
                affectedServers.addAll(getServersAffectedBySocketBindingGroup(entry.getKey(), startRoot, existingHostModel, serverProxies));
            }

            final ModelNode startServerGroups = startRoot.get(SERVER_GROUP);
            final ModelNode endServerGroups = endRoot.get(SERVER_GROUP);
            final Map<String, ModelNode> existingServerGroups = new HashMap<String, ModelNode>();
            if (startServerGroups.isDefined()) for (Property serverGroup : startServerGroups.asPropertyList()) {
                existingServerGroups.put(serverGroup.getName(), serverGroup.getValue());
            }
            if (endServerGroups.isDefined()) for (Property serverGroup : endServerGroups.asPropertyList()) {
                if (existingServerGroups.containsKey(serverGroup.getName())) {
                    if (!serverGroup.getValue().equals(existingServerGroups.get(serverGroup.getName()))) {
                        affectedServers.addAll(getServersForGroup(serverGroup.getName(), hostModel, localHostInfo.getLocalHostName(), serverProxies));
                    }
                    existingServerGroups.remove(serverGroup.getName());
                } else {
                    affectedServers.addAll(getServersForGroup(serverGroup.getName(), hostModel, localHostInfo.getLocalHostName(), serverProxies));
                }
            }
            for (Map.Entry<String, ModelNode> entry : existingServerGroups.entrySet()) {
                affectedServers.addAll(getServersForGroup(entry.getKey(), hostModel, localHostInfo.getLocalHostName(), serverProxies));
            }

            if (!affectedServers.isEmpty()) {
                Logger.getLogger(ApplyRemoteMasterDomainModelHandler.class).info("Domain model has changed on re-connect.  The following servers will need to be restarted for changes to take affect: " + affectedServers);
                final Set<ServerIdentity> runningServers = DomainServerUtils.getAllRunningServers(hostModel, localHostInfo.getLocalHostName(), serverProxies);
                for (ServerIdentity serverIdentity : affectedServers) {
                    if(!runningServers.contains(serverIdentity)) {
                        continue;
                    }
                    final PathAddress serverAddress = PathAddress.pathAddress(PathElement.pathElement(HOST, serverIdentity.getHostName()), PathElement.pathElement(SERVER, serverIdentity.getServerName()));
                    final OperationStepHandler handler = context.getResourceRegistration().getOperationHandler(serverAddress, ServerRestartRequiredHandler.OPERATION_NAME);
                    final ModelNode op = new ModelNode();
                    op.get(OP).set(ServerRestartRequiredHandler.OPERATION_NAME);
                    op.get(OP_ADDR).set(serverAddress.toModelNode());
                    context.addStep(op, handler, OperationContext.Stage.IMMEDIATE);
                }
            }
        }
        context.completeStep();
    }

    private Map<String, ProxyController> getServerProxies(OperationContext context) {
        final Set<String> serverNames = context.readResource(PathAddress.pathAddress(PathElement.pathElement(HOST, localHostInfo.getLocalHostName()))).getChildrenNames(SERVER_CONFIG);
        final Map<String, ProxyController> proxies = new HashMap<String, ProxyController>();
        for(String serverName : serverNames) {
            final PathAddress serverAddress = PathAddress.pathAddress(PathElement.pathElement(HOST, localHostInfo.getLocalHostName()), PathElement.pathElement(SERVER, serverName));
            final ProxyController proxyController = context.getResourceRegistration().getProxyController(serverAddress);
            if(proxyController != null) {
                proxies.put(serverName, proxyController);
            }
        }
        return proxies;
    }

    protected void initializeExtension(String module) {
        try {
            for (final Extension extension : Module.loadServiceFromCallerModuleLoader(ModuleIdentifier.fromString(module), Extension.class)) {
                ClassLoader oldTccl = SecurityActions.setThreadContextClassLoader(extension.getClass());
                try {
                    extension.initialize(extensionContext);
                } finally {
                    SecurityActions.setThreadContextClassLoader(oldTccl);
                }
            }
        } catch (ModuleLoadException e) {
            throw new RuntimeException(e);
        }
    }

    private void clearDomain(final Resource rootResource) {
        for(Resource.ResourceEntry entry : rootResource.getChildren(EXTENSION)) {
            rootResource.removeChild(entry.getPathElement());
        }
        for(Resource.ResourceEntry entry : rootResource.getChildren(ModelDescriptionConstants.PATH)) {
            rootResource.removeChild(entry.getPathElement());
        }
        for(Resource.ResourceEntry entry : rootResource.getChildren(ModelDescriptionConstants.SYSTEM_PROPERTY)) {
            rootResource.removeChild(entry.getPathElement());
        }
        for(Resource.ResourceEntry entry : rootResource.getChildren(ModelDescriptionConstants.PROFILE)) {
            rootResource.removeChild(entry.getPathElement());
        }
        for(Resource.ResourceEntry entry : rootResource.getChildren(ModelDescriptionConstants.INTERFACE)) {
            rootResource.removeChild(entry.getPathElement());
        }
        for(Resource.ResourceEntry entry : rootResource.getChildren(ModelDescriptionConstants.SOCKET_BINDING_GROUP)) {
            rootResource.removeChild(entry.getPathElement());
        }
        for(Resource.ResourceEntry entry : rootResource.getChildren(ModelDescriptionConstants.DEPLOYMENT)) {
            rootResource.removeChild(entry.getPathElement());
        }
        for(Resource.ResourceEntry entry : rootResource.getChildren(ModelDescriptionConstants.SERVER_GROUP)) {
            rootResource.removeChild(entry.getPathElement());
        }
    }

    private Resource getResource(PathAddress resourceAddress, Resource rootResource, OperationContext context) {
        if(resourceAddress.size() == 0) {
            return rootResource;
        }
        Resource temp = rootResource;
        for(PathElement element : resourceAddress) {
            temp = temp.getChild(element);
            if(temp == null) {
                return context.createResource(resourceAddress);
            }
        }
        return temp;
    }

    private Collection<ServerIdentity> getServersAffectedByPath(final String pathName, final ModelNode hostModel, final boolean forDomain) {
        if (forDomain && hostModel.hasDefined(PATH) && hostModel.get(PATH).keys().contains(pathName)) {
            // Host will take precedence; ignore the domain
            return Collections.emptySet();
        } else if (hostModel.hasDefined(SERVER_CONFIG)) {
            Set<ServerIdentity> servers = new HashSet<ServerIdentity>();
            for (Property prop : hostModel.get(SERVER_CONFIG).asPropertyList()) {

                String serverName = prop.getName();
                ModelNode server = prop.getValue();

                String serverGroupName = server.require(GROUP).asString();

                if (server.hasDefined(PATH) && server.get(PATH).keys().contains(pathName)) {
                    // Server takes precedence; ignore domain
                    continue;
                }

                ServerIdentity groupedServer = new ServerIdentity(localHostInfo.getLocalHostName(), serverGroupName, serverName);
                servers.add(groupedServer);
            }
            return servers;
        }
        return Collections.emptySet();
    }

    private Collection<ServerIdentity> getServerAffectedBySystemProperty(final String propName, final boolean isDomain, final ModelNode domain, final String affectedGroup, final ModelNode host) {
        boolean overridden = false;
        Set<String> groups = null;
        if (isDomain) {
            if (hasSystemProperty(host, propName)) {
                // host level value takes precedence
                overridden = true;
            } else if (affectedGroup != null) {
                groups = Collections.singleton(affectedGroup);
            } else if (domain.hasDefined(SERVER_GROUP)) {
                // Top level domain update applies to all groups where it was not overridden
                groups = new HashSet<String>();
                for (Property groupProp : domain.get(SERVER_GROUP).asPropertyList()) {
                    String groupName = groupProp.getName();
                    if (!hasSystemProperty(groupProp.getValue(), propName)) {
                        groups.add(groupName);
                    }
                }
            }
        }
        Set<ServerIdentity> servers = null;
        if (!overridden && host.hasDefined(SERVER_CONFIG)) {
            servers = new HashSet<ServerIdentity>();
            for (Property serverProp : host.get(SERVER_CONFIG).asPropertyList()) {
                String serverName = serverProp.getName();
                ModelNode server = serverProp.getValue();
                if (!hasSystemProperty(server, propName)) {
                    String groupName = server.require(GROUP).asString();
                    if (groups == null || groups.contains(groupName)) {
                        servers.add(new ServerIdentity(localHostInfo.getLocalHostName(), groupName, serverName));
                    }
                }
            }
        }
        if (servers != null) {
            return servers;
        }
        return Collections.emptySet();
    }

    private boolean hasSystemProperty(final ModelNode resource, final String propName) {
        return resource.hasDefined(SYSTEM_PROPERTY) && resource.get(SYSTEM_PROPERTY).hasDefined(propName);
    }

    private Collection<ServerIdentity> getServerAffectedByProfile(final String profileName, final ModelNode domain, final ModelNode host, final Map<String, ProxyController> serverProxies) {
        Set<String> relatedProfiles = getRelatedElements(PROFILE, profileName, domain);
        Set<ServerIdentity> allServers = new HashSet<ServerIdentity>();
        for (String profile : relatedProfiles) {
            allServers.addAll(getServersForType(PROFILE, profile, domain, host, localHostInfo.getLocalHostName(), serverProxies));
        }
        return allServers;
    }

    private Collection<ServerIdentity> getServersAffectedByInterface(final String interfaceName, final ModelNode hostModel, final boolean forDomain) {
        if (forDomain && hostModel.hasDefined(INTERFACE) && hostModel.get(INTERFACE).keys().contains(interfaceName)) {
            // Host will take precedence; ignore the domain
            return Collections.emptySet();
        } else if (hostModel.hasDefined(SERVER_CONFIG)) {
            Set<ServerIdentity> servers = new HashSet<ServerIdentity>();
            for (Property prop : hostModel.get(SERVER_CONFIG).asPropertyList()) {
                String serverName = prop.getName();
                ModelNode server = prop.getValue();

                String serverGroupName = server.require(GROUP).asString();
                if (server.hasDefined(INTERFACE) && server.get(INTERFACE).keys().contains(interfaceName)) {
                    // Server takes precedence; ignore domain
                    continue;
                }

                ServerIdentity groupedServer = new ServerIdentity(localHostInfo.getLocalHostName(), serverGroupName, serverName);
                servers.add(groupedServer);
            }
            return servers;
        }
        return Collections.emptySet();
    }

    private Collection<ServerIdentity> getServersAffectedBySocketBindingGroup(final String bindingGroupName, final ModelNode domain, final ModelNode host, final Map<String, ProxyController> serverProxies) {
        Set<String> relatedBindingGroups = getRelatedElements(SOCKET_BINDING_GROUP, bindingGroupName, domain);
        Set<ServerIdentity> result = new HashSet<ServerIdentity>();
        for (String bindingGroup : relatedBindingGroups) {
            result.addAll(getServersForType(SOCKET_BINDING_GROUP, bindingGroup, domain, host, localHostInfo.getLocalHostName(), serverProxies));
        }
        for (Iterator<ServerIdentity> iter = result.iterator(); iter.hasNext(); ) {
            ServerIdentity gs = iter.next();
            ModelNode server = host.get(SERVER_CONFIG, gs.getServerName());
            if (server.hasDefined(SOCKET_BINDING_GROUP) && !bindingGroupName.equals(server.get(SOCKET_BINDING_GROUP).asString())) {
                iter.remove();
            }
        }
        return result;
    }

    private Set<String> getOurServerGroups(OperationContext context) {
        Set<String> result = new HashSet<String>();

        Resource root = context.readResource(PathAddress.EMPTY_ADDRESS);
        Resource host = root.getChildren(HOST).iterator().next();
        for (Resource server : host.getChildren(SERVER_CONFIG)) {
            ModelNode model = server.getModel();
            result.add(model.get(GROUP).asString());
        }

        return result;
    }

    public ModelNode getModelDescription(Locale locale) {
        return new ModelNode(); // PRIVATE operation requires no description
    }
}
