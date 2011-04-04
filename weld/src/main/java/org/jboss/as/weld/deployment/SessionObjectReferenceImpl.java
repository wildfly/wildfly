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
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.weld.ejb.api.SessionObjectReference;

import java.io.Serializable;

/**
 * TODO: This is a massive hack
 *
 * @author Stuart Douglas
 */
public class SessionObjectReferenceImpl implements SessionObjectReference {

    private final EjbDescriptorImpl<?> descriptor;
    private final ServiceRegistry serviceRegistry;
    private final Serializable sessionId;
    private volatile boolean removed = false;
    private volatile Component component;

    public SessionObjectReferenceImpl(EjbDescriptorImpl<?> descriptor, ServiceRegistry serviceRegistry) {
        this.descriptor = descriptor;
        this.serviceRegistry = serviceRegistry;
        final ServiceName createServiceName = descriptor.getCreateServiceName();
        final ServiceController<?> controller = serviceRegistry.getRequiredService(createServiceName);
        component = (Component) controller.getValue();

        if (descriptor.isStateful()) {
            StatefulSessionComponent sfsb = (StatefulSessionComponent) component;
            sessionId = sfsb.createSession();
        } else {
            sessionId = null;
        }
    }


    @Override
    public <S> S getBusinessObject(Class<S> businessInterfaceType) {
        final ServiceName viewServiceName = component.getViewServices().get(businessInterfaceType);
        if (viewServiceName == null) {
            throw new RuntimeException("Could not find view for " + businessInterfaceType);
        }
        final ServiceController<?> viewController = serviceRegistry.getRequiredService(viewServiceName);
        ComponentView view = (ComponentView) viewController.getValue();
        if (descriptor.isStateful()) {
            return (S) view.getViewForInstance(sessionId);
        }
        return (S) view.getReference().getInstance();
    }

    @Override
    public void remove() {
        if (descriptor.isStateful()) {
            //TODO: destroy the EJB's
        }
        removed = true;
    }

    @Override
    public boolean isRemoved() {
        return removed;
    }
}
