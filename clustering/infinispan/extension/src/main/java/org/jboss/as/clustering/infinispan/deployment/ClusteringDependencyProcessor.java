/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.deployment;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;

/**
 * {@link DeploymentUnitProcessor} that adds the clustering api to the deployment classpath.
 * @author Paul Ferraro
 */
public class ClusteringDependencyProcessor implements DeploymentUnitProcessor {

    private static final String API = "org.wildfly.clustering.server.api";
    private static final String MARSHALLING_API = "org.wildfly.clustering.marshalling.api";
    private static final String SINGLETON_API = "org.wildfly.clustering.singleton.api";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, API).setOptional(true).build());
        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, MARSHALLING_API).setOptional(true).build());
        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, SINGLETON_API).setOptional(true).build());
    }
}