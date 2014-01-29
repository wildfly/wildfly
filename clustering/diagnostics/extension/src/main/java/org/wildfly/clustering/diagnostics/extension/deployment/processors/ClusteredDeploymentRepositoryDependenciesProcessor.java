package org.wildfly.clustering.diagnostics.extension.deployment.processors;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.wildfly.clustering.diagnostics.extension.deployment.ClusteredDeploymentRepository;

/**
 * A DUP that sets up service dependencies for the ClusteredDeploymentRepositoryProcessor
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class ClusteredDeploymentRepositoryDependenciesProcessor implements DeploymentUnitProcessor {

    public static AttachmentKey<ClusteredDeploymentRepository> CLUSTERED_DEPLOYMENT_REPOSITORY_SERVICE_KEY = AttachmentKey.create(ClusteredDeploymentRepository.class);

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        // the ClusteredDeploymentRepository instance is made available as an attachment
        phaseContext.addDependency(ClusteredDeploymentRepository.SERVICE_NAME, CLUSTERED_DEPLOYMENT_REPOSITORY_SERVICE_KEY);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
