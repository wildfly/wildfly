/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.web;

import static org.wildfly.extension.clustering.web.HotRodSessionManagementResourceDefinition.Attribute.CACHE_CONFIGURATION;
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

    HotRodSessionManagementServiceConfigurator(PathAddress address) {
        super(address);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.containerName = REMOTE_CACHE_CONTAINER.resolveModelAttribute(context, model).asString();
        this.configurationName = CACHE_CONFIGURATION.resolveModelAttribute(context, model).asStringOrNull();
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
}
