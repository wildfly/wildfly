/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.deployers.deployment;

import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * Deployment builder interface.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
interface DeploymentModelBuilder {
    /**
     * Creates Web Service deployment model and associates it with deployment.
     *
     * @param unit deployment unit
     */
    void newDeploymentModel(DeploymentUnit unit);
}
