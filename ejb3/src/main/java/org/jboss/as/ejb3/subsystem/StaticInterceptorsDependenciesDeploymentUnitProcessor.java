/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
        for(final String interceptorModule : interceptorModules) {
            deploymentModuleSpec.addSystemDependency(new ModuleDependency(moduleLoader, interceptorModule, false, false, true, false));
        }
    }
}
