/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.infinispan.logging.InfinispanLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.server.service.LocalGroupServiceConfiguratorProvider;
import org.wildfly.clustering.server.service.ProvidedGroupServiceConfigurator;
import org.wildfly.clustering.server.service.ProvidedIdentityGroupServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public class InfinispanSubsystemServiceHandler implements ResourceServiceHandler {

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        InfinispanLogger.ROOT_LOGGER.activatingSubsystem();

        ServiceTarget target = context.getServiceTarget();

        // Install local group services
        new ProvidedGroupServiceConfigurator<>(LocalGroupServiceConfiguratorProvider.class, LocalGroupServiceConfiguratorProvider.LOCAL).configure(context).build(target).install();

        // If JGroups subsystem is not available, install default group aliases to local group.
        if (!context.hasOptionalCapability(JGroupsRequirement.CHANNEL.getDefaultRequirement().getName(), null, null)) {
            new ProvidedIdentityGroupServiceConfigurator(null, LocalGroupServiceConfiguratorProvider.LOCAL).configure(context).build(target).install();
        }
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {

        new ProvidedGroupServiceConfigurator<>(LocalGroupServiceConfiguratorProvider.class, LocalGroupServiceConfiguratorProvider.LOCAL).remove(context);

        if (!context.hasOptionalCapability(JGroupsRequirement.CHANNEL.getDefaultRequirement().getName(), null, null)) {
            new ProvidedIdentityGroupServiceConfigurator(null, LocalGroupServiceConfiguratorProvider.LOCAL).remove(context);
        }
    }
}
