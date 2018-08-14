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

package org.wildfly.extension.microprofile.config.smallrye.deployment;

import java.util.List;

import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.inject.ConfigExtension;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.deployment.WeldPortableExtensions;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.wildfly.extension.microprofile.config.smallrye.ServiceNames;

/**
 */
public class SubsystemDeploymentProcessor implements DeploymentUnitProcessor {

    public static final AttachmentKey<Config> CONFIG = AttachmentKey.create(Config.class);
    public static final AttachmentKey<ConfigProviderResolver> CONFIG_PROVIDER_RESOLVER = AttachmentKey.create(ConfigProviderResolver.class);

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        Module module = deploymentUnit.getAttachment(Attachments.MODULE);

        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        builder.forClassLoader(module.getClassLoader())
                .addDefaultSources()
                .addDiscoveredSources()
                .addDiscoveredConverters();
        addConfigSourcesFromServices(builder, phaseContext.getServiceRegistry(), module.getClassLoader());
        Config config = builder.build();

        ConfigProviderResolver configProviderResolver = ConfigProviderResolver.instance();
        deploymentUnit.putAttachment(CONFIG, config);
        deploymentUnit.putAttachment(CONFIG_PROVIDER_RESOLVER, configProviderResolver);

        configProviderResolver.registerConfig(config, module.getClassLoader());

        if (WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
            WeldPortableExtensions extensions = WeldPortableExtensions.getPortableExtensions(deploymentUnit);
            extensions.registerExtensionInstance(new ConfigExtension(), deploymentUnit);
        }

    }

    private void addConfigSourcesFromServices(ConfigBuilder builder, ServiceRegistry serviceRegistry, ClassLoader classloader) {
        List<ServiceName> serviceNames = serviceRegistry.getServiceNames();
        for (ServiceName serviceName: serviceNames) {
            if (ServiceNames.CONFIG_SOURCE.isParentOf(serviceName)) {
                ServiceController<?> service = serviceRegistry.getService(serviceName);
                ConfigSource configSource = ConfigSource.class.cast(service.getValue());
                builder.withSources(configSource);
            } else if (ServiceNames.CONFIG_SOURCE_PROVIDER.isParentOf(serviceName)) {
                ServiceController<?> service = serviceRegistry.getService(serviceName);
                ConfigSourceProvider configSourceProvider = ConfigSourceProvider.class.cast(service.getValue());
                for (ConfigSource configSource : configSourceProvider.getConfigSources(classloader)) {
                    builder.withSources(configSource);
                }
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        Config config = context.getAttachment(CONFIG);
        ConfigProviderResolver configProviderResolver = context.getAttachment(CONFIG_PROVIDER_RESOLVER);

        configProviderResolver.releaseConfig(config);
    }
}
