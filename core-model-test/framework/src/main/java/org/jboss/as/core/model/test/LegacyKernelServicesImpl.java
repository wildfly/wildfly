/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.core.model.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_MODEL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_CLIENT_CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLOUT_PLAN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLOUT_PLANS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.util.List;

import org.jboss.as.controller.ControlledProcessState.State;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.validation.OperationValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.OperationTransformer.TransformedOperation;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.controller.operations.ApplyRemoteMasterDomainModelHandler;
import org.jboss.as.host.controller.discovery.DiscoveryOption;
import org.jboss.as.host.controller.ignored.IgnoreDomainResourceTypeResource;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.management.client.content.ManagedDMRContentTypeResource;
import org.jboss.as.model.test.ModelTestModelControllerService;
import org.jboss.as.model.test.StringConfigurationPersister;
import org.jboss.as.repository.ContentRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceContainer;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class LegacyKernelServicesImpl extends AbstractKernelServicesImpl {
    private final ContentRepository contentRepository;

    public LegacyKernelServicesImpl(ServiceContainer container, ModelTestModelControllerService controllerService,
            StringConfigurationPersister persister, ManagementResourceRegistration rootRegistration,
            OperationValidator operationValidator, ModelVersion legacyModelVersion, boolean successfulBoot, Throwable bootError,
            ExtensionRegistry extensionRegistry, ContentRepository contentRepository) {
        // FIXME MainKernelServicesImpl constructor
        super(container, controllerService, persister, rootRegistration, operationValidator, legacyModelVersion, successfulBoot,
                bootError, extensionRegistry);
        this.contentRepository = contentRepository;
    }

    @Override
    public TransformedOperation transformOperation(ModelVersion modelVersion, ModelNode operation)
            throws OperationFailedException {
        //Will throw an error since we are not the main controller
        checkIsMainController();
        return null;
    }

    @Override
    public ModelNode readTransformedModel(ModelVersion modelVersion) {
        //Will throw an error since we are not the main controller
        checkIsMainController();
        return null;
    }

    @Override
    public ModelNode executeOperation(ModelVersion modelVersion, TransformedOperation op) {
        //Will throw an error since we are not the main controller
        checkIsMainController();
        return null;
    }

    @Override
    public ModelNode callReadMasterDomainModelHandler(ModelVersion modelVersion) {
        //Will throw an error since we are not the main controller
        checkIsMainController();
        return null;
    }

    @Override
    public void applyMasterDomainModel(ModelVersion modelVersion, List<IgnoreDomainResourceTypeResource> ignoredResources) {
        //Will throw an error since we are not the main controller
        checkIsMainController();
    }

    public void applyMasterDomainModel(ModelNode resources, List<IgnoreDomainResourceTypeResource> ignoredResources) {
        ModelNode applyDomainModel = new ModelNode();
        applyDomainModel.get(OP).set(ApplyRemoteMasterDomainModelHandler.OPERATION_NAME);
        //FIXME this makes the op work after boot (i.e. slave connects to restarted master), but does not make the slave resync the servers
        applyDomainModel.get(OPERATION_HEADERS, "execute-for-coordinator").set(true);
        applyDomainModel.get(OP_ADDR).setEmptyList();
        applyDomainModel.get(DOMAIN_MODEL).set(resources);

        //Simulate ApplyRemoteMasterDomainModelHandler
        final IgnoredDomainResourceRegistry ignoredResourceRegistry = createIgnoredDomainResourceRegistry(ignoredResources);
        ModelNode result = internalExecute(applyDomainModel, new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                try {
                    // We get the model as a list of resources descriptions
                    final ModelNode domainModel = operation.get(DOMAIN_MODEL);
                    final ModelNode startRoot = Resource.Tools.readModel(context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS));
                    final Resource rootResource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);

                    for (final ModelNode resourceDescription : domainModel.asList()) {

                        final PathAddress resourceAddress = PathAddress.pathAddress(resourceDescription.require("domain-resource-address"));
                        if (ignoredResourceRegistry.isResourceExcluded(resourceAddress)) {
                            continue;
                        }

                        final Resource resource = getResource(resourceAddress, rootResource, context);
                        if (resourceAddress.size() == 1 && resourceAddress.getElement(0).getKey().equals(EXTENSION)) {
                            // Extensions are handled in ApplyExtensionsHandler
                            continue;
                        }
                        resource.writeModel(resourceDescription.get("domain-resource-model"));
                    }

                    context.completeStep();
                }catch (Exception e) {
                    throw new OperationFailedException(e.getMessage());
                }
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
                                // Extensions are handled in ApplyExtensionsHandler
                                continue;
                            } else if (type.equals(MANAGEMENT_CLIENT_CONTENT) && element.getValue().equals(ROLLOUT_PLANS)) {
                                // Needs a specialized resource type
                                temp = new ManagedDMRContentTypeResource(element, ROLLOUT_PLAN, null, contentRepository);
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
        });

        if (!SUCCESS.equals(result.get(OUTCOME).asString())) {
            throw new RuntimeException(result.get(FAILURE_DESCRIPTION).asString());
        }
    }

    private IgnoredDomainResourceRegistry createIgnoredDomainResourceRegistry(List<IgnoreDomainResourceTypeResource> ignoredResources) {
        IgnoredDomainResourceRegistry reg = new IgnoredDomainResourceRegistry(new LocalHostControllerInfo() {

            @Override
            public boolean isMasterDomainController() {
                return false;
            }

            @Override
            public String getRemoteDomainControllerUsername() {
                return null;
            }

            public List<DiscoveryOption> getRemoteDomainControllerDiscoveryOptions() {
                return null;
            }

            @Override
            public State getProcessState() {
                return null;
            }

            @Override
            public String getNativeManagementSecurityRealm() {
                return null;
            }

            @Override
            public int getNativeManagementPort() {
                return 0;
            }

            @Override
            public String getNativeManagementInterface() {
                return null;
            }

            @Override
            public String getLocalHostName() {
                return null;
            }

            @Override
            public String getHttpManagementSecurityRealm() {
                return null;
            }

            @Override
            public int getHttpManagementSecurePort() {
                return 0;
            }

            @Override
            public int getHttpManagementPort() {
                return 0;
            }

            @Override
            public String getHttpManagementInterface() {
                return null;
            }
        });

        for (IgnoreDomainResourceTypeResource resource : ignoredResources) {
            reg.getRootResource().registerChild(PathElement.pathElement(ModelDescriptionConstants.IGNORED_RESOURCE_TYPE, resource.getName()), resource);
        }

        return reg;
    }
}
