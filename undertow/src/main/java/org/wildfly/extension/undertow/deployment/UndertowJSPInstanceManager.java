package org.wildfly.extension.undertow.deployment;

import java.lang.reflect.InvocationTargetException;

import javax.naming.NamingException;

import org.apache.tomcat.InstanceManager;
import org.jboss.as.web.common.WebInjectionContainer;

/**
 *
 * InstanceManager is evil and needs to go away
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
        return webInjectionContainer.newInstance(fqcn, classLoader);
    }

    @Override
    public Object newInstance(final Class<?> c) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException {
        return webInjectionContainer.newInstance(c);
    }

    @Override
    public void newInstance(final Object o) throws IllegalAccessException, InvocationTargetException, NamingException {
        webInjectionContainer.newInstance(o);
    }

    @Override
    public void destroyInstance(final Object o) throws IllegalAccessException, InvocationTargetException {
        webInjectionContainer.destroyInstance(o);
    }
}
