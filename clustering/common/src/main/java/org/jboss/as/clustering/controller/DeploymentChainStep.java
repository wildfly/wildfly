/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.function.Consumer;

import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;

/**
 * Deployment chain step that delegates to a {@link DeploymentProcessorTarget} consumer.
 * @author Paul Ferraro
 */
public class DeploymentChainStep extends AbstractDeploymentChainStep {

    private final Consumer<DeploymentProcessorTarget> deploymentChainContributor;

    public DeploymentChainStep(Consumer<DeploymentProcessorTarget> deploymentChainContributor) {
        this.deploymentChainContributor = deploymentChainContributor;
    }

    @Override
    protected void execute(DeploymentProcessorTarget target) {
        this.deploymentChainContributor.accept(target);
    }
}
