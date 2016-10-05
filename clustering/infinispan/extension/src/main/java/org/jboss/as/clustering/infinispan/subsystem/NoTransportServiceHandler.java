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

import java.util.EnumSet;
import java.util.ServiceLoader;

import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.GroupAliasBuilderProvider;
import org.wildfly.clustering.spi.LocalGroupBuilderProvider;

/**
 * @author Paul Ferraro
 */
public class NoTransportServiceHandler implements ResourceServiceHandler {

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress containerAddress = context.getCurrentAddress().getParent();
        String name = containerAddress.getLastElement().getValue();

        ServiceTarget target = context.getServiceTarget();

        new NoTransportBuilder(containerAddress).build(target).install();
        new SiteBuilder(containerAddress).build(target).install();

        for (GroupAliasBuilderProvider provider : ServiceLoader.load(GroupAliasBuilderProvider.class, GroupAliasBuilderProvider.class.getClassLoader())) {
            for (Builder<?> builder : provider.getBuilders(context.getCapabilityServiceSupport(), name, LocalGroupBuilderProvider.LOCAL)) {
                builder.build(target).install();
            }
        }
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) {
        PathAddress containerAddress = context.getCurrentAddress().getParent();
        String name = containerAddress.getLastElement().getValue();

        for (GroupAliasBuilderProvider provider : ServiceLoader.load(GroupAliasBuilderProvider.class, GroupAliasBuilderProvider.class.getClassLoader())) {
            for (Builder<?> builder : provider.getBuilders(context.getCapabilityServiceSupport(), name, LocalGroupBuilderProvider.LOCAL)) {
                context.removeService(builder.getServiceName());
            }
        }

        EnumSet.allOf(CacheContainerComponent.class).stream().map(component -> component.getServiceName(containerAddress)).forEach(serviceName -> context.removeService(serviceName));
    }
}
