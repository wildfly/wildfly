/*
 * JBoss, Home of Professional Open Source. Copyright 2019, Red Hat, Inc., and
 * individual contributors as indicated by the @author tags. See the
 * copyright.txt file in the distribution for a full listing of individual
 * contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.wildfly.extension.microprofile.openapi;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.microprofile.openapi._private.MicroProfileOpenAPILogger;
import org.wildfly.extension.microprofile.openapi.deployment.DependencyProcessor;
import org.wildfly.extension.microprofile.openapi.deployment.DeploymentProcessor;

/**
 * @author Michael Edgar
 */
class MicroProfileOpenAPISubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final MicroProfileOpenAPISubsystemAdd INSTANCE = new MicroProfileOpenAPISubsystemAdd();

    private MicroProfileOpenAPISubsystemAdd() {
        super(MicroProfileOpenAPISubsystemDefinition.ATTRIBUTES);
    }

    @Override
    protected void performBoottime(OperationContext context,
                                   ModelNode operation,
                                   ModelNode model) throws OperationFailedException {
        super.performBoottime(context, operation, model);

        String server = MicroProfileOpenAPISubsystemDefinition.SERVER.resolveModelAttribute(context, model).asString();
        String virtualHost = MicroProfileOpenAPISubsystemDefinition.VIRTUAL_HOST.resolveModelAttribute(context, model).asString();
        OpenAPIContextService service = OpenAPIContextService.install(context, server, virtualHost);

        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(MicroProfileOpenAPIExtension.SUBSYSTEM_NAME,
                                                       Phase.DEPENDENCIES,
                                                       Phase.DEPENDENCIES_MICROPROFILE_OPENAPI,
                                                       new DependencyProcessor());
                processorTarget.addDeploymentProcessor(MicroProfileOpenAPIExtension.SUBSYSTEM_NAME,
                                                       Phase.POST_MODULE,
                                                       Phase.POST_MODULE_MICROPROFILE_OPENAPI,
                                                       new DeploymentProcessor(service));
            }
        }, OperationContext.Stage.RUNTIME);

        MicroProfileOpenAPILogger.LOGGER.activatingSubsystem();
    }
}
