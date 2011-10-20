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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_MODEL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
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
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.dmr.ModelNode;
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

    //This is a hack to avoid initializing the extensions again for the case when master is restarted and we reconnect
    private boolean appliedExensions;
    private final FileRepository fileRepository;

    private final ExtensionContext extensionContext;

    public ApplyRemoteMasterDomainModelHandler(final ExtensionContext extensionContext, final FileRepository fileRepository) {
        this.extensionContext = extensionContext;
        this.fileRepository = fileRepository;
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        // We get the model as a list of resources descriptions
        final ModelNode domainModel = operation.get(DOMAIN_MODEL);

        if (!appliedExensions) {

            final Set<String> ourServerGroups = getOurServerGroups(context);
            final Map<String, Set<byte[]>> deploymentHashes = new HashMap<String, Set<byte[]>>();
            final Set<String> relevantDeployments = new HashSet<String>();

            for(final ModelNode resourceDescription : domainModel.asList()) {
                appliedExensions = true;
                final PathAddress resourceAddress = PathAddress.pathAddress(resourceDescription.require("domain-resource-address"));
                final Resource resource = resourceAddress.size() == 0 ? context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS) : context.createResource(resourceAddress);
                if(resourceAddress.size() == 1 && resourceAddress.getElement(0).getKey().equals(ModelDescriptionConstants.EXTENSION)) {
                    final String module = resourceAddress.getElement(0).getValue();
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
        }
        context.completeStep();
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
