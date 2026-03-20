/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.config.smallrye;

import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.wildfly.extension.microprofile.config.smallrye._private.MicroProfileConfigLogger;
import org.wildfly.extension.microprofile.config.smallrye.deployment.DependencyProcessor;
import org.wildfly.extension.microprofile.config.smallrye.deployment.SubsystemDeploymentProcessor;

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
    public void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        MicroProfileConfigLogger.ROOT_LOGGER.activatingSubsystem();

        // Install the capability service so dependent subsystems can wait on config availability
        ConfigProviderResolver resolver = ConfigProviderResolver.instance();
        CapabilityServiceBuilder<?> sb = context.getCapabilityServiceTarget().addService();
        sb.setInstance(Service.newInstance(sb.provides(MicroProfileSubsystemDefinition.CONFIG_CAPABILITY), resolver));

        // Add dependencies on all config-source and config-source-provider child resources
        // This ensures all config sources are registered before this service starts
        // Note: Must read the full resource tree to get children, as 'model' only contains attributes
        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS, true);

        if (resource.hasChildren(MicroProfileConfigExtension.CONFIG_SOURCE_PATH.getKey())) {
            for (String name : resource.getChildrenNames(MicroProfileConfigExtension.CONFIG_SOURCE_PATH.getKey())) {
                // Read the child resource to check if it's a root directory config source
                Resource childResource = resource.getChild(
                        PathElement.pathElement(MicroProfileConfigExtension.CONFIG_SOURCE_PATH.getKey(), name));
                ModelNode configSourceModel = childResource.getModel();

                // Check if this is a root directory config source (uses CONFIG_SOURCE_ROOT service name)
                // or a regular config source (uses CONFIG_SOURCE service name)
                if (configSourceModel.hasDefined("dir", "root") &&
                        context.resolveExpressions(configSourceModel.get("dir", "root")).asBoolean()) {
                    sb.requires(ServiceNames.CONFIG_SOURCE_ROOT.append(name));
                } else {
                    sb.requires(ServiceNames.CONFIG_SOURCE.append(name));
                }
            }
        }

        if (resource.hasChildren(MicroProfileConfigExtension.CONFIG_SOURCE_PROVIDER_PATH.getKey())) {
            for (String name : resource.getChildrenNames(MicroProfileConfigExtension.CONFIG_SOURCE_PROVIDER_PATH.getKey())) {
                sb.requires(ServiceNames.CONFIG_SOURCE_PROVIDER.append(name));
            }
        }

        sb.install();

        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            public void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(MicroProfileConfigExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_MICROPROFILE_CONFIG, new DependencyProcessor());
                processorTarget.addDeploymentProcessor(MicroProfileConfigExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_MICROPROFILE_CONFIG, new SubsystemDeploymentProcessor());
            }
        }, OperationContext.Stage.RUNTIME);

    }
}
