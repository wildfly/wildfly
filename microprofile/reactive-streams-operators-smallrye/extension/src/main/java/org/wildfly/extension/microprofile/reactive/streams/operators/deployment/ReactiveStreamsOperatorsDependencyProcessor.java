/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.reactive.streams.operators.deployment;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ReactiveStreamsOperatorsDependencyProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        addModuleDependencies(deploymentUnit);
    }

    private void addModuleDependencies(DeploymentUnit deploymentUnit) {
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, "org.eclipse.microprofile.reactive-streams-operators.core").setImportServices(true).build());
        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, "org.eclipse.microprofile.reactive-streams-operators.api").setImportServices(true).build());
        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, "org.reactivestreams").setImportServices(true).build());
        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, "io.smallrye.reactive.mutiny.reactive-streams-operators").setImportServices(true).build());
        ModuleDependency moduleDependency = cdiDependency(ModuleDependency.Builder.of(moduleLoader, "org.wildfly.reactive.mutiny.reactive-streams-operators.cdi-provider").setImportServices(true).build());
        moduleSpecification.addSystemDependency(moduleDependency);

        // Converters
        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, "io.smallrye.reactive.converters.api").setOptional(true).build());
        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, "io.smallrye.reactive.converters.rxjava2").setOptional(true).setImportServices(true).build());
    }


    private ModuleDependency cdiDependency(ModuleDependency moduleDependency) {
        // This is needed following https://issues.redhat.com/browse/WFLY-13641 / https://github.com/wildfly/wildfly/pull/13406
        moduleDependency.addImportFilter(s -> s.equals("META-INF"), true);
        return moduleDependency;
    }

}
