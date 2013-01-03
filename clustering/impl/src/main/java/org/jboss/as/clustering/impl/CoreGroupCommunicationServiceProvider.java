/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012 Red Hat, Inc. and individual contributors
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

package org.jboss.as.clustering.impl;

import org.jboss.as.clustering.infinispan.subsystem.CacheService;
import org.jboss.as.clustering.infinispan.subsystem.ChannelDependentServiceProvider;
import org.jboss.as.clustering.jgroups.subsystem.ChannelService;
import org.jboss.as.server.Services;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.jgroups.Channel;

/**
 * @author Paul Ferraro
 */
public class CoreGroupCommunicationServiceProvider implements ChannelDependentServiceProvider {

    private static final short GROUP_COMMUNICATION_SERVICE_SCOPE = 222;

    @Override
    public ServiceName getServiceName(String cluster) {
        return CoreGroupCommunicationService.getServiceName(cluster);
    }

    @Override
    public ServiceController<?> install(ServiceTarget target, String cluster) {
        final InjectedValue<Channel> channel = new InjectedValue<Channel>();
        final InjectedValue<ModuleLoader> loader = new InjectedValue<ModuleLoader>();
        final Service<CoreGroupCommunicationService> service = new CoreGroupCommunicationService(GROUP_COMMUNICATION_SERVICE_SCOPE, channel, loader);
        return target.addService(this.getServiceName(cluster), service)
                // Make sure Infinispan starts its channel before we try to use it..
                .addDependency(CacheService.getServiceName(cluster, null))
                .addDependency(ChannelService.getServiceName(cluster), Channel.class, channel)
                .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, loader)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;
    }
}
