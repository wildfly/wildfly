package org.jboss.as.capedwarf.deployment;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.vfs.VirtualFile;

/**
 * @author <a href="mailto:marko.luksa@gmail.com">Marko Luksa</a>
 */
public class CapedwarfDeploymentProcessor implements DeploymentUnitProcessor {

    private static final ModuleIdentifier APPENGINE = ModuleIdentifier.create("com.google.appengine");
    private static final ModuleIdentifier CAPEDWARF = ModuleIdentifier.create("org.jboss.capedwarf");
    private static final String APPENGINE_WEB_XML = "WEB-INF/appengine-web.xml";

    Logger log = Logger.getLogger(CapedwarfDeploymentProcessor.class);

    /**
     * See {@link Phase} for a description of the different phases
     */
    public static final Phase PHASE = Phase.DEPENDENCIES;

    /**
     * The relative order of this processor within the {@link #PHASE}.
     * The current number is large enough for it to happen after all
     * the standard deployment unit processors that come with JBoss AS.
     */
    public static final int PRIORITY = 0x4000;

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        log.info("Deploy");
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (hasAppEngineWebXml(deploymentUnit)) {
            addDependenciesTo(deploymentUnit);
        }
    }

    private boolean hasAppEngineWebXml(DeploymentUnit deploymentUnit) {
        ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        VirtualFile root = deploymentRoot.getRoot();
        VirtualFile xml = root.getChild(APPENGINE_WEB_XML);
        return xml.exists();
    }

    private void addDependenciesTo(DeploymentUnit deploymentUnit) {
        ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        moduleSpecification.addSystemDependency(createModuleDependency(CAPEDWARF));
        moduleSpecification.addSystemDependency(createModuleDependency(APPENGINE));
    }

    private ModuleDependency createModuleDependency(ModuleIdentifier moduleIdentifier) {
        return new ModuleDependency(Module.getBootModuleLoader(), moduleIdentifier, false, false, true);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

}
