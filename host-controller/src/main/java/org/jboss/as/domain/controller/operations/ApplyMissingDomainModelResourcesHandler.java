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
package org.jboss.as.domain.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_MODEL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.extension.ExtensionResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.DomainControllerMessages;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

/**
 * Step handler used on a slave HC to apply missing parts of the domain model at runtime. This will happen either
 * <ul>
 *      <li>as a result of changing some configuration on the DC, and piggybacking the missing data down from the DC to the slave. The slave then applies these missing resources
 *      using this operation handler before changing the configuration in the local DC copy. In this case the ServerOperationHandler is responsible for propagating the restart-required
 *      to the affected servers.</li>
 *      <li>as a result of changing some configuration on the slave, which needs additional data from the DC. The operation handler is then responsible for pulling down the missing
 *      data and applying it using this handler. The ServerOperationHandler is again responsible for propagating the restart-required
 *      to the affected servers</li>
 * </ul>
 *
 * {@link ApplyRemoteMasterDomainModelHandler} contains similar functionality for when a slave connects to the DC.
 *
 * @see ApplyRemoteMasterDomainModelHandler
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ApplyMissingDomainModelResourcesHandler implements OperationStepHandler {
    public static final String OPERATION_NAME = "apply-missing-domain-resources";

    protected final DomainController domainController;
    protected final HostControllerEnvironment hostControllerEnvironment;
    protected final LocalHostControllerInfo localHostInfo;
    protected final IgnoredDomainResourceRegistry ignoredResourceRegistry;


    //Private method does not need resources for description
    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME, null)
        .setPrivateEntry()
        .build();


    public ApplyMissingDomainModelResourcesHandler(DomainController domainController,
            HostControllerEnvironment hostControllerEnvironment, LocalHostControllerInfo localHostInfo, IgnoredDomainResourceRegistry ignoredResourceRegistry) {
        this.domainController = domainController;
        this.hostControllerEnvironment = hostControllerEnvironment;
        this.localHostInfo = localHostInfo;
        this.ignoredResourceRegistry = ignoredResourceRegistry;
    }

    /**
     * Creates the operation to invoke when missing data is piggy-backed as a result of a change to the DC model
     *
     * @param missingResources a model node containing the missing resources list
     * @return the operation
     */
    public static ModelNode createPiggyBackedMissingDataOperation(ModelNode missingResources) {
        ModelNode applyMissingResourcesOp = new ModelNode();
        applyMissingResourcesOp.get(OP).set(ApplyMissingDomainModelResourcesHandler.OPERATION_NAME);
        applyMissingResourcesOp.get(OP_ADDR).setEmptyList();
        applyMissingResourcesOp.get(DOMAIN_MODEL).set(missingResources);
        return applyMissingResourcesOp;
    }

    /**
     * Creates the operation to invoke when missing data is obtained from the DC as a result of a change to a slave's server-config
     *
     * @param missingResources a model node containing the missing resources list
     * @return the operation
     */
    public static ModelNode createPulledMissingDataOperation(ModelNode missingResources) {
        ModelNode applyMissingResourcesOp = createPiggyBackedMissingDataOperation(missingResources);
        return applyMissingResourcesOp;
    }

    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

        // We get the model as a list of resources descriptions
        final ModelNode domainModel = operation.get(DOMAIN_MODEL);
        final Resource rootResource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);

        final List<ModelNode> nonExtensions = new ArrayList<>();

        for (ModelNode resourceDescription : domainModel.asList()) {
            PathAddress resourceAddress = PathAddress.pathAddress(resourceDescription.require(ReadMasterDomainModelUtil.DOMAIN_RESOURCE_ADDRESS));
            if (ignoredResourceRegistry.isResourceExcluded(resourceAddress)) {
                continue;
            }
            if (resourceAddress.size() == 1 && resourceAddress.getElement(0).getKey().equals(EXTENSION)) {
                //Apply the extensions first
                PathElement extensionElement = resourceAddress.getElement(0);
                if (!rootResource.hasChild(extensionElement)) {
                    initializeExtension(extensionElement.getValue());
                    getResource(resourceAddress, rootResource, context).writeModel(resourceDescription.get(DOMAIN_MODEL));
                }
            } else {
                nonExtensions.add(resourceDescription);
            }
        }

        //Now apply the new model
        for (final ModelNode resourceDescription : nonExtensions) {
            final PathAddress resourceAddress = PathAddress.pathAddress(resourceDescription.require(ReadMasterDomainModelUtil.DOMAIN_RESOURCE_ADDRESS));
            final Resource resource = getResource(resourceAddress, rootResource, context);
            resource.writeModel(resourceDescription.get(ReadMasterDomainModelUtil.DOMAIN_RESOURCE_MODEL));
        }

        //We don't need to restart the servers here, that should be handled by the ServerOperationResolver

        context.stepCompleted();
    }

    protected Resource getResource(PathAddress resourceAddress, Resource rootResource, OperationContext context) {
        if(resourceAddress.size() == 0) {
            return rootResource;
        }
        Resource temp = rootResource;
        for(PathElement element : resourceAddress) {
            temp = temp.getChild(element);
            if(temp == null) {
                String type = element.getKey();
                if (type.equals(EXTENSION)) {
                    // Needs a specialized resource type
                    temp = new ExtensionResource(element.getValue(), domainController.getExtensionRegistry());
                    context.addResource(resourceAddress, temp);
                }
                if (temp == null) {
                    temp = context.createResource(resourceAddress);
                }
            }
        }
        return temp;
    }


    protected void initializeExtension(String module) throws OperationFailedException {
        try {
            for (final Extension extension : Module.loadServiceFromCallerModuleLoader(ModuleIdentifier.fromString(module), Extension.class)) {
                ClassLoader oldTccl = SecurityActions.setThreadContextClassLoader(extension.getClass());
                try {
                    extension.initializeParsers(domainController.getExtensionRegistry().getExtensionParsingContext(module, null));
                    extension.initialize(domainController.getExtensionRegistry().getExtensionContext(module, false));
                } finally {
                    SecurityActions.setThreadContextClassLoader(oldTccl);
                }
            }
        } catch (ModuleLoadException e) {
            throw DomainControllerMessages.MESSAGES.failedToLoadModule(e, module);
        }
    }

}
