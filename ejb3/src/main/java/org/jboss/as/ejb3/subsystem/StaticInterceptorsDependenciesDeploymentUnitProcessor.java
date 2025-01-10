/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;


import java.util.Collection;
import java.util.HashSet;

import org.jboss.as.ejb3.interceptor.server.ServerInterceptorMetaData;
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
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class StaticInterceptorsDependenciesDeploymentUnitProcessor implements DeploymentUnitProcessor {

    final Collection<String> interceptorModules = new HashSet<>();

    public StaticInterceptorsDependenciesDeploymentUnitProcessor(final Collection<ServerInterceptorMetaData> serverInterceptors){
        for(final ServerInterceptorMetaData si: serverInterceptors){
            interceptorModules.add(si.getModule());
        }
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        final ModuleSpecification deploymentModuleSpec = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);

        for (String interceptorModule : interceptorModules) {
            deploymentModuleSpec.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, interceptorModule).setImportServices(true).build());
        }
    }
}
