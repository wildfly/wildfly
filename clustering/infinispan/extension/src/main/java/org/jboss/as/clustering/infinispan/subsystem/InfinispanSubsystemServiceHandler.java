/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
        if (!context.getCapabilityServiceSupport().hasCapability(JGroupsRequirement.CHANNEL.getDefaultRequirement().getName())) {
            new ProvidedIdentityGroupServiceConfigurator(null, LocalGroupServiceConfiguratorProvider.LOCAL).configure(context).build(target).install();
        }
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {

        new ProvidedGroupServiceConfigurator<>(LocalGroupServiceConfiguratorProvider.class, LocalGroupServiceConfiguratorProvider.LOCAL).remove(context);

        if (!context.getCapabilityServiceSupport().hasCapability(JGroupsRequirement.CHANNEL.getDefaultRequirement().getName())) {
            new ProvidedIdentityGroupServiceConfigurator(null, LocalGroupServiceConfiguratorProvider.LOCAL).remove(context);
        }
    }
}
