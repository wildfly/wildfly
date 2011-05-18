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
package org.jboss.as.weld.ejb;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.ComponentViewInstance;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;
import org.jboss.as.weld.CurrentServiceRegistry;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
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
public class StatefulSessionObjectReferenceImpl implements SessionObjectReference , Serializable {

    private volatile boolean removed = false;
    private final Map<String, ServiceName> viewServices;
    private transient StatefulSessionComponent ejbComponent;
    private final ServiceName createServiceName;
    private final Serializable id;


    public StatefulSessionObjectReferenceImpl(EjbDescriptorImpl<?> descriptor) {
        createServiceName = descriptor.getCreateServiceName();

        final Map<String, ServiceName> viewServices = new HashMap<String, ServiceName>();
        for (ViewDescription view : descriptor.getComponentDescription().getViews()) {
            viewServices.put(view.getViewClassName(), view.getServiceName());
        }
        id = getComponent().createSession();
        this.viewServices = viewServices;

    }


    @Override
    @SuppressWarnings({"unchecked"})
    public synchronized <S> S getBusinessObject(Class<S> businessInterfaceType) {

        if (viewServices.containsKey(businessInterfaceType.getName())) {
            final ServiceController<?> serviceController = CurrentServiceRegistry.getServiceRegistry().getRequiredService(viewServices.get(businessInterfaceType.getName()));
            final ComponentView view = (ComponentView) serviceController.getValue();
            final ComponentViewInstance instance = view.createInstance(Collections.<Object, Object>singletonMap(StatefulSessionComponent.SESSION_ATTACH_KEY, id));
            return (S) instance.createProxy();
        } else {
            throw new IllegalArgumentException("View of type " + businessInterfaceType + " not found on bean " + getComponent());
        }
    }

    @Override
    public void remove() {
        if (!isRemoved()) {
            getComponent().getCache().remove(id);
            removed = true;
        }
    }

    @Override
    public boolean isRemoved() {
        if(!removed) {
            return getComponent().getCache().get(id) == null;
        }
        return true;
    }

    private StatefulSessionComponent getComponent() {
        if(ejbComponent == null) {
            final ServiceController<?> controller = CurrentServiceRegistry.getServiceRegistry().getRequiredService(createServiceName);
            ejbComponent = (StatefulSessionComponent) controller.getValue();
        }
        return ejbComponent;
    }

}
