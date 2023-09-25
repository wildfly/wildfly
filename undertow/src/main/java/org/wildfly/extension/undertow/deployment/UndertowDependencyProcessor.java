/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
