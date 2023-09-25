/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import static org.wildfly.extension.clustering.web.HotRodSSOManagementResourceDefinition.Attribute.*;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.web.service.sso.DistributableSSOManagementProvider;
import org.wildfly.extension.clustering.web.sso.hotrod.HotRodSSOManagementConfiguration;
import org.wildfly.extension.clustering.web.sso.hotrod.HotRodSSOManagementProvider;

/**
 * @author Paul Ferraro
 */
public class HotRodSSOManagementServiceConfigurator extends SSOManagementServiceConfigurator implements HotRodSSOManagementConfiguration {

    private volatile String containerName;
    private volatile String configurationName;

    public HotRodSSOManagementServiceConfigurator(PathAddress address) {
        super(address);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.containerName = REMOTE_CACHE_CONTAINER.resolveModelAttribute(context, model).asString();
        this.configurationName = CACHE_CONFIGURATION.resolveModelAttribute(context, model).asStringOrNull();
        return this;
    }

    @Override
    public DistributableSSOManagementProvider get() {
        return new HotRodSSOManagementProvider(this);
    }

    @Override
    public String getContainerName() {
        return this.containerName;
    }

    @Override
    public String getConfigurationName() {
        return this.configurationName;
    }
}
