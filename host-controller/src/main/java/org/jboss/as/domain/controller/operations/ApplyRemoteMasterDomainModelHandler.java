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

import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_MODEL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_CLIENT_CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLOUT_PLAN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLOUT_PLANS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.domain.controller.DomainControllerLogger.ROOT_LOGGER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.access.management.WritableAuthorizerConfiguration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.controller.ServerIdentity;
import org.jboss.as.domain.controller.operations.coordination.DomainServerUtils;
import org.jboss.as.domain.management.CoreManagementResourceDefinition;
import org.jboss.as.domain.management.access.AccessAuthorizationDomainSlaveConfigHandler;
import org.jboss.as.domain.management.access.AccessAuthorizationResourceDefinition;
import org.jboss.as.domain.management.access.AccessConstraintResources;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.host.controller.ManagedServerBootCmdFactory;
import org.jboss.as.host.controller.ManagedServerBootConfiguration;
import org.jboss.as.host.controller.ManagedServerOperationsFactory;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.management.client.content.ManagedDMRContentTypeResource;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.server.operations.ServerRestartRequiredHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Step handler responsible for taking in a domain model and updating the local domain model to match. This happens when we connect to the domain controller,
 * either:
 * <ul>
 *      <li>When we are first booting</li>
 *      <li>When we reconnect to the DC having already booted. In this case we check the resulting boot operations for each running server of the copy of the domain model we
 *      had with boot operations of the domain model following the applied changes.</li>
 * </ul>
 *
 * {@link ApplyMissingDomainModelResourcesHandler} contains similar functionality for when config is changed at runtime to bring it into the domain model.
 *
 * @author John Bailey
 * @author Kabir Khan
 */
public class ApplyRemoteMasterDomainModelHandler implements OperationStepHandler {
    public static final String OPERATION_NAME = "apply-remote-domain-model";

