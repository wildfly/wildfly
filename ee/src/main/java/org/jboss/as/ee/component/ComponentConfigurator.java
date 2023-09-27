/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;

/**
 * A configurator for components.  Each configurator is run in the order it appears on the component description.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ComponentConfigurator {

    /**
     * Apply this configurator to the given component configuration.
     *
     * @param context the deployment phase context
     * @param description the completed component description
     * @param configuration the component configuration to build on to
     * @throws DeploymentUnitProcessingException if configuration fails
     */
    void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws DeploymentUnitProcessingException;
}
