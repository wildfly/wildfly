/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.beanvalidation;

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
 * Processor which adds each Jakarta Bean Validation API module as dependency to all deployments.
 *
 * @author Eduardo Martins
 */
public class BeanValidationDeploymentDependenciesProcessor implements DeploymentUnitProcessor {

    private static final String[] DEPENDENCIES = {
            "org.hibernate.validator",
            "jakarta.validation.api",
    };

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        for (String moduleIdentifier : DEPENDENCIES) {
            moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, moduleIdentifier).setOptional(true).setImportServices(true).build());
        }
    }
}
