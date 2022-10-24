/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.ejb;

import static org.wildfly.extension.clustering.ejb.HotRodBeanManagementResourceDefinition.Attribute.*;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.ejb.bean.BeanManagementProvider;
import org.wildfly.clustering.ejb.hotrod.bean.HotRodBeanManagementConfiguration;
import org.wildfly.clustering.ejb.hotrod.bean.HotRodBeanManagementProvider;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public class HotRodBeanManagementServiceConfigurator extends BeanManagementServiceConfigurator implements HotRodBeanManagementConfiguration {

    private volatile String containerName;
    private volatile String configurationName;

    public HotRodBeanManagementServiceConfigurator(PathAddress address) {
        super(address);
    }

    @Override
    public BeanManagementProvider apply(String name) {
        return new HotRodBeanManagementProvider(name, this);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.containerName = REMOTE_CACHE_CONTAINER.resolveModelAttribute(context, model).asString();
        this.configurationName = CACHE.resolveModelAttribute(context, model).asStringOrNull();
        return super.configure(context, model);
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
