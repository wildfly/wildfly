/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.web.deployment;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.filter.PathFilters;

/**
 * Module dependencies processor.
 *
 * @author Emanuel Muckenhuber
 */
public class WarClassloadingDependencyProcessor implements DeploymentUnitProcessor {

    private static final ModuleIdentifier JAVAX_EE_API = ModuleIdentifier.create("javaee.api");

    private static final ModuleIdentifier JSF_IMPL = ModuleIdentifier.create("com.sun.jsf-impl");
    private static final ModuleIdentifier JSF_API = ModuleIdentifier.create("javax.faces.api");
    private static final ModuleIdentifier JSF_1_2_IMPL = ModuleIdentifier.create("com.sun.jsf-impl", "1.2");
    private static final ModuleIdentifier JSF_1_2_API = ModuleIdentifier.create("javax.faces.api", "1.2");
    private static final ModuleIdentifier BEAN_VALIDATION = ModuleIdentifier.create("org.hibernate.validator");

    private static final ModuleIdentifier JBOSS_WEB = ModuleIdentifier.create("org.jboss.as.web");

    static {
        Module.registerURLStreamHandlerFactoryModule(Module.forClass(WarClassloadingDependencyProcessor.class));
    }

    private static final Logger logger = Logger.getLogger(WarClassloadingDependencyProcessor.class);

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit topLevelDeployment = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        //we always make the JSF api available
        final String jsfVersion = JsfVersionMarker.getVersion(topLevelDeployment);

        if(jsfVersion.equals(JsfVersionMarker.JSF_1_2)) {
            moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JSF_1_2_API, false, false, false));
        } else {
            moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JSF_API, false, false, false));
        }

        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return; // Skip non web deployments
        }
        // Add module dependencies on Java EE apis

        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JAVAX_EE_API, false, false, false));

        // Add modules for JSF

        if(jsfVersion.equals(JsfVersionMarker.JSF_1_2)) {
            ModuleDependency jsf = new ModuleDependency(moduleLoader, JSF_1_2_IMPL, false, false, false);
            jsf.addImportFilter(PathFilters.getMetaInfFilter(), true);
            moduleSpecification.addSystemDependency(jsf);
        } else {
            if(!jsfVersion.equals(JsfVersionMarker.JSF_2_0)) {
                logger.warn("Ukown JSF version " + jsfVersion + " " + JsfVersionMarker.JSF_2_0 + " will be used instead");
            }
            ModuleDependency jsf = new ModuleDependency(moduleLoader, JSF_IMPL, false, false, false);
            jsf.addImportFilter(PathFilters.getMetaInfFilter(), true);
            moduleSpecification.addSystemDependency(jsf);
        }

        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, BEAN_VALIDATION, false, false, true));

        // FIXME we need to revise the exports of the web module, so that we
        // don't export our internals
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JBOSS_WEB, false, false, true));

    }

    public void undeploy(final DeploymentUnit context) {
    }
}
