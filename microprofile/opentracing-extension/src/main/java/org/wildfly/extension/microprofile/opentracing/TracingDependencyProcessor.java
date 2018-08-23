/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.opentracing;

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

public class TracingDependencyProcessor implements DeploymentUnitProcessor {
    private static final String[] MODULES = {
            "io.jaegertracing.jaeger-core",
            "io.jaegertracing.jaeger-thrift",
            "io.opentracing.contrib.opentracing-tracerresolver",
            "io.opentracing.opentracing-api",
            "io.opentracing.opentracing-util",
            "org.eclipse.microprofile.opentracing",
    };

    private static final String[] EXPORTED_MODULES = {
            "io.smallrye.opentracing",
            "org.wildfly.microprofile.opentracing-smallrye",
    };

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        addDependencies(deploymentUnit);
    }

    private void addDependencies(DeploymentUnit deploymentUnit) {
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }

        ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        ModuleLoader moduleLoader = Module.getBootModuleLoader();
        for (String module : MODULES) {
            moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, module, false, false, true, false));
        }
        for (String module : EXPORTED_MODULES) {
            moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, module, false, true, true, false));
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
