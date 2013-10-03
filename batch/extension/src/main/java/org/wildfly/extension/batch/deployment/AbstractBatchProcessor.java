package org.wildfly.extension.batch.deployment;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.vfs.VirtualFile;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public abstract class AbstractBatchProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (isBatchDeployment(deploymentUnit)) {
            processDeployment(phaseContext, deploymentUnit);
        }
    }

    /**
     * Processes the deployment.
     * <p/>
     * <em>Note that the {@link #deploy(org.jboss.as.server.deployment.DeploymentPhaseContext)} only invokes this method
     * if {@link #isBatchDeployment(org.jboss.as.server.deployment.DeploymentUnit)} returns {@code true}</em>
     *
     * @param phaseContext   the deployment unit context
     * @param deploymentUnit the deployment unit
     */
    protected abstract void processDeployment(final DeploymentPhaseContext phaseContext, final DeploymentUnit deploymentUnit);

    /**
     * Batch deployments must have a {@code META-INF/batch.xml} and/or XML configuration files in {@code
     * META-INF/batch-jobs}. They must be in an EJB JAR or a WAR.
     *
     * @param deploymentUnit the deployment unit to check
     *
     * @return {@code true} if a {@code META-INF/batch.xml} or a non-empty {@code META-INF/batch-jobs} directory was
     *         found otherwise {@code false}
     */
    protected static boolean isBatchDeployment(final DeploymentUnit deploymentUnit) {
        // Section 10.7 of JSR 352 discusses valid packaging types, of which it appears EAR should be one. It seems
        // though that it's of no real use as 10.5 and 10.6 seem to indicate it must be in META-INF/batch-jobs of a JAR
        // and WEB-INF/classes/META-INF/batch-jobs of a WAR.
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit) || !deploymentUnit.hasAttachment(Attachments.DEPLOYMENT_ROOT)) {
            return false;
        }
        final ResourceRoot root = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        final VirtualFile metaInf;
        if (DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            metaInf = root.getRoot().getChild("WEB-INF/classes/META-INF");
        } else {
            metaInf = root.getRoot().getChild("META-INF");
        }
        final VirtualFile jobXmlFile = metaInf.getChild("batch.xml");
        final VirtualFile batchJobsDir = metaInf.getChild("batch-jobs");
        return (jobXmlFile.exists() || (batchJobsDir.exists() && !batchJobsDir.getChildren().isEmpty()));
    }
}
