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

import java.security.AccessController;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.weld.ejb.api.SessionObjectReference;

/**
 * Implementation for non-stateful beans.
 *
 * @author Stuart Douglas
 */
public class SessionObjectReferenceImpl implements SessionObjectReference {

    private final Map<Class<?>, ServiceName> viewServices;

    private final String ejbName;

    private transient Map<String, ManagedReference> businessInterfaceToReference;

    public SessionObjectReferenceImpl(EjbDescriptorImpl<?> descriptor) {
        ejbName = descriptor.getEjbName();
        this.viewServices = descriptor.getViewServices();

    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public synchronized <S> S getBusinessObject(Class<S> businessInterfaceType) {
        final String businessInterfaceName = businessInterfaceType.getName();
        ManagedReference managedReference = null;

        if (businessInterfaceToReference == null) {
            businessInterfaceToReference = new HashMap<String, ManagedReference>();
        } else {
            managedReference = businessInterfaceToReference.get(businessInterfaceName);
        }

        if (managedReference == null) {
            if (viewServices.containsKey(businessInterfaceType)) {
                final ServiceController<?> serviceController = currentServiceContainer().getRequiredService(
                        viewServices.get(businessInterfaceType));
                final ComponentView view = (ComponentView) serviceController.getValue();
                try {
                    managedReference = view.createInstance();
                    businessInterfaceToReference.put(businessInterfaceType.getName(), managedReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw WeldLogger.ROOT_LOGGER.viewNotFoundOnEJB(businessInterfaceType.getName(), ejbName);
            }
        }
        return (S) managedReference.getInstance();
    }

    @Override
    public void remove() {
        //nop
    }

    @Override
    public boolean isRemoved() {
        return false;
    }

    private static ServiceContainer currentServiceContainer() {
        if(System.getSecurityManager() == null) {
            return CurrentServiceContainer.getServiceContainer();
        }
        return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }
}
