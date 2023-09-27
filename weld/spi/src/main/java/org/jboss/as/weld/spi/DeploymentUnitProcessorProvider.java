/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.spi;

import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;

/**
 * Allows to add additional deployment unit processors to a {@link DeploymentProcessorTarget}.
 *
 * @author Martin Kouba
 * @see DeploymentUnitProcessor
 */
public interface DeploymentUnitProcessorProvider {

    /**
     *
      * @return the processor
     */
    DeploymentUnitProcessor getProcessor();

    /**
     *
     * @return the phase
     */
    Phase getPhase();

    /**
     *
     * @return the priority
     */
    int getPriority();

}
