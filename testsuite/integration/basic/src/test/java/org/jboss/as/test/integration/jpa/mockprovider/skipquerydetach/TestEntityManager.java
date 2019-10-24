/*
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2019, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.jpa.mockprovider.skipquerydetach;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.persistence.Query;

/**
 * TestEntityManager that implements createQuery
 *
 * @author Scott Marlow
 */
public class TestEntityManager implements InvocationHandler {

    private static final List<String> invocations = Collections.synchronizedList(new ArrayList<String>());
    private Properties properties;
    public TestEntityManager(Properties properties) {
        this.properties = properties;
    }

    public static List<String> getInvocations() {
        return invocations;
    }

    public static void clear() {
        invocations.clear();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        invocations.add(method.getName());
        if (method.getName().equals("isOpen")) {
            return true;
        } else if (method.getName().equals("createQuery")) {
            TestQuery testQuery =
                    new TestQuery();
            Class[] targetInterfaces = Query.class.getInterfaces();
            Class[] proxyInterfaces = new Class[targetInterfaces.length + 1];  // include extra element for extensionClass
            boolean alreadyHasInterfaceClass = false;
            for (int interfaceIndex = 0; interfaceIndex < targetInterfaces.length; interfaceIndex++) {
                Class interfaceClass = targetInterfaces[interfaceIndex];
                if (interfaceClass.equals(javax.persistence.Query.class)) {
                    proxyInterfaces = targetInterfaces;                     // targetInterfaces already has all interfaces
                    alreadyHasInterfaceClass = true;
                    break;
                }
                proxyInterfaces[1 + interfaceIndex] = interfaceClass;
            }
            if (!alreadyHasInterfaceClass) {
                proxyInterfaces[0] = javax.persistence.Query.class;
            }

            Query proxyQuery = (Query) Proxy.newProxyInstance(
                    testQuery.getClass().getClassLoader(), //use the target classloader so the proxy has the same scope
                    proxyInterfaces,
                    testQuery
            );
            return proxyQuery;
        } else if (method.getName().equals("close")) {
        }
        return null;

    }
}
