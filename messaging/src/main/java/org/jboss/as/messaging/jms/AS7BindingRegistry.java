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

package org.jboss.as.messaging.jms;

import org.hornetq.spi.core.naming.BindingRegistry;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.value.Values;

/**
 * @author Jason T. Greene
 */
public class AS7BindingRegistry implements BindingRegistry{

    private ServiceContainer container;

    public AS7BindingRegistry(ServiceContainer container) {
        this.container = container;
    }

    @Override
    public Object getContext() {
        // NOOP
        return null;
    }

    @Override
    public void setContext(Object ctx) {
        // NOOP
    }

    @Override
    public Object lookup(String name) {
        // NOOP
        return null;
    }

    @Override
    public boolean bind(String name, Object obj) {
        final BinderService binderService = new BinderService(name);
        container.addService(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(name), binderService)
                 .addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME, NamingStore.class, binderService.getNamingStoreInjector())
                 .addInjection(binderService.getManagedObjectInjector(), new ValueManagedReferenceFactory(Values.immediateValue(obj)))
                 .setInitialMode(ServiceController.Mode.ACTIVE)
                 .install();

        return true;
    }

    @Override
    public void unbind(String name) {
        ServiceController<?> service = container.getService(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(name));
        if (service != null)
            service.setMode(ServiceController.Mode.REMOVE);
    }

    @Override
    public void close() {
       // NOOP
    }
}
