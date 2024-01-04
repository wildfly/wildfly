/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.mockprovider.skipquerydetach;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import jakarta.persistence.Query;

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
                if (interfaceClass.equals(jakarta.persistence.Query.class)) {
                    proxyInterfaces = targetInterfaces;                     // targetInterfaces already has all interfaces
                    alreadyHasInterfaceClass = true;
                    break;
                }
                proxyInterfaces[1 + interfaceIndex] = interfaceClass;
            }
            if (!alreadyHasInterfaceClass) {
                proxyInterfaces[0] = jakarta.persistence.Query.class;
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
