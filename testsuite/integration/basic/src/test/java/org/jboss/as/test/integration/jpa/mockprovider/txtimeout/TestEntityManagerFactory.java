/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jpa.mockprovider.txtimeout;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;

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
