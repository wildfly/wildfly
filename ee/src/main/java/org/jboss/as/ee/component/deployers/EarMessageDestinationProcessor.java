package org.jboss.as.ee.component.deployers;

import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.structure.Attachments;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.metadata.javaee.spec.MessageDestinationMetaData;

/**
 * @author Stuart Douglas
 */
public class EarMessageDestinationProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            final EarMetaData metadata = deploymentUnit.getAttachment(Attachments.EAR_METADATA);
            final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);

            if (metadata != null) {

                if (metadata.getEarEnvironmentRefsGroup() != null) {
                    if (metadata.getEarEnvironmentRefsGroup().getMessageDestinations() != null) {
                        for (final MessageDestinationMetaData destination : metadata.getEarEnvironmentRefsGroup().getMessageDestinations()) {
                            //TODO: should these be two separate metadata attributes?
                            if (destination.getJndiName() != null) {
                                eeModuleDescription.addMessageDestination(destination.getName(), destination.getJndiName());
                            } else if (destination.getLookupName() != null) {
                                eeModuleDescription.addMessageDestination(destination.getName(), destination.getLookupName());
                            }
                        }
                    }
                }

            }

        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
