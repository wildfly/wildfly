/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.demos.jpa.runner;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class TestMBeanInvocationHandler implements InvocationHandler {
    private final MBeanServerConnection mbeanServer;
    private final ObjectName name;
    private final String lookupName;

    public TestMBeanInvocationHandler(final MBeanServerConnection server, final String lookupName) {
        this(server, getDefaultObjectName(), lookupName);
    }

    public TestMBeanInvocationHandler(final MBeanServerConnection server, final ObjectName name, final String lookupName) {
        this.mbeanServer = server;
        this.name = name;
        this.lookupName = lookupName;
    }

    private static ObjectName getDefaultObjectName() {
        try {
            return new ObjectName("jboss:name=jpa-test,type=service");
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object[] params = { lookupName, method.getName(), method.getParameterTypes(), args };
        return mbeanServer.invoke(name, "invoke", params, new String[] { String.class.getName(), String.class.getName(), new Class[0].getClass().getName(), new Object[0].getClass().getName() });
    }
}
