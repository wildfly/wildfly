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

package org.jboss.as.test.integration.sar.dependency;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class MBeanActivator implements ServiceActivator {
    public void activate(ServiceActivatorContext context) throws ServiceRegistryException {
        ServiceTarget target = context.getServiceTarget();
        ServiceBuilder<ObjectInstance> builder = target.addService(ServiceName.JBOSS.append("dummy123"), new RegService());
        builder.setInitialMode(ServiceController.Mode.ACTIVE).install();
    }

    private static MBeanServer getServer() {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        System.err.println("server = " + server);
        return server;
    }

    private static class RegService implements Service<ObjectInstance> {
        private ObjectInstance instance;

        public void start(StartContext context) throws StartException {
            try {
                XService service = new XService();
                instance = getServer().registerMBean(service, new ObjectName("jboss:name=mservice"));
            } catch (Exception e) {
                throw new ServiceRegistryException(e);
            }
        }

        public void stop(StopContext context) {
            try {
                getServer().unregisterMBean(instance.getObjectName());
            } catch (Exception ignored) {
            }
        }

        public ObjectInstance getValue() throws IllegalStateException, IllegalArgumentException {
            return instance;
        }
    }
}
