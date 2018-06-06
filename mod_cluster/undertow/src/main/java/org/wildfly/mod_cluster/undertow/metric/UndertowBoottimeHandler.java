/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
