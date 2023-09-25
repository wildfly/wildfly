/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.function.Consumer;

import org.jboss.as.server.DeploymentProcessorTarget;

/**
 * Registers a {@link DeploymentChainContributingAddStepHandler}, {@link ReloadRequiredRemoveStepHandler}, and {@link WriteAttributeStepHandler} on behalf of a resource definition.
 * @author Paul Ferraro
 */
public class DeploymentChainContributingResourceRegistrar extends ResourceRegistrar {

    public DeploymentChainContributingResourceRegistrar(ResourceDescriptor descriptor, ResourceServiceHandler handler, Consumer<DeploymentProcessorTarget> contributor) {
        super(descriptor, handler, new DeploymentChainContributingAddStepHandler(descriptor, handler, contributor), new ReloadRequiredRemoveStepHandler(descriptor));
    }
}
