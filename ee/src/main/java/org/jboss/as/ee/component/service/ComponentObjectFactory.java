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

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import org.jboss.as.ee.component.Component;
import org.jboss.as.naming.ServiceReferenceObjectFactory;

/**
 * Object factory used to retrieve instances of beans managed by a {@link org.jboss.as.ee.component.Component}.
 *
 * @author John Bailey
 */
public class ComponentObjectFactory extends ServiceReferenceObjectFactory {
    @SuppressWarnings("unchecked")
    public Object getObjectInstance(Object serviceValue, Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        final Component component = (Component)serviceValue;
        // TODO - temp hack until the factory has the view on it
        return component.createLocalProxy(component.getComponentClass());
    }
}
