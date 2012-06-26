package org.jboss.as.jsf.deployment;

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
import org.jboss.as.web.deployment.JsfVersionMarker;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.filter.PathFilters;

/**
 * @author Stuart Douglas
 */
public class JSFDependencyProcessor implements DeploymentUnitProcessor {


    private static final ModuleIdentifier JSF_IMPL = ModuleIdentifier.create("com.sun.jsf-impl");
    private static final ModuleIdentifier JSF_API = ModuleIdentifier.create("javax.faces.api");
    private static final ModuleIdentifier JSF_SUBSYSTEM = ModuleIdentifier.create("org.jboss.as.jsf");
    private static final ModuleIdentifier JSF_1_2_IMPL = ModuleIdentifier.create("com.sun.jsf-impl", "1.2");
    private static final ModuleIdentifier JSF_1_2_API = ModuleIdentifier.create("javax.faces.api", "1.2");
    private static final ModuleIdentifier BEAN_VALIDATION = ModuleIdentifier.create("org.hibernate.validator");
    private static final ModuleIdentifier JSTL = ModuleIdentifier.create("javax.servlet.jstl.api");

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

        final String jsfVersion = JsfVersionMarker.getVersion(topLevelDeployment);

        addJSFAPI(jsfVersion, moduleSpecification, moduleLoader);

        addJSFImpl(jsfVersion, moduleSpecification, moduleLoader);

        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JSTL, false, false, false, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, BEAN_VALIDATION, false, false, true, false));

        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JSF_SUBSYSTEM, false, false, true, false));
    }

    @Override
    public void undeploy(DeploymentUnit context) {
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
}
