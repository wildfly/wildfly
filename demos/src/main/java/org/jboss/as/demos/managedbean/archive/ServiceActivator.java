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

import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.NamingStore;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ServiceTarget;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.NamingException;

/**
 * @author John Bailey
 */
public class ServiceActivator implements org.jboss.msc.service.ServiceActivator {
    public void activate(ServiceActivatorContext serviceActivatorContext) throws ServiceRegistryException {

        System.out.println("Running");

        final ServiceTarget serviceTarget = serviceActivatorContext.getServiceTarget();

        final LookupService rebindService = new LookupService("BeanWithSimpleInjected");
        final Injector<Context> injector = rebindService.getLookupContextInjector();

        serviceTarget.addService(ServiceName.JBOSS.append("BeanWithSimpleInjected", "rebind"), rebindService)
            .addDependency(ServiceName.JBOSS.append("deployment", "subunit","managedbean-example.ear", "managedbean-example.jar", "component", "BeanWithSimpleInjected","START"))
            .addDependency(ContextNames.contextServiceNameOfModule("managedbean-example", "managedbean-example"), NamingStore.class, new Injector<NamingStore>() {
                public void inject(final NamingStore value) throws InjectionException {
                    try {
                        injector.inject((Context) value.lookup(new CompositeName()));
                    } catch (NamingException e) {
                        throw new InjectionException(e);
                    }
                }

                public void uninject() {
                    injector.uninject();
                }
            })
            .setInitialMode(ServiceController.Mode.ACTIVE)
            .install();
    }
}
