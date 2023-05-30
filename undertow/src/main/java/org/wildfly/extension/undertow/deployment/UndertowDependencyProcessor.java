/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.deployment;

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
 * Module dependencies processor.
 *
 * @author Emanuel Muckenhuber
 * @author Stan Silvert
 */
public class UndertowDependencyProcessor implements DeploymentUnitProcessor {

    private static final String JSTL = "jakarta.servlet.jstl.api";

    private static final String UNDERTOW_CORE = "io.undertow.core";
    private static final String UNDERTOW_SERVLET = "io.undertow.servlet";
    private static final String UNDERTOW_JSP = "io.undertow.jsp";
    private static final String UNDERTOW_WEBSOCKET = "io.undertow.websocket";

    private static final String SERVLET_API = "jakarta.servlet.api";
    private static final String JSP_API = "jakarta.servlet.jsp.api";
    private static final String WEBSOCKET_API = "jakarta.websocket.api";

    static {
        Module module = Module.forClass(UndertowDependencyProcessor.class);
        if (module != null) {
            //When testing the subsystems we are running in a non-modular environment
            //so module will be null. Having a null entry kills ModularURLStreamHandlerFactory
            Module.registerURLStreamHandlerFactoryModule(module);
        }
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        //add the api classes for every deployment
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, SERVLET_API, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JSP_API, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, WEBSOCKET_API, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JSTL, false, false, false, false));

        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return; // Skip non web deployments
        }

        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, UNDERTOW_CORE, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, UNDERTOW_SERVLET, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, UNDERTOW_JSP, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, UNDERTOW_WEBSOCKET, false, false, true, false));
    }
}
