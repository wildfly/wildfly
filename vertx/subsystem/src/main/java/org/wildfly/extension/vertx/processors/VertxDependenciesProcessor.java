/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.vertx.processors;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;

import java.util.Set;

/**
 *
 * Processor that is used to add necessary module dependencies.
 *
 * @author <a href="aoingl@gmail.com">Lin Gao</a>
 */
public class VertxDependenciesProcessor implements DeploymentUnitProcessor {

    public static final Phase PHASE = Phase.DEPENDENCIES;

    /**
     * The relative order of this processor within the {@link #PHASE}.
     * The current number is large enough for it to happen after all
     * the standard deployment unit processors that come with JBoss AS.
     */
    public static final int PRIORITY = 0x4000;

    private static final String MODULE_VERTX_EXTENSION = "org.wildfly.extension.vertx";
    private static final String MODULE_VERTX_CORE = "io.vertx.core";
    private static final String MODULE_VERTX_MUTINY_CORE = "io.smallrye.reactive.mutiny.vertx-core";
    private static final String MODULE_MUTINY = "io.smallrye.reactive.mutiny";
    private static final String MODULE_SMALLRYE_COMMON_ANNOTATION = "io.smallrye.common.annotation";

    @Override
    public void deploy(DeploymentPhaseContext context) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = context.getDeploymentUnit();
        if (!VertxDeploymentAttachment.isVertxDeployment(deploymentUnit)) {
            return;
        }
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        moduleSpecification.addSystemDependencies(Set.of(
                new ModuleDependency(moduleLoader, MODULE_VERTX_EXTENSION, false, true, true, false),
                new ModuleDependency(moduleLoader, MODULE_VERTX_CORE, false, true, true, false),
                new ModuleDependency(moduleLoader, MODULE_VERTX_MUTINY_CORE, false, true, true, false),
                new ModuleDependency(moduleLoader, MODULE_MUTINY, false, true, true, false),
                new ModuleDependency(moduleLoader, MODULE_SMALLRYE_COMMON_ANNOTATION, true, false, false, false)
        ));
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

}
