/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.config.smallrye.deployment;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
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

    public SubsystemDeploymentProcessor() {
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        Module module = deploymentUnit.getAttachment(Attachments.MODULE);

        Config config = ConfigProviderResolver.instance().getConfig(module.getClassLoader());
        deploymentUnit.putAttachment(CONFIG, config);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        Config config = context.removeAttachment(CONFIG);

        ConfigProviderResolver.instance().releaseConfig(config);
    }
}
