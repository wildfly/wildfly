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

import org.jboss.as.jmx.tcl.TcclMBeanServer;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Basic service managing an MBeanServer instance. Note: Just using the platform mbean server for now.
 *
 * @author John Bailey
 */
public class MBeanServerService implements Service<MBeanServer> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("mbean", "server");

    private MBeanServer mBeanServer;

    public static ServiceController<?> addService(final ServiceTarget batchBuilder, final ServiceListener<Object>... listeners) {
        return batchBuilder.addService(MBeanServerService.SERVICE_NAME, new MBeanServerService())
            .addListener(listeners)
            .setInitialMode(ServiceController.Mode.ACTIVE)
            .install();
    }

    /** {@inheritDoc} */
    public synchronized void start(final StartContext context) throws StartException {
        mBeanServer = new TcclMBeanServer(ManagementFactory.getPlatformMBeanServer());
    }

    /** {@inheritDoc} */
    public synchronized void stop(final StopContext context) {
        mBeanServer = null;
    }

    /** {@inheritDoc} */
    public synchronized MBeanServer getValue() throws IllegalStateException {
        return mBeanServer;
    }
}
