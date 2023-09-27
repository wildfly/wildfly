/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import java.lang.reflect.InvocationTargetException;

import javax.naming.NamingException;

import org.apache.jasper.runtime.HttpJspBase;
import org.apache.tomcat.InstanceManager;
import org.jboss.as.web.common.WebInjectionContainer;

/**
 *
 * InstanceManager is evil and needs to go away
 *
 *
 * We don't use web injection container for instances of org.apache.jasper.runtime.HttpJspBase, as it causes problems
 * with Jakarta Server Pages hot reload. Because a new Jakarta Server Pages class has the same name the generated ID is exactly the same.
 *
 *
 * @author Stuart Douglas
 */
public class UndertowJSPInstanceManager implements InstanceManager {

    private final WebInjectionContainer webInjectionContainer;

    public UndertowJSPInstanceManager(final WebInjectionContainer webInjectionContainer) {
        this.webInjectionContainer = webInjectionContainer;
    }

    @Override
    public Object newInstance(final String className) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException, ClassNotFoundException {
        return webInjectionContainer.newInstance(className);
    }

    @Override
    public Object newInstance(final String fqcn, final ClassLoader classLoader) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException, ClassNotFoundException {
        return newInstance(classLoader.loadClass(fqcn));
    }

    @Override
    public Object newInstance(final Class<?> c) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException {
        if(HttpJspBase.class.isAssignableFrom(c)) {
            return c.newInstance();
        }
        return webInjectionContainer.newInstance(c);
    }

    @Override
    public void newInstance(final Object o) throws IllegalAccessException, InvocationTargetException, NamingException {
        if(o instanceof HttpJspBase) {
            return;
        }
        webInjectionContainer.newInstance(o);
    }

    @Override
    public void destroyInstance(final Object o) throws IllegalAccessException, InvocationTargetException {
        if(o instanceof HttpJspBase) {
            return;
        }
        webInjectionContainer.destroyInstance(o);
    }
}
