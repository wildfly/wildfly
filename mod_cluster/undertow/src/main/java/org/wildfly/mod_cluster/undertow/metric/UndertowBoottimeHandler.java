/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.mod_cluster.undertow.metric;

import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.modcluster.load.metric.LoadMetric;
import org.kohsuke.MetaInfServices;
import org.wildfly.extension.mod_cluster.BoottimeHandlerProvider;
import org.wildfly.extension.mod_cluster.ModClusterExtension;
import org.wildfly.mod_cluster.undertow.ModClusterUndertowDeploymentProcessor;

/**
 * @author Radoslav Husar
 * @since 8.0
 */
@MetaInfServices(BoottimeHandlerProvider.class)
public class UndertowBoottimeHandler implements BoottimeHandlerProvider {

    @Override
    public void performBoottime(OperationContext context, Set<String> adapterNames, Set<LoadMetric> enabledMetrics) {
        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            protected void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(ModClusterExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_UNDERTOW_MODCLUSTER, new ModClusterUndertowDeploymentProcessor(adapterNames, enabledMetrics));
            }
        }, OperationContext.Stage.RUNTIME);
    }

}
