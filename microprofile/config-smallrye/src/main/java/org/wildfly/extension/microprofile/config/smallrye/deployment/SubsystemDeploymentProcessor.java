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

import io.smallrye.config.SmallRyeConfigBuilder;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;

/**
 */
public class SubsystemDeploymentProcessor implements DeploymentUnitProcessor {

    public static final AttachmentKey<Config> CONFIG = AttachmentKey.create(Config.class);

    private final Iterable<ConfigSourceProvider> providers;
    private final Iterable<ConfigSource> sources;

    public SubsystemDeploymentProcessor(Iterable<ConfigSourceProvider> providers, Iterable<ConfigSource> sources) {
        this.providers = providers;
        this.sources = sources;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        Module module = deploymentUnit.getAttachment(Attachments.MODULE);

        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        builder.forClassLoader(module.getClassLoader())
                .addDefaultSources()
                .addDiscoveredSources()
                .addDiscoveredConverters();

        ClassLoader loader = module.getClassLoader();
        for (ConfigSourceProvider provider : this.providers) {
            for (ConfigSource source : provider.getConfigSources(loader)) {
                builder.withSources(source);
            }
        }
        for (ConfigSource source : this.sources) {
            builder.withSources(source);
        }

        Config config = builder.build();
        deploymentUnit.putAttachment(CONFIG, config);

        ConfigProviderResolver.instance().registerConfig(config, module.getClassLoader());
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        Config config = context.removeAttachment(CONFIG);

        ConfigProviderResolver.instance().releaseConfig(config);
    }
}
