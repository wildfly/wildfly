/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.service;

import org.infinispan.Cache;
import org.jboss.as.clustering.GroupMembershipNotifier;
import org.jboss.as.clustering.GroupRpcDispatcher;
import org.jboss.as.clustering.impl.CoreGroupCommunicationService;
import org.jboss.as.clustering.infinispan.subsystem.CacheService;
import org.jboss.as.clustering.infinispan.subsystem.ChannelDependentServiceProvider;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Paul Ferraro
 */
public class ServiceProviderRegistryServiceProvider implements ChannelDependentServiceProvider {

    @Override
    public ServiceName getServiceName(String cluster) {
        return ServiceProviderRegistryService.getServiceName(cluster);
    }

    @Override
    public ServiceController<?> install(ServiceTarget target, String cluster) {
        final ServiceName name = this.getServiceName(cluster);
        @SuppressWarnings("rawtypes")
        final InjectedValue<Cache> cache = new InjectedValue<Cache>();
        final InjectedValue<GroupRpcDispatcher> dispatcher = new InjectedValue<GroupRpcDispatcher>();
        final InjectedValue<GroupMembershipNotifier> notifier = new InjectedValue<GroupMembershipNotifier>();
        final ServiceName serviceName = CoreGroupCommunicationService.getServiceName(cluster);
        return target.addService(name, new ServiceProviderRegistryService(name, cache, dispatcher, notifier))
                .addDependency(CacheService.getServiceName(cluster, null), Cache.class, cache)
                .addDependency(serviceName, GroupRpcDispatcher.class, dispatcher)
                .addDependency(serviceName, GroupMembershipNotifier.class, notifier)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;
    }
}
