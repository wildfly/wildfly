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

import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.ejb.client.EJBClient;
import org.jboss.msc.value.ImmediateValue;

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
        this.appName = appName;
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
            throw new RuntimeException("Could not load view class for ejb " + beanName, e);
        }

        final Object proxy = EJBClient.getProxy(appName, moduleName, distinctName, viewClass, beanName);

        if (stateful) {
            EJBClient.createSession(proxy);
        }
        return new ValueManagedReference(new ImmediateValue<Object>(proxy));
    }
}
