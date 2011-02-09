/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.component.service;

import org.jboss.as.ee.component.Component;
import org.jboss.as.naming.ServiceReferenceObjectFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.msc.service.ServiceName;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.Reference;
import java.util.Hashtable;

import static org.jboss.as.naming.util.NamingUtils.asReference;

/**
 * Object factory used to retrieve instances of beans managed by a {@link org.jboss.as.ee.component.Component}.
 *
 * @author John Bailey
 */
public class ComponentObjectFactory extends ServiceReferenceObjectFactory {

    public static Reference createReference(final ServiceName componentServiceName, final Class<?> viewClass) {
        final Reference componentFactoryReference = ServiceReferenceObjectFactory.createReference(componentServiceName, ComponentObjectFactory.class);
        componentFactoryReference.add(new ClassRefAddr("view-class", viewClass));
        return componentFactoryReference;
    }

    @SuppressWarnings("unchecked")
    public Object getObjectInstance(Object serviceValue, Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        final Reference reference = asReference(obj);
        final ClassRefAddr viewClassAddr = (ClassRefAddr) reference.get("view-class");
        if (viewClassAddr == null) {
            throw new NamingException("Invalid context reference.  No 'view-class' reference.");
        }
        final Class<?> viewClass = (Class<?>) viewClassAddr.getContent();
        final Component component = (Component) serviceValue;
        final Interceptor clientInterceptor = component.createClientInterceptor(viewClass);
        return component.createLocalProxy(viewClass, clientInterceptor);
    }
}
