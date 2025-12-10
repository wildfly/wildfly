/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.openapi.deployment;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;

/**
 * Adds MicroProfile OpenAPI dependencies to deployment.
 *
 * @author Michael Edgar
 * @author Paul Ferraro
 */
public class OpenAPIDependencyProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext context) {
        DeploymentUnit unit = context.getDeploymentUnit();

        if (DeploymentTypeMarker.isType(DeploymentType.WAR, unit)) {
            ModuleSpecification specification = unit.getAttachment(Attachments.MODULE_SPECIFICATION);
            ModuleLoader loader = Module.getBootModuleLoader();

            specification.addSystemDependency(ModuleDependency.Builder.of(loader, "org.eclipse.microprofile.openapi.api").build());
        }
    }
}
