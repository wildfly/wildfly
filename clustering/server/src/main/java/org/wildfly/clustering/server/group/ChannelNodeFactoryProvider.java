/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.server.group;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.clustering.infinispan.subsystem.GlobalComponentRegistryService;
import org.jboss.as.clustering.jgroups.subsystem.ChannelService;
import org.jboss.as.clustering.jgroups.subsystem.ChannelServiceProvider;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.jgroups.Channel;

/**
 * Installs a {@link ChannelNodeFactory} service per channel.
 * @author Paul Ferraro
 */
public class ChannelNodeFactoryProvider implements ChannelServiceProvider {

    public static ServiceName getServiceName(String cluster) {
        return ServiceName.JBOSS.append("clustering", "nodes", cluster);
    }

    @Override
    public Collection<ServiceName> getServiceNames(String cluster) {
        return Collections.singleton(getServiceName(cluster));
    }

    @Override
    public Collection<ServiceController<?>> install(ServiceTarget target, String cluster, ModuleIdentifier moduleId) {
        InjectedValue<Channel> channel = new InjectedValue<>();
        ServiceController<?> controller = target.addService(getServiceName(cluster), new ChannelNodeFactoryService(channel))
                .addDependency(GlobalComponentRegistryService.getServiceName(cluster))
                .addDependency(ChannelService.getServiceName(cluster), Channel.class, channel)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;
        return Collections.<ServiceController<?>>singleton(controller);
    }
}
