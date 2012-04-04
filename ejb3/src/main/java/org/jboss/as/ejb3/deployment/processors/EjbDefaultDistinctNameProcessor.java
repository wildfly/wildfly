package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ejb3.subsystem.DefaultDistinctNameService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * processor that sets the default distinct name for a deployment.
 *
 * @author Stuart Douglas
 */
public class EjbDefaultDistinctNameProcessor implements DeploymentUnitProcessor {

    private final DefaultDistinctNameService defaultDistinctNameService;

    public EjbDefaultDistinctNameProcessor(final DefaultDistinctNameService defaultDistinctNameService) {
        this.defaultDistinctNameService = defaultDistinctNameService;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final String defaultDistinctName = defaultDistinctNameService.getDefaultDistinctName();
        if(defaultDistinctName != null) {
            phaseContext.getDeploymentUnit().putAttachment(org.jboss.as.ee.structure.Attachments.DISTINCT_NAME, defaultDistinctName);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}
