/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet.deployment;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * Cleans up attachments no longer required on {@linkplain DeploymentUnit deployment units}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class BatchCleanupProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        // Clean up job XML resolvers
        phaseContext.getDeploymentUnit().removeAttachment(BatchAttachments.JOB_XML_RESOLVER);
        // Clean jboss-all meta-data
        phaseContext.getDeploymentUnit().removeAttachment(BatchAttachments.BATCH_ENVIRONMENT_META_DATA);
        // Remove the JobOperatorService from the deployment unit
        phaseContext.getDeploymentUnit().removeAttachment(BatchAttachments.JOB_OPERATOR);
    }
}
