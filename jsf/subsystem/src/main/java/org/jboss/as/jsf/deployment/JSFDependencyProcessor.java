/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.jsf.deployment;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.jsf.JSFLogger;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.LocalModuleLoader;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.filter.PathFilters;

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 * @author Stuart Douglas
 */
public class JSFDependencyProcessor implements DeploymentUnitProcessor {


 //   private static final ModuleIdentifier JSF_IMPL = ModuleIdentifier.create("com.sun.jsf-impl");
 //   private static final ModuleIdentifier JSF_API = ModuleIdentifier.create("javax.faces.api");
    private static final ModuleIdentifier JSF_SUBSYSTEM = ModuleIdentifier.create("org.jboss.as.jsf");
 //   private static final ModuleIdentifier JSF_INJECTION = ModuleIdentifier.create("org.jboss.as.jsf-injection");
 //   private static final ModuleIdentifier JSF_INJECTION_1_2 = ModuleIdentifier.create("org.jboss.as.jsf-injection", "1.2");
 //   private static final ModuleIdentifier JSF_1_2_IMPL = ModuleIdentifier.create("com.sun.jsf-impl", "1.2");
 //   private static final ModuleIdentifier JSF_1_2_API = ModuleIdentifier.create("javax.faces.api", "1.2");
    private static final ModuleIdentifier BEAN_VALIDATION = ModuleIdentifier.create("org.hibernate.validator");
    private static final ModuleIdentifier JSTL = ModuleIdentifier.create("javax.servlet.jstl.api");

    private JSFModuleIdFactory moduleIdFactory = JSFModuleIdFactory.getInstance();

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return;
        }
        if(JsfVersionMarker.getVersion(deploymentUnit).equals(JsfVersionMarker.WAR_BUNDLES_JSF_IMPL)) {
            //if JSF is provided by the application we leave it alone
            return;
        }
        //TODO: we should do that same check that is done in com.sun.faces.config.FacesInitializer
        //and only add the dependency if JSF is actually needed

        final DeploymentUnit topLevelDeployment = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        String jsfVersion = JsfVersionMarker.getVersion(topLevelDeployment);
        if (!moduleIdFactory.isValidJSFSlot(jsfVersion)) {
            JSFLogger.ROOT_LOGGER.unknownJSFVersion(jsfVersion);
            jsfVersion = JSFModuleIdFactory.DEFAULT_SLOT;
        }

        addJSFAPI(jsfVersion, moduleSpecification, moduleLoader);
        addJSFImpl(jsfVersion, moduleSpecification, moduleLoader);

        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JSTL, false, false, false, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, BEAN_VALIDATION, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JSF_SUBSYSTEM, false, false, true, false));

        addJSFInjection(jsfVersion, moduleSpecification, moduleLoader);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private void addJSFAPI(String jsfVersion, ModuleSpecification moduleSpecification, ModuleLoader moduleLoader) {
        if (jsfVersion.equals(JsfVersionMarker.WAR_BUNDLES_JSF_IMPL)) return;
        ModuleIdentifier jsfModule = moduleIdFactory.getApiModId(jsfVersion);
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, jsfModule, false, false, false, false));
    }

    private void addJSFImpl(String jsfVersion, ModuleSpecification moduleSpecification, ModuleLoader moduleLoader) {
        if (jsfVersion.equals(JsfVersionMarker.WAR_BUNDLES_JSF_IMPL)) return;
        ModuleIdentifier jsfModule = moduleIdFactory.getImplModId(jsfVersion);
        ModuleDependency jsf = new ModuleDependency(moduleLoader, jsfModule, false, false, false, false);
        jsf.addImportFilter(PathFilters.getMetaInfFilter(), true);
        moduleSpecification.addSystemDependency(jsf);
    }

    private void addJSFInjection(String jsfVersion, ModuleSpecification moduleSpecification, ModuleLoader moduleLoader) {
        if (jsfVersion.equals(JsfVersionMarker.WAR_BUNDLES_JSF_IMPL)) return;
        ModuleIdentifier jsfInjectionModule = moduleIdFactory.getInjectionModId(jsfVersion);
        ModuleDependency jsfInjectionDependency = new ModuleDependency(moduleLoader, jsfInjectionModule, false, true, true, false);
        moduleSpecification.addSystemDependency(jsfInjectionDependency);
    }
}
