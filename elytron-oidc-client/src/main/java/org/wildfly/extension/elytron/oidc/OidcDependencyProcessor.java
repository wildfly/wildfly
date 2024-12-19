/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;

/**
 * A {link {@link DeploymentUnitProcessor} to add the required dependencies to activate OpenID Connect.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
class OidcDependencyProcessor implements DeploymentUnitProcessor {

    private static final String ELYTRON_HTTP_OIDC = "org.wildfly.security.elytron-http-oidc";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!OidcDeploymentMarker.isOidcDeployment(deploymentUnit)) {
            return;
        }

        ModuleLoader moduleLoader = Module.getBootModuleLoader();
        ModuleSpecification moduleSpec = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        moduleSpec.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, ELYTRON_HTTP_OIDC).setImportServices(true).build());
    }
}
