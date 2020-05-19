/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.config.smallrye;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.microprofile.config.smallrye._private.MicroProfileConfigLogger;
import org.wildfly.extension.microprofile.config.smallrye.deployment.DependencyProcessor;
import org.wildfly.extension.microprofile.config.smallrye.deployment.SubsystemDeploymentProcessor;

import io.smallrye.config.SmallRyeConfigProviderResolver;

/**
 * Handler responsible for adding the subsystem resource to the model
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class MicroProfileConfigSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static {
        // Set the static resolver reference eagerly
        ConfigProviderResolver.setInstance(new SmallRyeConfigProviderResolver());
    }

    final Iterable<ConfigSourceProvider> providers;
    final Iterable<ConfigSource> sources;

    MicroProfileConfigSubsystemAdd(Iterable<ConfigSourceProvider> providers, Iterable<ConfigSource> sources) {
        this.providers = providers;
        this.sources = sources;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void performBoottime(OperationContext context, ModelNode operation, ModelNode model) {

        MicroProfileConfigLogger.ROOT_LOGGER.activatingSubsystem();

        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            public void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(MicroProfileConfigExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_MICROPROFILE_CONFIG, new DependencyProcessor());
                processorTarget.addDeploymentProcessor(MicroProfileConfigExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_MICROPROFILE_CONFIG, new SubsystemDeploymentProcessor(MicroProfileConfigSubsystemAdd.this.providers, MicroProfileConfigSubsystemAdd.this.sources));
            }
        }, OperationContext.Stage.RUNTIME);

    }
}
