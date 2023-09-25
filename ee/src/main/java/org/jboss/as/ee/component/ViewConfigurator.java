/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;

/**
 * A configurator for views.  Each configurator is run in the order it appears on the component description.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ViewConfigurator {

    /**
     * Apply this configurator to the given configuration.
     *
     * @param context the deployment phase context
     * @param componentConfiguration the completed component configuration
     * @param description the completed view description
     * @param configuration the view configuration to build on to
     * @throws DeploymentUnitProcessingException if configuration fails
     */
    void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException;
}
