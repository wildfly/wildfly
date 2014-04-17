/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.server.dispatcher;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.spi.ChannelServiceNames;

/**
 * Service that provides a non-clustered {@link CommandDispatcherFactory}.
 * @author Paul Ferraro
 */
public class LocalCommandDispatcherFactoryService implements Service<CommandDispatcherFactory> {

    public static ServiceBuilder<CommandDispatcherFactory> build(ServiceTarget target, ServiceName name, String cluster) {
        LocalCommandDispatcherFactoryService service = new LocalCommandDispatcherFactoryService();
        return target.addService(name, service)
                .addDependency(ChannelServiceNames.GROUP.getServiceName(cluster), Group.class, service.group)
        ;
    }

    private final InjectedValue<Group> group = new InjectedValue<>();

    private volatile CommandDispatcherFactory factory;

    private LocalCommandDispatcherFactoryService() {
        // Hide
    }

    @Override
    public CommandDispatcherFactory getValue() {
        return this.factory;
    }

    @Override
    public void start(StartContext context) throws StartException {
        this.factory = new LocalCommandDispatcherFactory(this.group.getValue());
    }

    @Override
    public void stop(StopContext context) {
        this.factory = null;
    }
}
