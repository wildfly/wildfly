/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.security.AccessController;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.ejb.client.SessionID;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 *
 * Serialized form of a SFSB
 *
 * @author Stuart Douglas
 */
public class SerializedStatefulSessionComponent implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String serviceName;
    private final SessionID sessionID;
    private final Map<Object, Object> serializableInterceptors;
    private final ManagedReference instance;

    public SerializedStatefulSessionComponent(final ManagedReference instance, final SessionID sessionID, final String serviceName, final Map<Object, Object> serializableInterceptors) {
        this.instance = instance;
        this.sessionID = sessionID;
        this.serviceName = serviceName;
        this.serializableInterceptors = serializableInterceptors;
    }


    private Object readResolve() throws ObjectStreamException {
        ServiceName name = ServiceName.parse(serviceName);
        ServiceController<?> service = currentServiceContainer().getRequiredService(name);
        StatefulSessionComponent component = (StatefulSessionComponent) service.getValue();
        final Map<Object, Object> context = new HashMap<Object, Object>();

        for(final Map.Entry<Object, Object> entry : serializableInterceptors.entrySet()) {
            context.put(entry.getKey(), entry.getValue());
        }
        context.put(SessionID.class, sessionID);
        context.put(BasicComponentInstance.INSTANCE_KEY, instance);
        return component.constructComponentInstance(instance, false, context);
    }

    private static ServiceContainer currentServiceContainer() {
        if(System.getSecurityManager() == null) {
            return CurrentServiceContainer.getServiceContainer();
        }
        return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }
}
