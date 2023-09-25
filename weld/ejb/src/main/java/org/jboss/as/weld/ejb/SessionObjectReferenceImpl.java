/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
