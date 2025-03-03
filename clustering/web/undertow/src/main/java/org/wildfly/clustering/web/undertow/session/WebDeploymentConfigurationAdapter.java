/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import org.wildfly.clustering.server.deployment.DeploymentConfiguration;
import org.wildfly.clustering.web.container.WebDeploymentConfiguration;

/**
 * @author Paul Ferraro
 */
public class WebDeploymentConfigurationAdapter implements DeploymentConfiguration {

    private final WebDeploymentConfiguration configuration;

    public WebDeploymentConfigurationAdapter(WebDeploymentConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String getServerName() {
        return this.configuration.getServerName();
    }

    @Override
    public String getDeploymentName() {
        return this.configuration.getDeploymentName();
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.configuration.getModule().getClassLoader();
    }
}
