/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import static org.wildfly.extension.clustering.web.HotRodSessionManagementResourceDefinition.Attribute.CACHE_CONFIGURATION;
import static org.wildfly.extension.clustering.web.HotRodSessionManagementResourceDefinition.Attribute.EXPIRATION_THREAD_POOL_SIZE;
import static org.wildfly.extension.clustering.web.HotRodSessionManagementResourceDefinition.Attribute.REMOTE_CACHE_CONTAINER;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.extension.clustering.web.session.hotrod.HotRodSessionManagementConfiguration;
import org.wildfly.extension.clustering.web.session.hotrod.HotRodSessionManagementProvider;

/**
 * @author Paul Ferraro
 */
public class HotRodSessionManagementServiceConfigurator extends SessionManagementServiceConfigurator<HotRodSessionManagementConfiguration<DeploymentUnit>> implements HotRodSessionManagementConfiguration<DeploymentUnit> {

    private volatile String containerName;
    private volatile String configurationName;
    private volatile int expirationThreadPoolSize;

    HotRodSessionManagementServiceConfigurator(PathAddress address) {
        super(address);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.containerName = REMOTE_CACHE_CONTAINER.resolveModelAttribute(context, model).asString();
        this.configurationName = CACHE_CONFIGURATION.resolveModelAttribute(context, model).asStringOrNull();
        this.expirationThreadPoolSize = EXPIRATION_THREAD_POOL_SIZE.resolveModelAttribute(context, model).asInt();
        return super.configure(context, model);
    }

    @Override
    public DistributableSessionManagementProvider<HotRodSessionManagementConfiguration<DeploymentUnit>> get() {
        return new HotRodSessionManagementProvider(this);
    }

    @Override
    public String getContainerName() {
        return this.containerName;
    }

    @Override
    public String getConfigurationName() {
        return this.configurationName;
    }

    @Override
    public int getExpirationThreadPoolSize() {
        return this.expirationThreadPoolSize;
    }
}
