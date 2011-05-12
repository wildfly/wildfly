/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld.deployment;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.ComponentViewInstance;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.weld.ejb.api.SessionObjectReference;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO: This is a massive hack
 *
 * @author Stuart Douglas
 * @author Ales Justin
 */
public class SessionObjectReferenceImpl implements SessionObjectReference {

    private volatile boolean removed = false;
    private volatile ComponentViewInstance instance;
    private final boolean stateful;
    private final Map<String, ServiceName> viewServices;
    private final ServiceRegistry serviceRegistry;
    final Component component;


    public SessionObjectReferenceImpl(EjbDescriptorImpl<?> descriptor, ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
        final ServiceName createServiceName = descriptor.getCreateServiceName();
        final ServiceController<?> controller = serviceRegistry.getRequiredService(createServiceName);

        component = (Component) controller.getValue();
        stateful = component instanceof StatefulSessionComponent;
        final Map<String, ServiceName> viewServices = new HashMap<String, ServiceName>();

        for(ViewDescription view : descriptor.getComponentDescription().getViews()) {
            viewServices.put(view.getViewClassName(), view.getServiceName());
        }
        if(stateful && viewServices.size() == 1) {
            final ServiceController<?> serviceController = serviceRegistry.getRequiredService(viewServices.values().iterator().next());
            final ComponentView view = (ComponentView)serviceController.getValue();
            instance = view.createInstance();
        }
        this.viewServices = viewServices;

    }


    @Override
    @SuppressWarnings({"unchecked"})
    public synchronized <S> S getBusinessObject(Class<S> businessInterfaceType) {
        if(removed) {
            return null;
        }
        if(instance != null && stateful) {
            if(businessInterfaceType.equals(instance.getViewClass())) {
                return (S) instance.createProxy();
            } else {
                throw new UnsupportedOperationException("Accessing different views of the same SFSB is not supported yet");
            }
        }
        if(viewServices.containsKey(businessInterfaceType.getName())) {
            final ServiceController<?> serviceController = serviceRegistry.getRequiredService(viewServices.get(businessInterfaceType.getName()));
            final ComponentView view = (ComponentView)serviceController.getValue();
            instance = view.createInstance();
            return (S) instance.createProxy();
        } else {
            throw new IllegalArgumentException("View of type " + businessInterfaceType + " not found on bean " + component);
        }
    }

    @Override
    public void remove() {
        if(instance != null) {
            instance.destroy();
        }
        removed = true;
    }

    @Override
    public boolean isRemoved() {
        return removed;
    }
}
