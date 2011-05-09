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
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.weld.ejb.api.SessionObjectReference;

/**
 * TODO: This is a massive hack
 *
 * @author Stuart Douglas
 * @author Ales Justin
 */
public class SessionObjectReferenceImpl implements SessionObjectReference {

    private volatile boolean removed = false;
    private final ComponentInstance instance;

    public SessionObjectReferenceImpl(EjbDescriptorImpl<?> descriptor, ServiceRegistry serviceRegistry) {
        final ServiceName createServiceName = descriptor.getCreateServiceName();
        final ServiceController<?> controller = serviceRegistry.getRequiredService(createServiceName);
        final Component component = (Component) controller.getValue();
        instance = component.createInstance();
    }


    @Override
    @SuppressWarnings({"unchecked"})
    public <S> S getBusinessObject(Class<S> businessInterfaceType) {
        return (S) instance.getInstance();
    }

    @Override
    public void remove() {
        instance.destroy();
        removed = true;
    }

    @Override
    public boolean isRemoved() {
        return removed;
    }
}
