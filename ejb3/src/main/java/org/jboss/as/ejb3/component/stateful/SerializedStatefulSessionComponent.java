/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
