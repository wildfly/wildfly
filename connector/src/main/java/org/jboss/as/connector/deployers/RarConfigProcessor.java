package org.jboss.as.connector.deployers;

import org.jboss.as.deployment.DeploymentPhases;
import org.jboss.as.deployment.module.ModuleConfig;
import org.jboss.as.deployment.module.ModuleDependencies;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.modules.ModuleIdentifier;

public class RarConfigProcessor implements DeploymentUnitProcessor {
    public static final long PRIORITY = DeploymentPhases.MODULE_DEPENDENCIES.plus(200L);
    private static ModuleIdentifier JAVAX_ID = ModuleIdentifier.create("javax.resource.api");
    private static ModuleIdentifier LOGGING_ID = ModuleIdentifier.create("org.jboss.logging");
    private static ModuleIdentifier IRON_JACAMAR_ID = ModuleIdentifier.create("org.jboss.ironjacamar.api");
    private static ModuleIdentifier IRON_JACAMAR_IMPL_ID = ModuleIdentifier.create("org.jboss.ironjacamar.impl");
    private static ModuleIdentifier NAMING_ID = ModuleIdentifier.create("org.jboss.as.naming");
    private static ModuleIdentifier VALIDATION_ID = ModuleIdentifier.create("javax.validation.api");
    private static ModuleIdentifier HIBERNATE_VALIDATOR_ID = ModuleIdentifier.create("org.hibernate.validator");
    private static ModuleIdentifier COMMON_CORE_ID = ModuleIdentifier.create("org.jboss.common-core");

    private static ModuleIdentifier SYSTEM_ID = ModuleIdentifier.create("javax.api");

    /**
     * Add dependencies for modules required for ra deployments
     * @param context the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        ModuleDependencies.addDependency(context, new ModuleConfig.Dependency(JAVAX_ID, true, false, false));
        ModuleDependencies.addDependency(context, new ModuleConfig.Dependency(LOGGING_ID, true, false, false));
        ModuleDependencies.addDependency(context, new ModuleConfig.Dependency(IRON_JACAMAR_ID, true, false, false));
        ModuleDependencies.addDependency(context, new ModuleConfig.Dependency(IRON_JACAMAR_IMPL_ID, true, false, true));
        ModuleDependencies.addDependency(context, new ModuleConfig.Dependency(SYSTEM_ID, true, false, false));
        ModuleDependencies.addDependency(context, new ModuleConfig.Dependency(NAMING_ID, true, false, false));
        ModuleDependencies.addDependency(context, new ModuleConfig.Dependency(VALIDATION_ID, true, false, false));
        ModuleDependencies.addDependency(context, new ModuleConfig.Dependency(HIBERNATE_VALIDATOR_ID, true, false, false));
        ModuleDependencies.addDependency(context, new ModuleConfig.Dependency(COMMON_CORE_ID, true, false, false));
    }
}
