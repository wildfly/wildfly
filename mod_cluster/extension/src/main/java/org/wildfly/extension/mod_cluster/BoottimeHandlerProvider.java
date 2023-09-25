/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.mod_cluster;

import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.modcluster.load.metric.LoadMetric;

/**
 * Mechanism to plug in {@link org.jboss.as.server.deployment.DeploymentUnitProcessor} on boot time, e.g. Undertow
 * {@link io.undertow.server.HttpHandler}. Passes on a reference to sets with enabled {@link LoadMetric}s and event handler
 * adapters.
 *
 * @author Radoslav Husar
 * @since 8.0
 */
public interface BoottimeHandlerProvider {
    void performBoottime(OperationContext context, Set<String> adapterNames, Set<LoadMetric> enabledMetrics) throws OperationFailedException;
}
