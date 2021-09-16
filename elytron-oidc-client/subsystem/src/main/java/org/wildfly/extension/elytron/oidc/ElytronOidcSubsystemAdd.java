/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.elytron.oidc;

import static org.wildfly.extension.elytron.oidc._private.ElytronOidcLogger.ROOT_LOGGER;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;

/**
 * Add handler for the Elytron OpenID Connect subsystem.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
class ElytronOidcSubsystemAdd extends AbstractBoottimeAddStepHandler {

    private static final int PARSE_OIDC_DETECTION = 0x4C0E;
    private static final int PARSE_DEFINE_VIRTUAL_HTTP_SERVER_MECHANISM_FACTORY_NAME = 0x4C18;
    private static final int DEPENDENCIES_OIDC = 0x1910;
    private static final int INSTALL_VIRTUAL_HTTP_SERVER_MECHANISM_FACTORY = 0x3100;

    ElytronOidcSubsystemAdd() {
    }

    @Override
    public void performBoottime(OperationContext context, ModelNode operation, ModelNode model) {
        ROOT_LOGGER.activatingSubsystem();

        if (context.isNormalServer()) {
            context.addStep(new AbstractDeploymentChainStep() {

                @Override
                protected void execute(DeploymentProcessorTarget processorTarget) {
                    processorTarget.addDeploymentProcessor(ElytronOidcExtension.SUBSYSTEM_NAME, Phase.PARSE, PARSE_OIDC_DETECTION, new OidcActivationProcessor());
                    processorTarget.addDeploymentProcessor(ElytronOidcExtension.SUBSYSTEM_NAME, Phase.PARSE, PARSE_DEFINE_VIRTUAL_HTTP_SERVER_MECHANISM_FACTORY_NAME, new VirtualHttpServerMechanismFactoryNameProcessor());
                    processorTarget.addDeploymentProcessor(ElytronOidcExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, DEPENDENCIES_OIDC, new OidcDependencyProcessor());
                    processorTarget.addDeploymentProcessor(ElytronOidcExtension.SUBSYSTEM_NAME, Phase.INSTALL, INSTALL_VIRTUAL_HTTP_SERVER_MECHANISM_FACTORY, new VirtualHttpServerMechanismFactoryProcessor());
                }

            }, OperationContext.Stage.RUNTIME);
        }

    }
}
