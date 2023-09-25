/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi;

import java.util.function.Consumer;

import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.microprofile.openapi.deployment.OpenAPIDependencyProcessor;
import org.wildfly.extension.microprofile.openapi.deployment.OpenAPIDocumentProcessor;
import org.wildfly.extension.microprofile.openapi.logging.MicroProfileOpenAPILogger;

/**
 * @author Paul Ferraro
 */
public class MicroProfileOpenAPIServiceHandler implements ResourceServiceHandler, Consumer<DeploymentProcessorTarget> {

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        MicroProfileOpenAPILogger.LOGGER.activatingSubsystem();
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {
    }

    @Override
    public void accept(DeploymentProcessorTarget target) {
        target.addDeploymentProcessor(MicroProfileOpenAPIExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_MICROPROFILE_OPENAPI, new OpenAPIDependencyProcessor());
        target.addDeploymentProcessor(MicroProfileOpenAPIExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.POST_MODULE_MICROPROFILE_OPENAPI, new OpenAPIDocumentProcessor());
    }
}
