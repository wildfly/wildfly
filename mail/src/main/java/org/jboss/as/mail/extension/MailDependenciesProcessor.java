package org.jboss.as.mail.extension;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * @author Stuart Douglas
 */
public class MailDependenciesProcessor implements DeploymentUnitProcessor {


    private static final ModuleIdentifier MAIL_API = ModuleIdentifier.create("javax.mail.api");
    private static final ModuleIdentifier ACTIVATION_API = ModuleIdentifier.create("javax.activation.api");

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        final ModuleSpecification moduleSpec = unit.getAttachment(Attachments.MODULE_SPECIFICATION);
        moduleSpec.addSystemDependency(new ModuleDependency(moduleLoader, MAIL_API, false, false, true, false));
        moduleSpec.addSystemDependency(new ModuleDependency(moduleLoader, ACTIVATION_API, false, false, true, false));

    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
