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

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.ComponentViewInstance;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.weld.ejb.api.SessionObjectReference;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation for SFSB's
 *
 * @author Stuart Douglas
 */
public class StatefulSessionObjectReferenceImpl implements SessionObjectReference {

    private volatile boolean removed = false;
    private final Map<String, ServiceName> viewServices;
    private final ServiceRegistry serviceRegistry;
    private final StatefulSessionComponent component;
    private final Serializable id;


    public StatefulSessionObjectReferenceImpl(EjbDescriptorImpl<?> descriptor, ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
        final ServiceName createServiceName = descriptor.getCreateServiceName();
        final ServiceController<?> controller = serviceRegistry.getRequiredService(createServiceName);

        component = (StatefulSessionComponent) controller.getValue();
        final Map<String, ServiceName> viewServices = new HashMap<String, ServiceName>();

        for (ViewDescription view : descriptor.getComponentDescription().getViews()) {
            viewServices.put(view.getViewClassName(), view.getServiceName());
        }
        id = component.createSession();
        this.viewServices = viewServices;

    }


    @Override
    @SuppressWarnings({"unchecked"})
    public synchronized <S> S getBusinessObject(Class<S> businessInterfaceType) {

        if (viewServices.containsKey(businessInterfaceType.getName())) {
            final ServiceController<?> serviceController = serviceRegistry.getRequiredService(viewServices.get(businessInterfaceType.getName()));
            final ComponentView view = (ComponentView) serviceController.getValue();
            final ComponentViewInstance instance = view.createInstance(Collections.<Object, Object>singletonMap(StatefulSessionComponent.SESSION_ATTACH_KEY, id));
            return (S) instance.createProxy();
        } else {
            throw new IllegalArgumentException("View of type " + businessInterfaceType + " not found on bean " + component);
        }
    }

    @Override
    public void remove() {
        if (!isRemoved()) {
            component.getCache().remove(id);
            removed = true;
        }
    }

    @Override
    public boolean isRemoved() {
        if(!removed) {
            return component.getCache().get(id) == null;
        }
        return true;
    }
}
