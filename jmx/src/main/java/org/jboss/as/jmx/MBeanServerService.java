/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jmx;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;

import org.jboss.as.controller.ModelController;
import org.jboss.as.jmx.model.ModelControllerMBeanServerPlugin;
import org.jboss.as.server.Services;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Basic service managing and wrapping an MBeanServer instance. Note: Just using the platform mbean server for now.
 *
 * @author John Bailey
 * @author Kabir Khan
 */
public class MBeanServerService implements Service<PluggableMBeanServer> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("mbean", "server");

    private final boolean showModel;
    private final InjectedValue<ModelController> modelControllerValue = new InjectedValue<ModelController>();

    private PluggableMBeanServer mBeanServer;
    private MBeanServerPlugin showModelPlugin;

    private MBeanServerService(final boolean showModel) {
        this.showModel = showModel;
    }

    public static ServiceController<?> addService(final ServiceTarget batchBuilder, final boolean showModel, final ServiceListener<Object>... listeners) {
        MBeanServerService service = new MBeanServerService(showModel);
        return batchBuilder.addService(MBeanServerService.SERVICE_NAME, service)
            .addListener(listeners)
            .setInitialMode(ServiceController.Mode.ACTIVE)
            .addDependency(DependencyType.OPTIONAL, Services.JBOSS_SERVER_CONTROLLER, ModelController.class, service.modelControllerValue)
            .install();
    }

    /** {@inheritDoc} */
    public synchronized void start(final StartContext context) throws StartException {
        //If the platform MBeanServer was set up to be the PluggableMBeanServer, use that otherwise create a new one and delegate
        MBeanServer platform = ManagementFactory.getPlatformMBeanServer();
        PluggableMBeanServer pluggable = platform instanceof PluggableMBeanServer ? (PluggableMBeanServer)platform : new PluggableMBeanServer(platform);
        if (showModel) {
            showModelPlugin = new ModelControllerMBeanServerPlugin(modelControllerValue.getValue());
            pluggable.addPlugin(showModelPlugin);
        }
        mBeanServer = pluggable;
    }

    /** {@inheritDoc} */
    public synchronized void stop(final StopContext context) {
        mBeanServer.removePlugin(showModelPlugin);
        mBeanServer = null;
    }

    /** {@inheritDoc} */
    public synchronized PluggableMBeanServer getValue() throws IllegalStateException {
        return mBeanServer;
    }
}
