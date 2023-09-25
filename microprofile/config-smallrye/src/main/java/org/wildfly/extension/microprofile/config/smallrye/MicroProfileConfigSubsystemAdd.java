/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigProviderResolver;

/**
 * Handler responsible for adding the subsystem resource to the model
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class MicroProfileConfigSubsystemAdd extends AbstractBoottimeAddStepHandler {

    final Iterable<ConfigSourceProvider> providers;
    final Iterable<ConfigSource> sources;

    MicroProfileConfigSubsystemAdd(Iterable<ConfigSourceProvider> providers, Iterable<ConfigSource> sources) {
        this.providers = providers;
        this.sources = sources;

        // Override smallrye-config's ConfigProviderResolver so that
        // the builder loads config sources from the microprofile-config-smallrye subsystem by default
        ConfigProviderResolver.setInstance(new SmallRyeConfigProviderResolver() {

            @Override
            public SmallRyeConfigBuilder getBuilder() {
                // The builder will take into account the config-sources available when the Config object is created.
                // any config-sources added or modified subsequently will not be taken into account.
                SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder() {
                    @Override
                    public SmallRyeConfigBuilder forClassLoader(ClassLoader classLoader) {
                        SmallRyeConfigBuilder builder = super.forClassLoader(classLoader);
                        for (ConfigSourceProvider provider : providers) {
                            for (ConfigSource source : provider.getConfigSources(classLoader)) {
                                builder.withSources(source);
                            }
                        }
                        for (ConfigSource source : sources) {
                            builder.withSources(source);
                        }
                        return builder;
                    }
                };
                builder.addDefaultInterceptors();
                return builder;
            }
        });
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
                processorTarget.addDeploymentProcessor(MicroProfileConfigExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_MICROPROFILE_CONFIG, new SubsystemDeploymentProcessor());
            }
        }, OperationContext.Stage.RUNTIME);

    }
}
