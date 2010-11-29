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

import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Service that provides access to the OSGi PackageAdmin.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 15-Oct-2010
 */
public class PackageAdminService implements Service<PackageAdmin> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("osgi", "packageadmin");

    private InjectedValue<BundleContext> injectedContext = new InjectedValue<BundleContext>();
    private PackageAdmin packageAdmin;

    public static void addService(final BatchBuilder batchBuilder) {
        PackageAdminService service = new PackageAdminService();
        ServiceBuilder<?> serviceBuilder = batchBuilder.addService(PackageAdminService.SERVICE_NAME, service);
        serviceBuilder.addDependency(FrameworkService.SERVICE_NAME, BundleContext.class, service.injectedContext);
        serviceBuilder.setInitialMode(Mode.ON_DEMAND);
    }

    public static PackageAdmin getServiceValue(ServiceContainer container) {
        try {
            ServiceController<?> controller = container.getRequiredService(SERVICE_NAME);
            return (PackageAdmin) controller.getValue();
        } catch (ServiceNotFoundException ex) {
            throw new IllegalStateException("Cannot obtain required service: " + SERVICE_NAME);
        }
    }

    public synchronized void start(StartContext context) throws StartException {
        try {
            BundleContext sysContext = injectedContext.getValue();
            ServiceReference sref = sysContext.getServiceReference(PackageAdmin.class.getName());
            packageAdmin = (PackageAdmin) sysContext.getService(sref);
        } catch (Throwable t) {
            throw new StartException("Failed to start PackageAdmin service", t);
        }
    }

    public synchronized void stop(StopContext context) {
    }

    @Override
    public PackageAdmin getValue() throws IllegalStateException {
        return packageAdmin;
    }
}
