/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.jgroups.subsystem;

import javax.management.MBeanServer;

import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.clustering.jgroups.DefaultChannelFactory;
import org.jboss.as.clustering.jgroups.ProtocolStackConfiguration;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Paul Ferraro
 */
public class ChannelFactoryService implements Service<ChannelFactory> {
    private static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("jgroups");

    private final InjectedValue<MBeanServer> mbeanServer = new InjectedValue<MBeanServer>();
    private final InjectedValue<ServerEnvironment> environment = new InjectedValue<ServerEnvironment>();

    private final String name;
    private final ProtocolStackConfiguration configuration;

    private volatile ChannelFactory factory;

    public static ServiceName getServiceName() {
        return SERVICE_NAME;
    }

    public static ServiceName getServiceName(String name) {
        return SERVICE_NAME.append(name);
    }

    public ChannelFactoryService(String name, ProtocolStackConfiguration configuration) {
        this.name = name;
        this.configuration = configuration;
    }

    ServiceBuilder<ChannelFactory> build(ServiceTarget target) {
        return target.addService(getServiceName(this.name), this)
            .setInitialMode(ServiceController.Mode.ACTIVE)
            .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, this.environment)
            .addDependency(DependencyType.OPTIONAL, ServiceName.JBOSS.append("mbean", "server"), MBeanServer.class, this.mbeanServer);
    }

    @Override
    public void start(StartContext context) throws StartException {
        this.factory = new DefaultChannelFactory(this.name, this.configuration, this.environment.getValue().getNodeName());
    }

    @Override
    public void stop(StopContext context) {
        this.factory = null;
    }

    @Override
    public ChannelFactory getValue() {
        return this.factory;
    }
}
