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
import org.jboss.as.web.WebLogger;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.filter.PathFilters;

/**
 * Module dependencies processor.
 *
 * @author Emanuel Muckenhuber
 * @author Stan Silvert
 */
public class WarClassloadingDependencyProcessor implements DeploymentUnitProcessor {

    private static final ModuleIdentifier JAVAX_EE_API = ModuleIdentifier.create("javaee.api");

    private static final ModuleIdentifier JSF_IMPL = ModuleIdentifier.create("com.sun.jsf-impl");
    private static final ModuleIdentifier JSF_API = ModuleIdentifier.create("javax.faces.api");
    private static final ModuleIdentifier JSF_1_2_IMPL = ModuleIdentifier.create("com.sun.jsf-impl", "1.2");
    private static final ModuleIdentifier JSF_1_2_API = ModuleIdentifier.create("javax.faces.api", "1.2");
    private static final ModuleIdentifier BEAN_VALIDATION = ModuleIdentifier.create("org.hibernate.validator");
    private static final ModuleIdentifier JSTL = ModuleIdentifier.create("javax.servlet.jstl.api");

    private static final ModuleIdentifier JBOSS_WEB = ModuleIdentifier.create("org.jboss.as.web");

    static {
        Module.registerURLStreamHandlerFactoryModule(Module.forClass(WarClassloadingDependencyProcessor.class));
    }

    private static final Logger logger = Logger.getLogger(WarClassloadingDependencyProcessor.class);

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return; // Skip non web deployments
        }

        final DeploymentUnit topLevelDeployment = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        final String jsfVersion = JsfVersionMarker.getVersion(topLevelDeployment);

        addJSFAPI(jsfVersion, moduleSpecification, moduleLoader);

        // Add module dependencies on Java EE apis
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JAVAX_EE_API, false, false, false, false));

        addJSFImpl(jsfVersion, moduleSpecification, moduleLoader);

        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JSTL, false, false, false, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, BEAN_VALIDATION, false, false, true, false));

        // FIXME we need to revise the exports of the web module, so that we
        // don't export our internals
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JBOSS_WEB, false, false, true, false));

    }

    private void addJSFAPI(String jsfVersion, ModuleSpecification moduleSpecification, ModuleLoader moduleLoader) {
        if (jsfVersion.equals(JsfVersionMarker.WAR_BUNDLES_JSF_IMPL)) return;

        ModuleIdentifier jsfModule = JSF_API;
        if (jsfVersion.equals(JsfVersionMarker.JSF_1_2)) jsfModule = JSF_1_2_API;
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, jsfModule, false, false, false, false));
    }

    private void addJSFImpl(String jsfVersion, ModuleSpecification moduleSpecification, ModuleLoader moduleLoader) {
        if (jsfVersion.equals(JsfVersionMarker.WAR_BUNDLES_JSF_IMPL)) return;

        ModuleIdentifier jsfModule = null;
        if (jsfVersion.equals(JsfVersionMarker.JSF_1_2)) jsfModule = JSF_1_2_IMPL;
        if (jsfVersion.equals(JsfVersionMarker.JSF_2_0)) jsfModule = JSF_IMPL;
        if (jsfModule == null) {
            jsfModule = JSF_IMPL;
            WebLogger.WEB_LOGGER.unknownJSFVersion(jsfVersion, JsfVersionMarker.JSF_2_0);
        }

        ModuleDependency jsf = new ModuleDependency(moduleLoader, jsfModule, false, false, false, false);
        jsf.addImportFilter(PathFilters.getMetaInfFilter(), true);
        moduleSpecification.addSystemDependency(jsf);
    }

    public void undeploy(final DeploymentUnit context) {
    }
}
