/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.eesecurity;

import static org.wildfly.extension.eesecurity.EESecuritySubsystemDefinition.ELYTRON_JAKARTA_SECURITY;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;

class EESecurityDependencyProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        final DeploymentUnit top = unit.getParent() == null ? unit : unit.getParent();
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        final ModuleSpecification moduleSpec = unit.getAttachment(Attachments.MODULE_SPECIFICATION);
        moduleSpec.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, "jakarta.security.enterprise.api").setImportServices(true).build());

        Boolean securityPresent = top.getAttachment(EESecurityAnnotationProcessor.SECURITY_PRESENT);
        if (securityPresent != null && securityPresent) {
            moduleSpec.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, ELYTRON_JAKARTA_SECURITY).setImportServices(true).build());
        }

    }
}
