package org.jboss.as.webservices.deployers;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.deployers.EEResourceReferenceProcessorRegistry;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * @author Stuart Douglas
 */
public class WebServicesContextJndiSetupProcessor  implements DeploymentUnitProcessor {
    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEResourceReferenceProcessorRegistry registry = deploymentUnit.getAttachment(Attachments.RESOURCE_REFERENCE_PROCESSOR_REGISTRY);
        // Add a EEResourceReferenceProcessor which handles @Resource references of type WebServiceContext.
        registry.registerResourceReferenceProcessor(new WebServiceContextResourceProcessor());
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}
