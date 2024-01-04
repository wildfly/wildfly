/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.naming.deployment;

import org.jboss.as.naming.context.external.ExternalContexts;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * A processor which adds the subsystem external contexts to deployment units attachments.
 * @author Eduardo Martins
 */
public class ExternalContextsProcessor implements DeploymentUnitProcessor {

    /**
     *
     */
    private final ExternalContexts externalContexts;

    /**
     *
     * @param externalContexts
     */
    public ExternalContextsProcessor(ExternalContexts externalContexts) {
        this.externalContexts = externalContexts;
    }

    /**
     *
     * @return
     */
    public ExternalContexts getExternalContexts() {
        return externalContexts;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        // add the external contexts to every DU attachments
        phaseContext.getDeploymentUnit().putAttachment(Attachments.EXTERNAL_CONTEXTS, externalContexts);
    }
}
