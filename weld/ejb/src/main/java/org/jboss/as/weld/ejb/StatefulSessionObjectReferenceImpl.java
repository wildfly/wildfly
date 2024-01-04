/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.ejb;

import java.io.IOException;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponentInstance;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBean;
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
public class StatefulSessionObjectReferenceImpl implements SessionObjectReference {

    private final ServiceName createServiceName;
    private final SessionID id;
    private final StatefulSessionComponent ejbComponent;
    private final Map<Class<?>, ServiceName> viewServices;
    private volatile boolean removed = false;

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
        try (StatefulSessionBean<SessionID, StatefulSessionComponentInstance> bean = this.ejbComponent.getCache().findStatefulSessionBean(this.id)) {
            this.removed = true;
            if (bean != null) {
                bean.remove();
            }
        }
    }

    @Override
    public boolean isRemoved() {
        return this.removed;
    }
}
