/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.deployment;

import java.util.function.UnaryOperator;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.wildfly.extension.clustering.web.HotRodSessionManagementResourceDefinition;
import org.wildfly.extension.clustering.web.session.hotrod.HotRodSessionManagementConfiguration;

/**
 * @author Paul Ferraro
 */
public class MutableHotRodSessionManagementConfiguration extends MutableSessionManagementConfiguration implements HotRodSessionManagementConfiguration<DeploymentUnit> {

    private volatile String containerName;
    private volatile String configurationName;
    private volatile int expirationThreadPoolSize = HotRodSessionManagementResourceDefinition.Attribute.EXPIRATION_THREAD_POOL_SIZE.getDefinition().getDefaultValue().asInt();

    /**
     * Constructs a new HotRod session management configuration.
     * @param replacer a property replacer
     */
    public MutableHotRodSessionManagementConfiguration(UnaryOperator<String> replacer) {
        super(replacer);
    }

    @Override
    public String getContainerName() {
        return this.containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    @Override
    public String getConfigurationName() {
        return this.configurationName;
    }

    public void setConfigurationName(String configurationName) {
        this.configurationName = configurationName;
    }

    @Override
    public int getExpirationThreadPoolSize() {
        return this.expirationThreadPoolSize;
    }

    public void setExpirationThreadPoolSize(int expirationThreadPoolSize) {
        this.expirationThreadPoolSize = expirationThreadPoolSize;
    }
}
