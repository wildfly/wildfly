/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

import java.util.concurrent.atomic.AtomicReference;

/**
 * processor that sets the default distinct name for a deployment.
 *
 * @author Stuart Douglas
 */
public class EjbDefaultDistinctNameProcessor implements DeploymentUnitProcessor {

    private final AtomicReference<String> defaultDistinctName;

    public EjbDefaultDistinctNameProcessor(final AtomicReference<String> defaultDistinctName) {
        this.defaultDistinctName = defaultDistinctName;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final String defaultDistinctName = this.defaultDistinctName.get();
        if(defaultDistinctName != null) {
            phaseContext.getDeploymentUnit().putAttachment(org.jboss.as.ee.structure.Attachments.DISTINCT_NAME, defaultDistinctName);
        }
    }
}
