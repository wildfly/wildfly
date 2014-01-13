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
