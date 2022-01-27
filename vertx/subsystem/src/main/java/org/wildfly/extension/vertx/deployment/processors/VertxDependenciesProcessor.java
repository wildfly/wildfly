/*
 * Copyright (C) 2022 RedHat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.vertx.deployment.processors;

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
import org.wildfly.extension.vertx.deployment.VertxDeploymentAttachment;

import java.util.ArrayList;
import java.util.List;

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
    private static final String MODULE_IO_VERTX_CORE = "io.vertx.core";
    private static final String MODULE_IO_VERTX_INFINISPAN = "io.vertx.infinispan";
    private static final String MODULE_IO_VERTX_AUTH = "io.vertx.auth";
    private static final String MODULE_IO_VERTX_CLIENT = "io.vertx.client";

    @Override
    public void deploy(DeploymentPhaseContext context) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = context.getDeploymentUnit();
        if (!VertxDeploymentAttachment.isVertxDeployment(deploymentUnit)) {
            return;
        }
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        List<ModuleDependency> dependencies = new ArrayList<>();
        dependencies.add(new ModuleDependency(moduleLoader, MODULE_VERTX_EXTENSION, false, false, false, false));
        dependencies.add(new ModuleDependency(moduleLoader, MODULE_IO_VERTX_CORE, false, true, true, false));
        dependencies.add(new ModuleDependency(moduleLoader, MODULE_IO_VERTX_INFINISPAN, true, true, true, false));
        dependencies.add(new ModuleDependency(moduleLoader, MODULE_IO_VERTX_AUTH, true, true, true, false));
        dependencies.add(new ModuleDependency(moduleLoader, MODULE_IO_VERTX_CLIENT, true, true, true, false));
        moduleSpecification.addSystemDependencies(dependencies);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

}
