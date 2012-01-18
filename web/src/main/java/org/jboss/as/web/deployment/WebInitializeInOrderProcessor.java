package org.jboss.as.web.deployment;

import static org.jboss.as.web.WebMessages.MESSAGES;

import org.jboss.as.ee.structure.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.WebSubsystemServices;
import org.jboss.msc.service.ServiceName;

/**
 * Processor that adds web deployments to the list
 *
 *
 * @author Stuart Douglas
 */
public class WebInitializeInOrderProcessor implements DeploymentUnitProcessor {

    private final String defaultHost;

    public WebInitializeInOrderProcessor(String defaultHost) {
        if (defaultHost == null) {
            throw MESSAGES.nullDefaultHost();
        }
        this.defaultHost = defaultHost;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final WarMetaData metaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (metaData == null) {
            return;
        }
        final String hostName = WarDeploymentProcessor.hostNameOfDeployment(metaData, defaultHost);
        processDeployment(hostName, metaData, deploymentUnit);
    }

    private void processDeployment(final String hostName, final WarMetaData metaData, final DeploymentUnit deploymentUnit) {
        final String pathName = WarDeploymentProcessor.pathNameOfDeployment(deploymentUnit, metaData.getMergedJBossWebMetaData());
        final ServiceName deploymentServiceName = WebSubsystemServices.deploymentServiceName(hostName, pathName);
        final ServiceName realmServiceName = deploymentServiceName.append("realm");
        deploymentUnit.addToAttachmentList(Attachments.INITIALISE_IN_ORDER_SERVICES, deploymentServiceName);
        deploymentUnit.addToAttachmentList(Attachments.INITIALISE_IN_ORDER_SERVICES, realmServiceName);
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}
