/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.naming.deployment;

import org.jboss.as.naming.logging.NamingLogger;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;

/**
 * A processor which adds jdk naming modules to deployments.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class JdkDependenciesProcessor implements DeploymentUnitProcessor {

    private static final String[] JDK_NAMING_MODULES = {
            "jdk.naming.dns",
            "jdk.naming.rmi"
    };

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        for (String moduleName : JDK_NAMING_MODULES) {
            try {
                moduleLoader.loadModule(moduleName);
                moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, moduleName).build());
            } catch (ModuleLoadException ex) {
                NamingLogger.ROOT_LOGGER.debugf("Module not found: %s", moduleName);
            }
        }
    }
}
