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

import com.sun.faces.flow.FlowCDIExtension;
import com.sun.faces.flow.FlowDiscoveryCDIExtension;
import com.sun.faces.application.view.ViewScopeExtension;

import javax.enterprise.inject.spi.Extension;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.jsf.JSFLogger;
import org.jboss.as.jsf.JSFMessages;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.filter.PathFilters;
import org.jboss.weld.bootstrap.spi.Metadata;

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 * @author Stuart Douglas
 */
public class JSFDependencyProcessor implements DeploymentUnitProcessor {

    private static final ModuleIdentifier JSF_SUBSYSTEM = ModuleIdentifier.create("org.jboss.as.jsf");
    private static final ModuleIdentifier BEAN_VALIDATION = ModuleIdentifier.create("org.hibernate.validator");
    private static final ModuleIdentifier JSTL = ModuleIdentifier.create("javax.servlet.jstl.api");

    private JSFModuleIdFactory moduleIdFactory = JSFModuleIdFactory.getInstance();

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return;
        }
        if (JsfVersionMarker.getVersion(deploymentUnit).equals(JsfVersionMarker.WAR_BUNDLES_JSF_IMPL)) {
            //if JSF is provided by the application we leave it alone
            return;
        }
        //TODO: we should do that same check that is done in com.sun.faces.config.FacesInitializer
        //and only add the dependency if JSF is actually needed

        final DeploymentUnit topLevelDeployment = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        String defaultJsfVersion = JSFModuleIdFactory.getInstance().getDefaultSlot();
        String jsfVersion = JsfVersionMarker.getVersion(topLevelDeployment);
        if (!moduleIdFactory.isValidJSFSlot(jsfVersion)) {
            JSFLogger.ROOT_LOGGER.unknownJSFVersion(jsfVersion, defaultJsfVersion);
            jsfVersion = defaultJsfVersion;
        }

        if (jsfVersion.equals(defaultJsfVersion) && !moduleIdFactory.isValidJSFSlot(jsfVersion)) {
            throw JSFMessages.MESSAGES.invalidDefaultJSFImpl(defaultJsfVersion);
        }

        addJSFAPI(jsfVersion, moduleSpecification, moduleLoader);
        addJSFImpl(jsfVersion, moduleSpecification, moduleLoader, topLevelDeployment);

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

    private void addJSFImpl(String jsfVersion,
                            ModuleSpecification moduleSpecification,
                            ModuleLoader moduleLoader,
                            DeploymentUnit topLevelDeployment) {
        if (jsfVersion.equals(JsfVersionMarker.WAR_BUNDLES_JSF_IMPL)) return;
        
        ModuleIdentifier jsfModule = moduleIdFactory.getImplModId(jsfVersion);
        ModuleDependency jsfImpl = new ModuleDependency(moduleLoader, jsfModule, false, false, false, false);
        jsfImpl.addImportFilter(PathFilters.getMetaInfFilter(), true);
        moduleSpecification.addSystemDependency(jsfImpl);

        // HACK!! Determine if we are using Mojarra 2.2 or greater
        try {
            jsfImpl.getModuleLoader().loadModule(jsfModule).getClassLoader().loadClass("com.sun.faces.flow.FlowCDIExtension");
        } catch (Exception e) {
            // If we can't load FlowCDIExtension then we must be using MyFaces or a pre-2.2 Mojarra impl.
            return;
        }

        // If using Mojarra 2.2 or greater, enable CDI Extensions
        addCDIExtensions(topLevelDeployment);
    }

    private void addJSFInjection(String jsfVersion, ModuleSpecification moduleSpecification, ModuleLoader moduleLoader) {
        if (jsfVersion.equals(JsfVersionMarker.WAR_BUNDLES_JSF_IMPL)) return;

        ModuleIdentifier jsfInjectionModule = moduleIdFactory.getInjectionModId(jsfVersion);
        ModuleDependency jsfInjectionDependency = new ModuleDependency(moduleLoader, jsfInjectionModule, false, true, true, false);
        moduleSpecification.addSystemDependency(jsfInjectionDependency);
    }

    // HACK!!! CDI Extensions should be automatically loaded from the Weld subsystem.  For now, CDI Extensions are only
    // recognized if the jar containing the service resides in the deployment.  Since Weld subsystem doesn't handle this yet,
    // we do it here.
    private void addCDIExtensions(DeploymentUnit topLevelDeployment) {
        final ClassLoader classLoader = SecurityActions.getContextClassLoader();
        try {
            SecurityActions.setContextClassLoader(FlowCDIExtension.class.getClassLoader());

            Metadata<Extension> metadata = new CDIExtensionMetadataImpl(new FlowCDIExtension());
            topLevelDeployment.addToAttachmentList(WeldAttachments.PORTABLE_EXTENSIONS, metadata);

            metadata = new CDIExtensionMetadataImpl(new ViewScopeExtension());
            topLevelDeployment.addToAttachmentList(WeldAttachments.PORTABLE_EXTENSIONS, metadata);

            metadata = new CDIExtensionMetadataImpl(new FlowDiscoveryCDIExtension());
            topLevelDeployment.addToAttachmentList(WeldAttachments.PORTABLE_EXTENSIONS, metadata);
        } finally {
            SecurityActions.setContextClassLoader(classLoader);
        }
    }

    private static class CDIExtensionMetadataImpl implements Metadata<Extension> {

        private final Extension ext;

        public CDIExtensionMetadataImpl(Extension ext) {
            this.ext = ext;
        }

        @Override
        public Extension getValue() {
            return ext;
        }

        @Override
        public String getLocation() {
            return ext.getClass().getName();
        }
    }
}
