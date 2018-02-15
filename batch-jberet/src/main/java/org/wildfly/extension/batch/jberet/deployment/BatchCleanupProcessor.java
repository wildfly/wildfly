/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    @Override
    public void undeploy(final DeploymentUnit context) {
    }
}
