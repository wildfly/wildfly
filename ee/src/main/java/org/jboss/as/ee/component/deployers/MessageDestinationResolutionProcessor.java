/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.component.deployers;

import java.util.List;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * Processor that resolves all message destinations. This cannot be done when they are first discovered, as
 * they may resolve to destinations in other deployments.
 *
 * @author Stuart Douglas
 */
public class MessageDestinationResolutionProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final List<MessageDestinationInjectionSource> injections = phaseContext.getDeploymentUnit().getAttachmentList(Attachments.MESSAGE_DESTINATIONS);
        for (final MessageDestinationInjectionSource injection : injections) {
            injection.resolve(phaseContext);
        }

    }
}
