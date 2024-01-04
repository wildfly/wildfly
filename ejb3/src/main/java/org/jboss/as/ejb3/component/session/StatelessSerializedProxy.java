/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.session;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.security.AccessController;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Serialized form of a singleton or session bean
 *
 * @author Stuart Douglas
 */
public class StatelessSerializedProxy implements Serializable {

    private static final long serialVersionUID = 45678904536435L;

    private final String viewName;

    public StatelessSerializedProxy(final String viewName) {
        this.viewName = viewName;
    }

    public String getViewName() {
        return this.viewName;
    }

    private Object readResolve() throws ObjectStreamException {
        ServiceController<ComponentView> view = (ServiceController<ComponentView>) currentServiceContainer().getRequiredService(ServiceName.parse(viewName));
        try {
            return view.getValue().createInstance().getInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ServiceContainer currentServiceContainer() {
        if(System.getSecurityManager() == null) {
            return CurrentServiceContainer.getServiceContainer();
        }
        return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }
}
