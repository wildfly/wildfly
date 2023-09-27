/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import static org.jboss.as.server.security.VirtualDomainUtil.OIDC_VIRTUAL_SECURITY_DOMAIN_CREATION_SERVICE;
import static org.wildfly.extension.elytron.oidc.ElytronOidcSubsystemDefinition.installService;
import static org.wildfly.extension.elytron.oidc._private.ElytronOidcLogger.ROOT_LOGGER;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;

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
        OidcConfigService.getInstance().clear();
        ServiceTarget target = context.getServiceTarget();
        installService(OIDC_VIRTUAL_SECURITY_DOMAIN_CREATION_SERVICE, new OidcVirtualSecurityDomainCreationService(), target);

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

    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, Resource resource) {
        context.removeService(OIDC_VIRTUAL_SECURITY_DOMAIN_CREATION_SERVICE);
    }

}
