package org.wildfly.extension.opentelemetry.deployment;

import static org.wildfly.extension.opentelemetry.extension.OpenTelemetrySubsystemDefinition.EXPORTED_MODULES;
import static org.wildfly.extension.opentelemetry.extension.OpenTelemetrySubsystemDefinition.MODULES;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;

public class OpenTelemetryDependencyProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        addDependencies(phaseContext.getDeploymentUnit());
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {

    }

    private void addDependencies(DeploymentUnit deploymentUnit) {
        ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        ModuleLoader moduleLoader = Module.getBootModuleLoader();
        for (String module : MODULES) {
            moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, module, false, false,
                    true, false));
        }
        for (String module : EXPORTED_MODULES) {
            ModuleDependency modDep = new ModuleDependency(moduleLoader, module, false, true,
                    true, false);
            moduleSpecification.addSystemDependency(modDep);
        }
    }
}
