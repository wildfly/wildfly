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
