/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.naming.remote.simple;

import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.value.Values;

/**
 * @author John Bailey
 */
public class BindingActivator implements ServiceActivator {
    public void activate(final ServiceActivatorContext serviceActivatorContext) throws ServiceRegistryException {

        final BinderService binding = new BinderService("test");
        serviceActivatorContext.getServiceTarget().addService(ContextNames.EXPORTED_CONTEXT_SERVICE_NAME.append("test"), binding)
            .addDependency(ContextNames.EXPORTED_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class, binding.getNamingStoreInjector())
            .addInjection(binding.getManagedObjectInjector(), new ValueManagedReferenceFactory(Values.immediateValue("TestValue")))
            .install();

        final BinderService nestedBinding = new BinderService("context/test");
        serviceActivatorContext.getServiceTarget().addService(ContextNames.EXPORTED_CONTEXT_SERVICE_NAME.append("context", "test"), nestedBinding)
            .addDependency(ContextNames.EXPORTED_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class, nestedBinding.getNamingStoreInjector())
            .addInjection(nestedBinding.getManagedObjectInjector(), new ValueManagedReferenceFactory(Values.immediateValue("TestValue")))
            .install();
    }
}
