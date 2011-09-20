/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.arquillian.container;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * A simple MBeanProxy
 *
 * @author Thomas.Diesler@jboss.com
 * @since 24-Feb-2009
 */
public class MBeanProxy {

    public static <T> T get(MBeanServerConnection server, String name, Class<T> interf) {
        ObjectName oname;
        try {
            oname = ObjectName.getInstance(name);
        } catch (MalformedObjectNameException ex) {
            throw new IllegalArgumentException("Invalid object name: " + name);
        }
        return (T) MBeanProxy.get(server, oname, interf);
    }

    public static <T> T get(MBeanServerConnection server, ObjectName name, Class<T> interf) {
        return (T) MBeanServerInvocationHandler.newProxyInstance(server, name, interf, false);
    }
}
