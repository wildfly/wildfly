/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.deployment.processor;

import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.weld.spi.DeploymentUnitProcessorProvider;

/**
 *
 * @author Martin Kouba
 */
public class WeldBeanValidationDependencyProcessorProvider implements DeploymentUnitProcessorProvider {

    @Override
    public DeploymentUnitProcessor getProcessor() {
        return new WeldBeanValidationDependencyProcessor();
    }

    @Override
    public Phase getPhase() {
        return Phase.DEPENDENCIES;
    }

    @Override
    public int getPriority() {
        return Phase.DEPENDENCIES_WELD;
    }

}
