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

import jakarta.persistence.EntityManager;

/**
 * TestEntityManagerFactory
 *
 * @author Scott Marlow
 */
public class TestEntityManagerFactory implements InvocationHandler {

    private Properties properties;
    public TestEntityManagerFactory(Properties properties) {
        this.properties = properties;
    }

    private static final List<String> invocations = Collections.synchronizedList(new ArrayList<String>());

    public static List<String> getInvocations() {
        return invocations;
    }

    public static void clear() {
        invocations.clear();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        invocations.add(method.getName());
        if (method.getName().equals("toString")) {
            return "xxx TestEntityManagerFactory proxy";
        }
        if (method.getName().equals("isOpen")) {
            return true;
        } else if(method.getName().equals("createEntityManager")) {
            TestEntityManager testEntityManager =
                    new TestEntityManager(properties);
            Class[] targetInterfaces = jakarta.persistence.EntityManager.class.getInterfaces();
            Class[] proxyInterfaces = new Class[targetInterfaces.length + 1];  // include extra element for extensionClass
            boolean alreadyHasInterfaceClass = false;
            for (int interfaceIndex = 0; interfaceIndex < targetInterfaces.length; interfaceIndex++) {
                Class interfaceClass = targetInterfaces[interfaceIndex];
                if (interfaceClass.equals(jakarta.persistence.EntityManager.class)) {
                    proxyInterfaces = targetInterfaces;                     // targetInterfaces already has all interfaces
                    alreadyHasInterfaceClass = true;
                    break;
                }
                proxyInterfaces[1 + interfaceIndex] = interfaceClass;
            }
            if (!alreadyHasInterfaceClass) {
                proxyInterfaces[0] = jakarta.persistence.EntityManager.class;
            }

            EntityManager proxyEntityManager = (EntityManager) Proxy.newProxyInstance(
                    testEntityManager.getClass().getClassLoader(), //use the target classloader so the proxy has the same scope
                    proxyInterfaces,
                    testEntityManager
            );
            return proxyEntityManager;
        } else if(method.getName().equals("getProperties")) {
            return properties;
        }
        return null;

    }
}
