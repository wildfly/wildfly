/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.ejb;

import org.jboss.ejb.client.SessionID;
import org.jboss.msc.service.ServiceName;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Serialized form of a SFSB
 *
 * @author Stuart Douglas
 */
public class SerializedStatefulSessionObject implements Serializable {

    private final String componentServiceName;
    private final SessionID sessionID;
    private final Map<Class<?>, String> serviceNames;

    public SerializedStatefulSessionObject(final ServiceName componentServiceName, final SessionID sessionID, final Map<Class<?>, ServiceName> serviceNames) {
        this.componentServiceName = componentServiceName.getCanonicalName();
        this.sessionID = sessionID;
        Map<Class<?>, String> names = new HashMap<Class<?>, String>();
        for (Map.Entry<Class<?>, ServiceName> e : serviceNames.entrySet()) {
            names.put(e.getKey(), e.getValue().getCanonicalName());
        }
        this.serviceNames = names;
    }

    public SerializedStatefulSessionObject(final String componentServiceName, final SessionID sessionID, final Map<Class<?>, String> serviceNames) {
        this.componentServiceName = componentServiceName;
        this.sessionID = sessionID;
        this.serviceNames = serviceNames;
    }

    private Object readResolve() throws ObjectStreamException {
        Map<Class<?>, ServiceName> names = new HashMap<Class<?>, ServiceName>();
        for (Map.Entry<Class<?>, String> e : serviceNames.entrySet()) {
            names.put(e.getKey(), ServiceName.parse(e.getValue()));
        }
        return new StatefulSessionObjectReferenceImpl(sessionID, ServiceName.parse(componentServiceName), names);
    }

    public String getComponentServiceName() {
        return this.componentServiceName;
    }

    public SessionID getSessionID() {
        return this.sessionID;
    }

    public Map<Class<?>, String> getServiceNames() {
        return serviceNames;
    }
}
