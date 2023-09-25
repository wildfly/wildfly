/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.stateful;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.security.AccessController;
import java.util.Collections;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.ejb.client.SessionID;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Serialized form of a SFSB
 *
 * @author Stuart Douglas
 */
public class StatefulSerializedProxy implements Serializable {

    private static final long serialVersionUID = 627023970448688592L;

    private final String viewName;
    private final SessionID sessionID;

    public StatefulSerializedProxy(final String viewName, final SessionID sessionID) {
        this.viewName = viewName;
        this.sessionID = sessionID;
    }

    public String getViewName() {
        return this.viewName;
    }

    public SessionID getSessionID() {
        return this.sessionID;
    }

    private Object readResolve() throws ObjectStreamException {
        ServiceController<ComponentView> view = (ServiceController<ComponentView>) currentServiceContainer().getRequiredService(ServiceName.parse(viewName));
        try {
            return view.getValue().createInstance(Collections.<Object, Object>singletonMap(SessionID.class, sessionID)).getInstance();
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
