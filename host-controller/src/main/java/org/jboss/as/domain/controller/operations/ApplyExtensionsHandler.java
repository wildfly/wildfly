/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_MODEL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.ExtensionResource;
import org.jboss.as.controller.registry.Resource;
import static org.jboss.as.domain.controller.DomainControllerLogger.ROOT_LOGGER;
import org.jboss.as.domain.controller.DomainControllerMessages;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.controller.ServerIdentity;
import org.jboss.as.domain.controller.operations.coordination.DomainServerUtils;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.server.operations.ServerRestartRequiredHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Step handler responsible for adding the extensions as part of the host registration process.
 *
 * @author Emanuel Muckenhuber
 */
public class ApplyExtensionsHandler implements OperationStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "resolve-subsystems";
    private final Set<String> appliedExtensions = new HashSet<String>();
    private final ExtensionRegistry extensionRegistry;
    private final LocalHostControllerInfo localHostInfo;
    private final IgnoredDomainResourceRegistry ignoredResourceRegistry;

    public ApplyExtensionsHandler(ExtensionRegistry extensionRegistry, LocalHostControllerInfo localHostInfo, final IgnoredDomainResourceRegistry ignoredResourceRegistry) {
        this.extensionRegistry = extensionRegistry;
        this.localHostInfo = localHostInfo;
        this.ignoredResourceRegistry = ignoredResourceRegistry;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        // We get the model as a list of resources descriptions
        final ModelNode domainModel = operation.get(DOMAIN_MODEL);
        final ModelNode startRoot = Resource.Tools.readModel(context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS));

        final Resource rootResource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        for(Resource.ResourceEntry entry : rootResource.getChildren(EXTENSION)) {
            rootResource.removeChild(entry.getPathElement());
        }

        for (final ModelNode resourceDescription : domainModel.asList()) {
            final PathAddress resourceAddress = PathAddress.pathAddress(resourceDescription.require("domain-resource-address"));
            if (ignoredResourceRegistry.isResourceExcluded(resourceAddress)) {
                continue;
            }

            final Resource resource = getResource(resourceAddress, rootResource, context);
            if (resourceAddress.size() == 1 && resourceAddress.getElement(0).getKey().equals(EXTENSION)) {
                final String module = resourceAddress.getElement(0).getValue();
                if (!appliedExtensions.contains(module)) {
                    appliedExtensions.add(module);
                    initializeExtension(module);
                }
            } else {
                continue;
            }
            resource.writeModel(resourceDescription.get("domain-resource-model"));
        }
        if (!context.isBooting()) {
            final Resource domainRootResource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
            final ModelNode endRoot = Resource.Tools.readModel(domainRootResource);
            final Set<ServerIdentity> affectedServers = new HashSet<ServerIdentity>();
            final ModelNode hostModel = endRoot.require(HOST).asPropertyList().iterator().next().getValue();
            final Map<String, ProxyController> serverProxies = DomainServerUtils.getServerProxies(localHostInfo.getLocalHostName(), domainRootResource, context.getResourceRegistration());

            final ModelNode startExtensions = startRoot.get(EXTENSION);
            final ModelNode finishExtensions = endRoot.get(EXTENSION);
            if (!startExtensions.equals(finishExtensions)) {
                // This affects all servers
                affectedServers.addAll(DomainServerUtils.getAllRunningServers(hostModel, localHostInfo.getLocalHostName(), serverProxies));
            }

            if (!affectedServers.isEmpty()) {
                ROOT_LOGGER.domainModelChangedOnReConnect(affectedServers);
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

    private Resource getResource(PathAddress resourceAddress, Resource rootResource, OperationContext context) {
        if(resourceAddress.size() == 0) {
            return rootResource;
        }
        Resource temp = rootResource;
        int idx = 0;
        for(PathElement element : resourceAddress) {
            temp = temp.getChild(element);
            if(temp == null) {
                if (idx == 0) {
                    String type = element.getKey();
                    if (type.equals(EXTENSION)) {
                        // Needs a specialized resource type
                        temp = new ExtensionResource(element.getValue(), extensionRegistry);
                        context.addResource(resourceAddress, temp);
                    }
                }
                if (temp == null) {
                    temp = context.createResource(resourceAddress);
                }
                break;
            }
            idx++;
        }
        return temp;
    }

    protected void initializeExtension(String module) throws OperationFailedException {
        try {
            for (final Extension extension : Module.loadServiceFromCallerModuleLoader(ModuleIdentifier.fromString(module), Extension.class)) {
                ClassLoader oldTccl = SecurityActions.setThreadContextClassLoader(extension.getClass());
                try {
                    extension.initializeParsers(extensionRegistry.getExtensionParsingContext(module, null));
                    extension.initialize(extensionRegistry.getExtensionContext(module));
                } finally {
                    SecurityActions.setThreadContextClassLoader(oldTccl);
                }
            }
        } catch (ModuleLoadException e) {
            throw DomainControllerMessages.MESSAGES.failedToLoadModule(e, module);
        }
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return new ModelNode(); // PRIVATE operation requires no description
    }

}
