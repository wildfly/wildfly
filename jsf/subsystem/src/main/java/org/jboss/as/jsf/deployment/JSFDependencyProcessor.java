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

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.jsf.logging.JSFLogger;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.filter.PathFilters;

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 * @author Stuart Douglas
 */
public class JSFDependencyProcessor implements DeploymentUnitProcessor {
    public static final String IS_CDI_PARAM = "org.jboss.jbossfaces.IS_CDI";

    private static final ModuleIdentifier JSF_SUBSYSTEM = ModuleIdentifier.create("org.jboss.as.jsf");

    private JSFModuleIdFactory moduleIdFactory = JSFModuleIdFactory.getInstance();

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit tl = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();

        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        if(JsfVersionMarker.isJsfDisabled(deploymentUnit)) {
            addJSFAPI(JsfVersionMarker.JSF_2_0, moduleSpecification, moduleLoader);
            return;
        }
        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit) && !DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }
        String jsfVersion = JsfVersionMarker.getVersion(tl);
        if (jsfVersion.equals(JsfVersionMarker.WAR_BUNDLES_JSF_IMPL)) {
            //if JSF is provided by the application we leave it alone
            return;
        }
        //TODO: we should do that same check that is done in com.sun.faces.config.FacesInitializer
        //and only add the dependency if JSF is actually needed

        String defaultJsfVersion = JSFModuleIdFactory.getInstance().getDefaultSlot();
        if (!moduleIdFactory.isValidJSFSlot(jsfVersion)) {
            JSFLogger.ROOT_LOGGER.unknownJSFVersion(jsfVersion, defaultJsfVersion);
            jsfVersion = defaultJsfVersion;
        }

        if (jsfVersion.equals(defaultJsfVersion) && !moduleIdFactory.isValidJSFSlot(jsfVersion)) {
            throw JSFLogger.ROOT_LOGGER.invalidDefaultJSFImpl(defaultJsfVersion);
        }

        addJSFAPI(jsfVersion, moduleSpecification, moduleLoader);
        addJSFImpl(jsfVersion, moduleSpecification, moduleLoader);

        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JSF_SUBSYSTEM, false, false, true, false));

        addJSFInjection(jsfVersion, moduleSpecification, moduleLoader);

        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if(warMetaData != null) {
            addCDIFlag(warMetaData, deploymentUnit);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private void addJSFAPI(String jsfVersion, ModuleSpecification moduleSpecification, ModuleLoader moduleLoader) {
        if (jsfVersion.equals(JsfVersionMarker.WAR_BUNDLES_JSF_IMPL)) return;

        ModuleIdentifier jsfModule = moduleIdFactory.getApiModId(jsfVersion);
        ModuleDependency jsfAPI = new ModuleDependency(moduleLoader, jsfModule, false, false, false, false);
        moduleSpecification.addSystemDependency(jsfAPI);
    }

    // Is JSF spec greater than 1.1?  If we add JSF 1.1 support we'll need this to keep from calling addJSFInjection()
  /*  private boolean isJSFSpecOver1_1(ModuleIdentifier jsfModule, ModuleDependency jsfAPI) throws DeploymentUnitProcessingException {
        try {
            return (jsfAPI.getModuleLoader().loadModule(jsfModule).getClassLoader().getResource("/javax/faces/component/ActionSource2.class") != null);
        } catch (ModuleLoadException e) {
            throw new DeploymentUnitProcessingException(e);
        }
    } */

    private void addJSFImpl(String jsfVersion,
            ModuleSpecification moduleSpecification,
            ModuleLoader moduleLoader) {
        if (jsfVersion.equals(JsfVersionMarker.WAR_BUNDLES_JSF_IMPL)) return;

        ModuleIdentifier jsfModule = moduleIdFactory.getImplModId(jsfVersion);
        ModuleDependency jsfImpl = new ModuleDependency(moduleLoader, jsfModule, false, false, true, false);
        jsfImpl.addImportFilter(PathFilters.getMetaInfFilter(), true);
        moduleSpecification.addSystemDependency(jsfImpl);
    }

    private void addJSFInjection(String jsfVersion, ModuleSpecification moduleSpecification, ModuleLoader moduleLoader)
            throws DeploymentUnitProcessingException {
        if (jsfVersion.equals(JsfVersionMarker.WAR_BUNDLES_JSF_IMPL)) return;

        ModuleIdentifier jsfInjectionModule = moduleIdFactory.getInjectionModId(jsfVersion);
        ModuleDependency jsfInjectionDependency = new ModuleDependency(moduleLoader, jsfInjectionModule, false, true, true, false);

        try {
            if (isJSF12(jsfInjectionDependency, jsfInjectionModule.toString())) {
                JSFLogger.ROOT_LOGGER.loadingJsf12();
                jsfInjectionDependency.addImportFilter(PathFilters.is("META-INF/faces-config.xml"), false);
                jsfInjectionDependency.addImportFilter(PathFilters.is("META-INF/1.2/faces-config.xml"), true);
            } else {
                JSFLogger.ROOT_LOGGER.loadingJsf2x();
                jsfInjectionDependency.addImportFilter(PathFilters.getMetaInfFilter(), true);
                // Exclude JSF 1.2 faces-config.xml to make extra sure it won't interfere with JSF 2.0 deployments
                jsfInjectionDependency.addImportFilter(PathFilters.is("META-INF/1.2/faces-config.xml"), false);
            }
        } catch (ModuleLoadException e) {
            throw JSFLogger.ROOT_LOGGER.jsfInjectionFailed(jsfVersion);
        }

        moduleSpecification.addSystemDependency(jsfInjectionDependency);
    }

    private boolean isJSF12(ModuleDependency moduleDependency, String identifier) throws ModuleLoadException {

        // The class javax.faces.event.NamedEvent was introduced in JSF 2.0
        return (moduleDependency.getModuleLoader().loadModule(identifier)
                                .getClassLoader().getResource("/javax/faces/event/NamedEvent.class") == null);
    }

    // Add a flag to the sevlet context so that we know if we need to instantiate
    // a CDI ViewHandler.
    private void addCDIFlag(WarMetaData warMetaData, DeploymentUnit deploymentUnit) {
        JBossWebMetaData webMetaData = warMetaData.getMergedJBossWebMetaData();
        if (webMetaData == null) {
            webMetaData = new JBossWebMetaData();
            warMetaData.setMergedJBossWebMetaData(webMetaData);
        }

        List<ParamValueMetaData> contextParams = webMetaData.getContextParams();
        if (contextParams == null) {
            contextParams = new ArrayList<ParamValueMetaData>();
        }

        boolean isCDI = WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit);
        ParamValueMetaData param = new ParamValueMetaData();
        param.setParamName(IS_CDI_PARAM);
        param.setParamValue(Boolean.toString(isCDI));
        contextParams.add(param);

        webMetaData.setContextParams(contextParams);
    }
}
