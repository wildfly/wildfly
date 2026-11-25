/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.jwt.smallrye;

import static org.wildfly.extension.microprofile.jwt.smallrye.MicroProfileSubsystemDefinition.EE_SECURITY_IMPL;

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
 * A {link {@link DeploymentUnitProcessor} to add the required dependencies to activate MicroProfile JWT.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class JwtDependencyProcessor implements DeploymentUnitProcessor {

    private static final String EE_SECURITY_API = "jakarta.security.enterprise.api";
    private static final String MP_JWT_API = "org.eclipse.microprofile.jwt.auth.api";
    private static final String SMALLRYE_JWT = "io.smallrye.jwt";
    private static final String ELYTRON_JWT = "org.wildfly.security.elytron-jwt";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!JwtDeploymentMarker.isJWTDeployment(deploymentUnit)) {
            return;
        }

        ModuleLoader moduleLoader = Module.getBootModuleLoader();
        ModuleSpecification moduleSpec = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        moduleSpec.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, EE_SECURITY_API).setImportServices(true).build());
        moduleSpec.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, EE_SECURITY_IMPL).setImportServices(true).build());
        moduleSpec.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, MP_JWT_API).setImportServices(true).build());
        moduleSpec.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, SMALLRYE_JWT).setImportServices(true).build());
        moduleSpec.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, ELYTRON_JWT).setImportServices(true).build());
    }
}
