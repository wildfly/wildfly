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

package org.jboss.as.demos.managedbean.archive;

import javax.naming.Context;
import org.jboss.as.ee.naming.ContextNames;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author John Bailey
 */
public class ServiceActivator implements org.jboss.msc.service.ServiceActivator {
    public void activate(ServiceActivatorContext serviceActivatorContext) throws ServiceRegistryException {

        System.out.println("Running");

        final ServiceTarget serviceTarget = serviceActivatorContext.getServiceTarget();

        final LookupService rebindService = new LookupService("BeanWithSimpleInjected");

        serviceTarget.addService(ServiceName.JBOSS.append("BeanWithSimpleInjected", "rebind"), rebindService)
            .addDependency(ContextNames.MODULE_CONTEXT_SERVICE_NAME.append("managedbean-example.jar"), Context.class, rebindService.getLookupContextInjector())
            .addDependency(ServiceName.JBOSS.append("deployment", "managedbean-example.jar", "component", "BeanWithSimpleInjected"))
            .setInitialMode(ServiceController.Mode.ACTIVE)
            .install();
    }
}
