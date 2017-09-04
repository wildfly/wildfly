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

import java.io.IOException;
import java.io.Serializable;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.cache.Cache;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponentInstance;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.as.weld._private.WeldEjbLogger;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.ejb.client.SessionID;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.weld.ejb.api.SessionObjectReference;

/**
 * Implementation for SFSB's
 *
 * @author Stuart Douglas
 */
public class StatefulSessionObjectReferenceImpl implements SessionObjectReference, Serializable {

    private volatile boolean removed = false;

    private final ServiceName createServiceName;
    private final SessionID id;
    private final StatefulSessionComponent ejbComponent;
    private final Map<Class<?>, ServiceName> viewServices;

    private transient Map<String, ManagedReference> businessInterfaceToReference;

    public StatefulSessionObjectReferenceImpl(final SessionID id, final ServiceName createServiceName, final Map<Class<?>, ServiceName> viewServices) {
        this.id = id;
        this.createServiceName = createServiceName;
        this.viewServices = viewServices;
        final ServiceController<?> controller = currentServiceContainer().getRequiredService(createServiceName);
        ejbComponent = (StatefulSessionComponent) controller.getValue();
    }

    public StatefulSessionObjectReferenceImpl(EjbDescriptorImpl<?> descriptor) {
        this.createServiceName = descriptor.getCreateServiceName();
        final ServiceController<?> controller = currentServiceContainer().getRequiredService(createServiceName);
        ejbComponent = (StatefulSessionComponent) controller.getValue();
        this.id = ejbComponent.createSession();
        this.viewServices = descriptor.getViewServices();

    }


    @Override
    @SuppressWarnings({ "unchecked" })
    public synchronized <S> S getBusinessObject(Class<S> businessInterfaceType) {
        if (isRemoved()) {
            throw WeldEjbLogger.ROOT_LOGGER.ejbHashBeenRemoved(ejbComponent);
        }

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
                    managedReference = view.createInstance(Collections.<Object, Object> singletonMap(SessionID.class, id));
                    businessInterfaceToReference.put(businessInterfaceName, managedReference);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw WeldLogger.ROOT_LOGGER
                        .viewNotFoundOnEJB(businessInterfaceType.getName(), ejbComponent.getComponentName());
            }
        }
        return (S) managedReference.getInstance();
    }

    private static ServiceContainer currentServiceContainer() {
        if(System.getSecurityManager() == null) {
            return CurrentServiceContainer.getServiceContainer();
        }
        return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }

    protected Object writeReplace() throws IOException {
        return new SerializedStatefulSessionObject(createServiceName, id, viewServices);
    }

    @Override
    public void remove() {
        if (!isRemoved()) {
            ejbComponent.removeSession(id);
            removed = true;
        }
    }

    @Override
    public boolean isRemoved() {
        if (!removed) {
            Cache<SessionID, StatefulSessionComponentInstance> cache = ejbComponent.getCache();
            if(cache == null) {
                return true;
            }
            return !cache.contains(id);
        }
        return true;
    }


}
