/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.mockprovider.txtimeout;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import jakarta.persistence.EntityManager;

/**
 * TestEntityManagerFactory
 *
 * @author Scott Marlow
 */
public class TestEntityManagerFactory implements InvocationHandler {

    private static final List<String> invocations = Collections.synchronizedList(new ArrayList<String>());

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        invocations.add(method.getName());
        if (method.getName().equals("createEntityManager")) {
            return entityManager();
        } else if(method.getName().equals("getProperties")) {
            return new HashMap<String, Object>();
        } else if(method.getName().equals("isOpen")) {
            return true;
        } else {
            // System.out.println("TestEntityManagerFactory method=" + method.getName() + " is returning null");
        }
        return null;

    }

    private EntityManager entityManager() {
        TestEntityManager testEntityManager =
                new TestEntityManager();
        Class[] targetInterfaces = EntityManager.class.getInterfaces();
        Class[] proxyInterfaces = new Class[targetInterfaces.length + 1];  // include extra element for extensionClass
        boolean alreadyHasInterfaceClass = false;
        for (int interfaceIndex = 0; interfaceIndex < targetInterfaces.length; interfaceIndex++) {
            Class interfaceClass = targetInterfaces[interfaceIndex];
            if (interfaceClass.equals(EntityManager.class)) {
                proxyInterfaces = targetInterfaces;                     // targetInterfaces already has all interfaces
                alreadyHasInterfaceClass = true;
                break;
            }
            proxyInterfaces[1 + interfaceIndex] = interfaceClass;
        }
        if (!alreadyHasInterfaceClass) {
            proxyInterfaces[0] = EntityManager.class;
        }

        EntityManager proxyEntityManager = (EntityManager) Proxy.newProxyInstance(
                testEntityManager.getClass().getClassLoader(),
                proxyInterfaces,
                testEntityManager
        );
        return proxyEntityManager;
    }

}
