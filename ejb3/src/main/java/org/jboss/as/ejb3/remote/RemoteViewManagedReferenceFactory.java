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
package org.jboss.as.ejb3.remote;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;

import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBHomeLocator;
import org.jboss.ejb.client.EJBLocator;
import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.StatefulEJBLocator;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.jboss.msc.value.ImmediateValue;

import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

/**
 * Managed reference factory for remote EJB views that are bound to java: JNDI locations
 *
 * @author Stuart Douglas
 */
public class RemoteViewManagedReferenceFactory implements ManagedReferenceFactory {

    private final String appName;
    private final String moduleName;
    private final String distinctName;
    private final String beanName;
    private final String viewClass;
    private final boolean stateful;

    public RemoteViewManagedReferenceFactory(final String appName, final String moduleName, final String distinctName, final String beanName, final String viewClass, final boolean stateful) {
        this.appName = appName == null ? "" : appName;
        this.moduleName = moduleName;
        this.distinctName = distinctName;
        this.beanName = beanName;
        this.viewClass = viewClass;
        this.stateful = stateful;
    }

    @Override
    public ManagedReference getReference() {
        final ClassLoader tccl = SecurityActions.getContextClassLoader();
        final Class<?> viewClass;
        try {
            viewClass = Class.forName(this.viewClass, false, tccl);
        } catch (ClassNotFoundException e) {
            throw MESSAGES.failToLoadViewClassEjb(beanName, e);
        }
        EJBLocator ejbLocator = null;
        if (EJBHome.class.isAssignableFrom(viewClass) || EJBLocalHome.class.isAssignableFrom(viewClass)) {
            ejbLocator = new EJBHomeLocator(viewClass, appName, moduleName, beanName, distinctName);
        } else if (stateful) {
            final SessionID sessionID;
            try {
                sessionID = EJBClient.createSession(appName, moduleName, beanName, distinctName);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            ejbLocator = new StatefulEJBLocator(viewClass, appName, moduleName, beanName, distinctName, sessionID);
        } else {
            ejbLocator = new StatelessEJBLocator(viewClass, appName, moduleName, beanName, distinctName);
        }
        final Object proxy = EJBClient.createProxy(ejbLocator);

        return new ValueManagedReference(new ImmediateValue<Object>(proxy));
    }
}
