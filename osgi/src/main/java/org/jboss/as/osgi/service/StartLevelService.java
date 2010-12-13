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

package org.jboss.as.osgi.service;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.startlevel.StartLevel;

/**
 * Service that provides access to the OSGi StartLevel.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 24-Nov-2010
 */
public class StartLevelService implements Service<StartLevel> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("osgi", "startlevel");

    private InjectedValue<BundleContext> injectedContext = new InjectedValue<BundleContext>();
    private StartLevel startLevel;

    public static void addService(final ServiceTarget batchBuilder) {
        StartLevelService service = new StartLevelService();
        ServiceBuilder<?> serviceBuilder = batchBuilder.addService(StartLevelService.SERVICE_NAME, service);
        serviceBuilder.addDependency(BundleContextService.SERVICE_NAME, BundleContext.class, service.injectedContext);
        serviceBuilder.setInitialMode(Mode.ON_DEMAND);
        serviceBuilder.install();
    }

    public static StartLevel getServiceValue(ServiceContainer container) {
        try {
            ServiceController<?> controller = container.getRequiredService(SERVICE_NAME);
            return (StartLevel) controller.getValue();
        } catch (ServiceNotFoundException ex) {
            throw new IllegalStateException("Cannot obtain required service: " + SERVICE_NAME);
        }
    }

    public synchronized void start(StartContext context) throws StartException {
        try {
            BundleContext sysContext = injectedContext.getValue();
            ServiceReference sref = sysContext.getServiceReference(StartLevel.class.getName());
            startLevel = (StartLevel) sysContext.getService(sref);
        } catch (Throwable t) {
            throw new StartException("Failed to start StartLevel service", t);
        }
    }

    public synchronized void stop(StopContext context) {
    }

    @Override
    public StartLevel getValue() throws IllegalStateException {
        return startLevel;
    }
}
