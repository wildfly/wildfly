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

import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.clustering.jgroups.JChannelFactory;
import org.jboss.as.clustering.jgroups.ProtocolStackConfiguration;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * @author Paul Ferraro
 */
public class ChannelFactoryService implements Service<ChannelFactory> {
    public static final String DEFAULT = "default";
    static final String FACTORY = "factory";
    private static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(JGroupsExtension.SUBSYSTEM_NAME).append(FACTORY);

    static ContextNames.BindInfo createChannelFactoryBinding(String stack) {
        return ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, JGroupsExtension.SUBSYSTEM_NAME, FACTORY, stack).getAbsoluteName());
    }

    public static ServiceName getServiceName(String name) {
        return SERVICE_NAME.append((name != null) ? name : DEFAULT);
    }

    private final ProtocolStackConfiguration configuration;

    private volatile ChannelFactory factory;

    public ChannelFactoryService(ProtocolStackConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void start(StartContext context) {
        this.factory = new JChannelFactory(this.configuration);
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
