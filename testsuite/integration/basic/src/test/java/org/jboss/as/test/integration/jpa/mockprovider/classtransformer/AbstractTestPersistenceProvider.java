/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.mockprovider.classtransformer;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.ProviderUtil;

/**
 * Abstract test PersistenceProvider. EE-version-specific subclasses extend this.
 *
 * @author Scott Marlow
 */
public abstract class AbstractTestPersistenceProvider implements PersistenceProvider {

    // key = pu name
    private static Map<String, PersistenceUnitInfo> persistenceUnitInfo = new HashMap<String, PersistenceUnitInfo>();

    public static PersistenceUnitInfo getPersistenceUnitInfo(String name) {
        return persistenceUnitInfo.get(name);
    }

    @Override
    public EntityManagerFactory createEntityManagerFactory(String emName, Map map) {
        return null;
    }

    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map map) {
        persistenceUnitInfo.put(info.getPersistenceUnitName(), info);
        TestClassTransformer testClassTransformer = new TestClassTransformer();
        info.addTransformer(testClassTransformer);

        TestEntityManagerFactory testEntityManagerFactory =
                new TestEntityManagerFactory();
        Class[] targetInterfaces = jakarta.persistence.EntityManagerFactory.class.getInterfaces();
        Class[] proxyInterfaces = new Class[targetInterfaces.length + 1];  // include extra element for extensionClass
        boolean alreadyHasInterfaceClass = false;
        for (int interfaceIndex = 0; interfaceIndex < targetInterfaces.length; interfaceIndex++) {
            Class interfaceClass = targetInterfaces[interfaceIndex];
            if (interfaceClass.equals(jakarta.persistence.EntityManagerFactory.class)) {
                proxyInterfaces = targetInterfaces;                     // targetInterfaces already has all interfaces
                alreadyHasInterfaceClass = true;
                break;
            }
            proxyInterfaces[1 + interfaceIndex] = interfaceClass;
        }
        if (!alreadyHasInterfaceClass) {
            proxyInterfaces[0] = jakarta.persistence.EntityManagerFactory.class;
        }

        EntityManagerFactory proxyEntityManagerFactory = (EntityManagerFactory) Proxy.newProxyInstance(
                testEntityManagerFactory.getClass().getClassLoader(), //use the target classloader so the proxy has the same scope
                proxyInterfaces,
                testEntityManagerFactory
        );

        return proxyEntityManagerFactory;
    }

    @Override
    public void generateSchema(PersistenceUnitInfo persistenceUnitInfo, Map map) {

    }

    @Override
    public boolean generateSchema(String s, Map map) {
        return false;
    }

    @Override
    public ProviderUtil getProviderUtil() {
        return null;
    }

    public static void clearLastPersistenceUnitInfo() {
        persistenceUnitInfo.clear();
    }
}

