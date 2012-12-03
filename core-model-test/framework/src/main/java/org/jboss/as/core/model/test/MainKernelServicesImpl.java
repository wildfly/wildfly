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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;

import java.util.Collections;
import java.util.Map;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.validation.OperationValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationTransformerRegistry;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.OperationTransformer.TransformedOperation;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.as.controller.transform.TransformationTargetImpl;
import org.jboss.as.controller.transform.TransformerRegistry;
import org.jboss.as.controller.transform.Transformers;
import org.jboss.as.domain.controller.operations.ReadMasterDomainModelHandler;
import org.jboss.as.model.test.ModelTestModelControllerService;
import org.jboss.as.model.test.StringConfigurationPersister;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceContainer;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MainKernelServicesImpl extends AbstractKernelServicesImpl {
    private final ExtensionRegistry extensionRegistry;

    public MainKernelServicesImpl(ServiceContainer container, ModelTestModelControllerService controllerService,
            StringConfigurationPersister persister, ManagementResourceRegistration rootRegistration,
            OperationValidator operationValidator, ModelVersion legacyModelVersion, boolean successfulBoot, Throwable bootError,
            ExtensionRegistry extensionRegistry) {
        // FIXME MainKernelServicesImpl constructor
        super(container, controllerService, persister, rootRegistration, operationValidator, legacyModelVersion, successfulBoot,
                bootError, extensionRegistry);
        this.extensionRegistry = extensionRegistry;
    }

    public TransformedOperation transformOperation(ModelVersion modelVersion, ModelNode operation) throws OperationFailedException {
        checkIsMainController();
        PathAddress opAddr = PathAddress.pathAddress(operation.get(OP_ADDR));
        TransformerRegistry transformerRegistry = extensionRegistry.getTransformerRegistry();

        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        Map<PathAddress, ModelVersion> subsystemVersions = Collections.<PathAddress, ModelVersion>emptyMap();
        OperationTransformerRegistry registry = transformerRegistry.resolveHost(modelVersion, subsystemVersions);

        //TODO Initialise this
        TransformationTarget target = TransformationTargetImpl.create(extensionRegistry.getTransformerRegistry(), modelVersion,
                subsystemVersions, MOCK_IGNORED_DOMAIN_RESOURCE_REGISTRY, TransformationTarget.TransformationTargetType.DOMAIN);
        TransformationContext transformationContext = createTransformationContext(target);

        OperationTransformer operationTransformer = registry.resolveOperationTransformer(address, operation.get(OP).asString()).getTransformer();
        if (operationTransformer != null) {
            return operationTransformer.transformOperation(transformationContext, address, operation);
        }
        return new OperationTransformer.TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
    }

    public ModelNode readTransformedModel(ModelVersion modelVersion) {
        checkIsMainController();


        final TransformationTarget target = TransformationTargetImpl.create(extensionRegistry.getTransformerRegistry(), modelVersion,
                Collections.<PathAddress, ModelVersion>emptyMap(), MOCK_IGNORED_DOMAIN_RESOURCE_REGISTRY, TransformationTarget.TransformationTargetType.DOMAIN);
        final Transformers transformers = Transformers.Factory.create(target);

        ModelNode result = internalExecute(new ModelNode(), new ReadMasterDomainModelHandler(transformers));
        if (FAILED.equals(result.get(OUTCOME).asString())) {
            throw new RuntimeException(result.get(FAILURE_DESCRIPTION).asString());
        }

        ModelNode domainModel = new ModelNode();
        //Reassemble the model from the reead master domain model handler result
        for (ModelNode entry : result.get(RESULT).asList()) {
            PathAddress address = PathAddress.pathAddress(entry.require("domain-resource-address"));
            ModelNode toSet = domainModel;
            for (PathElement pathElement : address) {
                toSet = toSet.get(pathElement.getKey(), pathElement.getValue());
            }
            toSet.set(entry.require("domain-resource-model"));
        }
        return domainModel;
    }

    public ModelNode executeOperation(final ModelVersion modelVersion, final TransformedOperation op) {

        throw new IllegalStateException("NYI");
    }

    private static  TransformationTarget.IgnoredTransformationRegistry MOCK_IGNORED_DOMAIN_RESOURCE_REGISTRY = new  TransformationTarget.IgnoredTransformationRegistry() {

        @Override
        public boolean isResourceTransformationIgnored(PathAddress address) {
            return false;
        }

        @Override
        public boolean isOperationTransformationIgnored(PathAddress address) {
            return false;
        }
    };
}
