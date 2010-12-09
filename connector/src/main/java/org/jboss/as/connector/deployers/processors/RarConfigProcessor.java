package org.jboss.as.connector.deployers.processors;

import org.jboss.as.connector.metadata.xmldescriptors.ConnectorXmlDescriptor;
import org.jboss.as.deployment.Attachments;
import org.jboss.as.deployment.module.ModuleDependency;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.deployment.unit.DeploymentPhaseContext;
import org.jboss.modules.ModuleIdentifier;

public class RarConfigProcessor implements DeploymentUnitProcessor {

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
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        if(phaseContext.getDeploymentUnitContext().getAttachment(ConnectorXmlDescriptor.ATTACHMENT_KEY) == null) {
            return;  // Skip non ra deployments
        }
        phaseContext.addToAttachmentList(Attachments.MODULE_DEPENDENCIES, new ModuleDependency(JAVAX_ID, false, false));
        phaseContext.addToAttachmentList(Attachments.MODULE_DEPENDENCIES, new ModuleDependency(LOGGING_ID, false, false));
        phaseContext.addToAttachmentList(Attachments.MODULE_DEPENDENCIES, new ModuleDependency(IRON_JACAMAR_ID, false, false));
        phaseContext.addToAttachmentList(Attachments.MODULE_DEPENDENCIES, new ModuleDependency(IRON_JACAMAR_IMPL_ID, false, true));
        phaseContext.addToAttachmentList(Attachments.MODULE_DEPENDENCIES, new ModuleDependency(SYSTEM_ID, false, false));
        phaseContext.addToAttachmentList(Attachments.MODULE_DEPENDENCIES, new ModuleDependency(NAMING_ID, false, false));
        phaseContext.addToAttachmentList(Attachments.MODULE_DEPENDENCIES, new ModuleDependency(VALIDATION_ID, false, false));
        phaseContext.addToAttachmentList(Attachments.MODULE_DEPENDENCIES, new ModuleDependency(HIBERNATE_VALIDATOR_ID, false, false));
        phaseContext.addToAttachmentList(Attachments.MODULE_DEPENDENCIES, new ModuleDependency(COMMON_CORE_ID, false, false));
    }

    public void undeploy(final DeploymentUnitContext context) {
    }
}
