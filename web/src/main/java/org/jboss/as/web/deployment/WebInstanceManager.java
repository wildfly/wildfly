package org.jboss.as.web.deployment;

import java.lang.reflect.InvocationTargetException;

import javax.naming.NamingException;

import org.apache.tomcat.InstanceManager;
import org.jboss.as.web.common.WebInjectionContainer;

/**
 * @author Stuart Douglas
 */
public class WebInstanceManager implements InstanceManager {

    private final WebInjectionContainer injectionContainer;

    public WebInstanceManager(final WebInjectionContainer injectionContainer) {
        this.injectionContainer = injectionContainer;
    }

    @Override
    public Object newInstance(final String s) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException, ClassNotFoundException {
        return injectionContainer.newInstance(s);
    }

    @Override
    public Object newInstance(final String s, final ClassLoader classLoader) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException, ClassNotFoundException {
        return injectionContainer.newInstance(s, classLoader);
    }

    @Override
    public Object newInstance(final Class<?> aClass) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException {
        return injectionContainer.newInstance(aClass);
    }

    @Override
    public void newInstance(final Object o) throws IllegalAccessException, InvocationTargetException, NamingException {
        injectionContainer.newInstance(o);
    }

    @Override
    public void destroyInstance(final Object o) throws IllegalAccessException, InvocationTargetException {
        injectionContainer.destroyInstance(o);
    }
}
