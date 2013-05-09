/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
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
 * /
 */

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
