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
package org.jboss.as.ejb3.component;

import java.util.Map;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.ViewInstanceFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBHomeLocator;
import org.jboss.msc.value.ImmediateValue;

/**
 * @author Stuart Douglas
 */
public class RemoteHomeViewInstanceFactory implements ViewInstanceFactory {

    private final String applicationName;
    private final String moduleName;
    private final String distinctName;
    private final String beanName;

    public RemoteHomeViewInstanceFactory(final String applicationName, final String moduleName, final String distinctName, final String beanName) {
        this.applicationName = applicationName == null ? "" : applicationName;
        this.moduleName = moduleName;
        this.distinctName = distinctName;
        this.beanName = beanName;
    }

    @Override
    public ManagedReference createViewInstance(final ComponentView componentView, final Map<Object, Object> contextData) {
        Object value = EJBClient.createProxy(new EJBHomeLocator(componentView.getViewClass(), applicationName, moduleName, beanName, distinctName, Affinity.LOCAL));
        return new ValueManagedReference(new ImmediateValue(value));
    }


}