    //Private method does not need resources for description
    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME, null)
        .withFlag(OperationEntry.Flag.HOST_CONTROLLER_ONLY)
        .setPrivateEntry()
        .build();

    protected final DomainController domainController;
    protected final HostControllerEnvironment hostControllerEnvironment;
    protected final LocalHostControllerInfo localHostInfo;
    protected final IgnoredDomainResourceRegistry ignoredResourceRegistry;
    private final HostFileRepository fileRepository;
    private final ContentRepository contentRepository;
    private final WritableAuthorizerConfiguration authorizerConfiguration;

    public ApplyRemoteMasterDomainModelHandler(final DomainController domainController,
                                               final HostControllerEnvironment hostControllerEnvironment,
                                               final HostFileRepository fileRepository,
                                               final ContentRepository contentRepository,
                                               final LocalHostControllerInfo localHostInfo,
                                               final IgnoredDomainResourceRegistry ignoredResourceRegistry,
                                               final WritableAuthorizerConfiguration authorizerConfiguration) {
        this.domainController = domainController;
        this.hostControllerEnvironment = hostControllerEnvironment;
        this.fileRepository = fileRepository;
        this.contentRepository = contentRepository;
        this.localHostInfo = localHostInfo;
        this.ignoredResourceRegistry = ignoredResourceRegistry;
        this.authorizerConfiguration = authorizerConfiguration;
    }

    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

        // We get the model as a list of resources descriptions
        final ModelNode domainModel = operation.get(DOMAIN_MODEL);

        final ModelNode startRoot = Resource.Tools.readModel(context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS));

        final Set<String> ourServerGroups = getOurServerGroups(context);
        final Map<String, Set<byte[]>> deploymentHashes = new HashMap<String, Set<byte[]>>();
        final Set<String> relevantDeployments = new HashSet<String>();
        final Set<byte[]> requiredContent = new HashSet<byte[]>();

        final Resource rootResource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        clearDomain(rootResource);
        if (!context.isBooting()) {
            authorizerConfiguration.reset();
        }

        List<ModelNode> addOps = new ArrayList<ModelNode>();
        for (final ModelNode resourceDescription : domainModel.asList()) {

            final PathAddress resourceAddress = PathAddress.pathAddress(resourceDescription.require(ReadMasterDomainModelUtil.DOMAIN_RESOURCE_ADDRESS));

            if (ignoredResourceRegistry.isResourceExcluded(resourceAddress)) {
                continue;
            }

            if (resourceAddress.size() == 1 && resourceAddress.getElement(0).getKey().equals(EXTENSION)) {
                // Extensions are handled in ApplyExtensionsHandler
                continue;
            }

            ModelNode resourceModel = resourceDescription.get(ReadMasterDomainModelUtil.DOMAIN_RESOURCE_MODEL);
            final Resource resource = getResource(resourceAddress, rootResource, resourceModel, context, addOps);

            // Track deployment and management content hashes and server group deployments so we can pull over the content we need
            if (resource != null && resourceAddress.size() == 1) {
                PathElement pe = resourceAddress.getElement(0);
                String peKey = pe.getKey();
                if (peKey.equals(DEPLOYMENT)) {
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
                } else if (peKey.equals(MANAGEMENT_CLIENT_CONTENT)) {
                    // We need to pull over management content from the master HC's repo
                    ModelNode model = resource.getModel();
                    if (model.hasDefined(HASH)) {
                        requiredContent.add(model.get(HASH).asBytes());
                    }
                }

            } else if (resourceAddress.size() == 2
                    && resourceAddress.getElement(0).getKey().equals(SERVER_GROUP)
                    && ourServerGroups.contains(resourceAddress.getElement(0).getValue())
                    && resourceAddress.getElement(1).getKey().equals(DEPLOYMENT)) {
                relevantDeployments.add(resourceAddress.getElement(1).getValue());
            }
        }

        // Make sure we have all needed deployment and management client content
        for (String id : relevantDeployments) {
            Set<byte[]> hashes = deploymentHashes.remove(id);
            if (hashes != null) {
                requiredContent.addAll(hashes);
            }
        }
        for (byte[] hash : requiredContent) {
            fileRepository.getDeploymentFiles(hash);
        }

        if (!context.isBooting()) {
            //We have reconnected to the DC. Add an immediate step to put out-of-sync servers in restart-required mode
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    makeAffectedServersRestartRequired(context, startRoot);
                    context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
                }
            }, OperationContext.Stage.MODEL, true);
        }

        // Before the above step, add steps for any ops we need to run
        ImmutableManagementResourceRegistration registry = context.getResourceRegistration();
        for (int i = addOps.size() - 1; i >= 0; i--) {
            ModelNode subOperation = addOps.get(i);
            PathAddress stepAddress = PathAddress.pathAddress(subOperation.get(OP_ADDR));
            String stepOpName = subOperation.require(OP).asString();
            OperationStepHandler stepHandler = registry.getOperationHandler(stepAddress, stepOpName);
            if (stepHandler == null) {
                ImmutableManagementResourceRegistration child = registry.getSubModel(stepAddress);
                if (child == null) {
                    throw new IllegalStateException(MESSAGES.noSuchResourceType(stepAddress));
                } else {
                    throw new IllegalStateException(MESSAGES.noHandlerForOperation(stepOpName, stepAddress));
                }
            }
            context.addStep(subOperation, stepHandler, OperationContext.Stage.MODEL, true);
        }


        context.stepCompleted();
    }

    private void clearDomain(final Resource rootResource) {
        // Extensions are handled in ApplyExtensionsHandler
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
        // Prune parts of the RBAC tree
        Resource accessControl = rootResource.navigate(
                PathAddress.pathAddress(CoreManagementResourceDefinition.PATH_ELEMENT, AccessAuthorizationResourceDefinition.PATH_ELEMENT));
        accessControl.writeModel(new ModelNode());
        for(Resource.ResourceEntry entry : accessControl.getChildren(ModelDescriptionConstants.SERVER_GROUP_SCOPED_ROLE)) {
            rootResource.removeChild(entry.getPathElement());
        }
        for(Resource.ResourceEntry entry : accessControl.getChildren(ModelDescriptionConstants.HOST_SCOPED_ROLE)) {
            rootResource.removeChild(entry.getPathElement());
        }
        for(Resource.ResourceEntry entry : accessControl.getChildren(ModelDescriptionConstants.ROLE_MAPPING)) {
            rootResource.removeChild(entry.getPathElement());
        }
    }

    protected Resource getResource(PathAddress resourceAddress, Resource rootResource, ModelNode resourceModel,
                                   OperationContext context, List<ModelNode> addOps) {
        if(resourceAddress.size() == 0) {
            return rootResource;
        }
        boolean allowCreate = true;
        boolean writeResourceModel = true;
        boolean coreService = false;
        boolean accessControl = false;
        PathElement created = null;
        Resource temp = rootResource;
        int idx = 0;
        for (PathElement element : resourceAddress) {
            temp = temp == null ? null : temp.getChild(element);
            String type = element.getKey();
            assert !EXTENSION.equals(type) : "extension resources should be excluded";
            String value = element.getValue();
            if (temp == null) {
                if (idx == 0) {
                    if (MANAGEMENT_CLIENT_CONTENT.equals(type) && ROLLOUT_PLANS.equals(value)) {
                        // Needs a specialized resource type
                        temp = new ManagedDMRContentTypeResource(element, ROLLOUT_PLAN, null, contentRepository);
                        context.addResource(resourceAddress, temp);
                    }
                } else if (accessControl) {
                    // RBAC config child resources where we need to invoke add ops
                    // to ensure the AuthorizerConfiguration is updated
                    allowCreate = false;
                    if (idx == resourceAddress.size() - 1) {
                        ModelNode addOp = Util.createAddOperation(resourceAddress);
                        if (resourceModel.isDefined()) {
                            for (Property property : resourceModel.asPropertyList()) {
                                addOp.get(property.getName()).set(property.getValue());
                            }
                        }
                        addOps.add(addOp);
                    }
                }
                if (temp == null && allowCreate) {
                    assert created == null : "already created " + created;
                    temp = context.createResource(resourceAddress);
                    created = element;
                }
            } else if (CORE_SERVICE.equals(type) && MANAGEMENT.equals(value)) {
                coreService = true;
            } else if (coreService && idx == 1 && element.equals(AccessAuthorizationResourceDefinition.PATH_ELEMENT)) {
                accessControl = true;
                if (idx == resourceAddress.size() - 1) {
                    writeResourceModel = false;
                    // Invoke a specialized op for high level rbac config
                    ModelNode configureOp = Util.createEmptyOperation(AccessAuthorizationDomainSlaveConfigHandler.OPERATION_NAME, resourceAddress);
                    for (AttributeDefinition ad : AccessAuthorizationResourceDefinition.CONFIG_ATTRIBUTES) {
                        String attrName = ad.getName();
                        if (resourceModel.hasDefined(attrName)) {
                            configureOp.get(attrName).set(resourceModel.get(attrName));
                        }
                    }
                    addOps.add(configureOp);
                }
            } else if (accessControl && idx == 2
                        && (AccessConstraintResources.APPLICATION_PATH_ELEMENT.equals(element)
                            || AccessConstraintResources.SENSITIVITY_PATH_ELEMENT.equals(element)
                            || AccessConstraintResources.VAULT_PATH_ELEMENT.equals(element))) {
                // Just write the model to the resources in these trees
                accessControl = false;
                allowCreate = false;
            }
            idx++;
        }
        if (writeResourceModel && temp != null) {
            temp.writeModel(resourceModel);
        }
        return temp;
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

    private void makeAffectedServersRestartRequired(OperationContext context, ModelNode startRoot) {
        final Resource domainRootResource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        final ModelNode endRoot = Resource.Tools.readModel(domainRootResource);
        final ModelNode hostModel = endRoot.require(HOST).asPropertyList().iterator().next().getValue();
        final ModelNode existingHostModel = startRoot.require(HOST).asPropertyList().iterator().next().getValue();

        final Set<ServerIdentity> affectedServers;
        //Path that gets called when missing data is piggy-backed as a result of changes to the domain model
        affectedServers = new HashSet<ServerIdentity>();

        if (!hostModel.hasDefined(SERVER_CONFIG)) {
            return;
        }

        for (String serverName : hostModel.get(SERVER_CONFIG).keys()) {
            ModelNode startOps = createBootOps(context, serverName, startRoot, existingHostModel);
            ModelNode endOps = createBootOps(context, serverName, endRoot, hostModel);
            if (bootOpsChanged(startOps, endOps)) {
                affectedServers.add(createServerIdentity(hostModel, serverName));
            }
            ManagedServerBootConfiguration startConfig = new ManagedServerBootCmdFactory(serverName, startRoot, existingHostModel, hostControllerEnvironment, domainController.getExpressionResolver()).createConfiguration();
            ManagedServerBootConfiguration endConfig = new ManagedServerBootCmdFactory(serverName, endRoot, hostModel, hostControllerEnvironment, domainController.getExpressionResolver()).createConfiguration();
            if (!startConfig.getServerLaunchCommand().equals(endConfig.getServerLaunchCommand())) {
                affectedServers.add(createServerIdentity(hostModel, serverName));
            }
        }

        final Map<String, ProxyController> serverProxies = DomainServerUtils.getServerProxies(localHostInfo.getLocalHostName(), domainRootResource, context.getResourceRegistration());

        if (!affectedServers.isEmpty()) {
            ROOT_LOGGER.domainModelChangedOnReConnect(affectedServers);
            final Set<ServerIdentity> runningServers = DomainServerUtils.getAllRunningServers(hostModel, localHostInfo.getLocalHostName(), serverProxies);
            for (ServerIdentity serverIdentity : affectedServers) {
                if(!runningServers.contains(serverIdentity)) {
                    continue;
                }
                //TODO https://issues.jboss.org/browse/AS7-6858 If the launch command did not change, we should put the server into reload-required
                // If the launch command changed, restart-required is still needed
                final PathAddress serverAddress = PathAddress.pathAddress(PathElement.pathElement(HOST, serverIdentity.getHostName()), PathElement.pathElement(SERVER, serverIdentity.getServerName()));
                final OperationStepHandler handler = context.getResourceRegistration().getOperationHandler(serverAddress, ServerRestartRequiredHandler.OPERATION_NAME);
                final ModelNode op = new ModelNode();
                op.get(OP).set(ServerRestartRequiredHandler.OPERATION_NAME);
                op.get(OP_ADDR).set(serverAddress.toModelNode());
                context.addStep(op, handler, OperationContext.Stage.MODEL, true);
            }
        }
    }

    private ServerIdentity createServerIdentity(ModelNode hostModel, String serverName) {
        return new ServerIdentity(localHostInfo.getLocalHostName(), hostModel.require(SERVER_CONFIG).require(serverName).require(GROUP).asString(), serverName);
    }

    private boolean bootOpsChanged(ModelNode startOps, ModelNode endOps) {
        //The boot ops could be in a different order, so do a compare ignoring the order
        List<ModelNode> startOpList = startOps.asList();
        List<ModelNode> endOpList = endOps.asList();
        if (startOpList.size() != endOpList.size()) {
            return true;
        }
        Set<ModelNode> startOpSet = new HashSet<>(startOpList);
        Set<ModelNode> endOpSet = new HashSet<>(endOpList);
        return !startOpSet.equals(endOpSet);
    }

    private ModelNode createBootOps(final OperationContext context, final String serverName, final ModelNode domainModel, final ModelNode hostModel) {
        return ManagedServerOperationsFactory.createBootUpdates(serverName, domainModel, hostModel, domainController, new ExpressionResolver() {
            @Override
            public ModelNode resolveExpressions(final ModelNode node) throws OperationFailedException {
                return context.resolveExpressions(node);
            }
        });

    }

    private static class ResourceAddition {
        private final Resource addedResource;
        private final List<ModelNode> addOps;


        private ResourceAddition(Resource addedResource, List<ModelNode> addOps) {
            this.addedResource = addedResource;
            this.addOps = addOps;
        }
    }
}
