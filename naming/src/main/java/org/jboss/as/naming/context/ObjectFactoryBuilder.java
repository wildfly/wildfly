/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming.context;

import java.security.AccessController;
import java.util.Hashtable;
import java.util.StringTokenizer;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.directory.Attributes;
import javax.naming.spi.DirObjectFactory;
import javax.naming.spi.ObjectFactory;

import org.jboss.as.naming.ServiceAwareObjectFactory;
import org.jboss.as.naming.logging.NamingLogger;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceContainer;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * ObjectFactoryBuilder implementation used to support custom object factories being loaded from modules. This class
 * also provides the default object factory implementation.
 *
 * @author John Bailey
 */
public class ObjectFactoryBuilder implements javax.naming.spi.ObjectFactoryBuilder, DirObjectFactory {

    public static final ObjectFactoryBuilder INSTANCE = new ObjectFactoryBuilder();

    private ObjectFactoryBuilder() {
    }

    /**
     * Create an object factory.  If the object parameter is a reference it will attempt to create an {@link javax.naming.spi.ObjectFactory}
     * from the reference.  If the parameter is not a reference, or the reference does not create an {@link javax.naming.spi.ObjectFactory}
     * it will return {@code this} as the {@link javax.naming.spi.ObjectFactory} to use.
     *
     * @param obj         The object bound in the naming context
     * @param environment The environment information
     * @return The object factory the object resolves to
     * @throws NamingException If any problems occur
     */
    public ObjectFactory createObjectFactory(final Object obj, Hashtable<?, ?> environment) throws NamingException {
        try {
            if (obj instanceof Reference) {
                return factoryFromReference((Reference) obj, environment);
            }
        } catch (Throwable ignored) {
        }
        return this;
    }

    /**
     * Create an object instance.
     *
     * @param ref         Object containing reference information
     * @param name        The name relative to nameCtx
     * @param nameCtx     The naming context
     * @param environment The environment information
     * @return The object
     * @throws Exception If any error occur
     */
    public Object getObjectInstance(final Object ref, final Name name, final Context nameCtx, final Hashtable<?, ?> environment) throws Exception {
        final ClassLoader classLoader = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        if (classLoader == null) {
            return ref;
        }
        final String factoriesProp = (String) environment.get(Context.OBJECT_FACTORIES);
        if (factoriesProp != null) {
            final String[] classes = factoriesProp.split(":");
            for (String className : classes) {
                try {
                    final Class<?> factoryClass = classLoader.loadClass(className);
                    final ObjectFactory objectFactory = ObjectFactory.class.cast(factoryClass.newInstance());
                    final Object result = objectFactory.getObjectInstance(ref, name, nameCtx, environment);
                    if (result != null) {
                        return result;
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return ref;
    }

    /**
     * Create an object instance.
     *
     * @param ref         Object containing reference information
     * @param name        The name relative to nameCtx
     * @param nameCtx     The naming context
     * @param environment The environment information
     * @param attributes  The directory attributes
     * @return The object
     * @throws Exception If any error occur
     */
    public Object getObjectInstance(final Object ref, final Name name, final Context nameCtx, final Hashtable<?, ?> environment, final Attributes attributes) throws Exception {
        final ClassLoader classLoader = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        if (classLoader == null) {
            return ref;
        }
        final String factoriesProp = (String) environment.get(Context.OBJECT_FACTORIES);
        if (factoriesProp != null) {
            final String[] classes = factoriesProp.split(":");
            for (String className : classes) {
                try {
                    final Class<?> factoryClass = classLoader.loadClass(className);
                    final ObjectFactory objectFactory = ObjectFactory.class.cast(factoryClass.newInstance());
                    final Object result;
                    if (objectFactory instanceof DirObjectFactory) {
                        result = DirObjectFactory.class.cast(objectFactory).getObjectInstance(ref, name, nameCtx, environment, attributes);
                    } else {
                        result = objectFactory.getObjectInstance(ref, name, nameCtx, environment);
                    }
                    if (result != null) {
                        return result;
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return ref;
    }

    private ObjectFactory factoryFromReference(final Reference reference, final Hashtable<?, ?> environment) throws Exception {
        if (reference.getFactoryClassName() == null) {
            return lookForURLs(reference, environment);
        }
        if (reference instanceof ModularReference) {
            return factoryFromModularReference(ModularReference.class.cast(reference), environment);
        }
        return factoryFromReference(reference, WildFlySecurityManager.getCurrentContextClassLoaderPrivileged(), environment);
    }

    private ObjectFactory factoryFromModularReference(ModularReference modularReference, final Hashtable<?, ?> environment) throws Exception {
        final Module module = Module.getCallerModuleLoader().loadModule(modularReference.getModuleIdentifier());
        final ClassLoader classLoader = module.getClassLoader();
        return factoryFromReference(modularReference, classLoader, environment);
    }

    private ObjectFactory factoryFromReference(final Reference reference, final ClassLoader classLoader, final Hashtable<?, ?> environment) throws Exception {
        try {
            final Class<?> factoryClass = classLoader.loadClass(reference.getFactoryClassName());
            ObjectFactory factory = ObjectFactory.class.cast(factoryClass.newInstance());
            if (factory instanceof ServiceAwareObjectFactory) {
                ((ServiceAwareObjectFactory) factory).injectServiceRegistry(currentServiceContainer());
            }
            return factory;
        } catch (Throwable t) {
            throw NamingLogger.ROOT_LOGGER.objectFactoryCreationFailure(t);
        }
    }

    static ObjectFactory lookForURLs(Reference ref, Hashtable environment)
            throws NamingException {

        for (int i = 0; i < ref.size(); i++) {
            RefAddr addr = ref.get(i);
            if (addr instanceof StringRefAddr &&
                    addr.getType().equalsIgnoreCase("URL")) {

                String url = (String) addr.getContent();
                ObjectFactory answer = processURL(url, environment);
                if (answer != null) {
                    return answer;
                }
            }
        }
        return null;
    }

    private static ObjectFactory processURL(Object refInfo, Hashtable environment) throws NamingException {

        if (refInfo instanceof String) {
            String url = (String) refInfo;
            String scheme = getURLScheme(url);
            if (scheme != null) {
                ObjectFactory answer = getURLObjectFactory(scheme, url, environment);
                if (answer != null) {
                    return answer;
                }
            }
        }

        if (refInfo instanceof String[]) {
            String[] urls = (String[]) refInfo;
            for (int i = 0; i < urls.length; i++) {
                String scheme = getURLScheme(urls[i]);
                if (scheme != null) {
                    ObjectFactory answer = getURLObjectFactory(scheme, urls[i], environment);
                    if (answer != null) {
                        return answer;
                    }
                }
            }
        }
        return null;
    }

    private static ObjectFactory getURLObjectFactory(String scheme, String url, Hashtable environment) throws NamingException {

        String facProp = (String) environment.get(Context.URL_PKG_PREFIXES);
        if (facProp != null) {
            facProp += ":" + "com.sun.jndi.url";
        } else {
            facProp = "com.sun.jndi.url";
        }

        ClassLoader loader = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();

        String suffix = "." + scheme + "." + scheme + "URLContextFactory";

        // Not cached; find first factory and cache
        StringTokenizer parser = new StringTokenizer(facProp, ":");
        String className;
        ObjectFactory factory = null;
        while (parser.hasMoreTokens()) {
            className = parser.nextToken() + suffix;
            try {
                Class<?> clazz;
                if (loader == null) {
                    clazz = Class.forName(className);
                } else {
                    clazz = Class.forName(className, true, loader);
                }
                return new ReferenceUrlContextFactoryWrapper((ObjectFactory) clazz.newInstance(), url);
            } catch (InstantiationException | IllegalAccessException e) {
                NamingException ne = new NamingException(className);
                ne.setRootCause(e);
                throw ne;
            } catch (Exception e) {
            }
        }

        return factory;
    }


    private static String getURLScheme(String str) {
        int colon = str.indexOf(':');
        int slash = str.indexOf('/');

        if (colon > 0 && (slash == -1 || colon + 1 == slash))
            return str.substring(0, colon);
        return null;
    }

    private static ServiceContainer currentServiceContainer() {
        if(System.getSecurityManager() == null) {
            return CurrentServiceContainer.getServiceContainer();
        }
        return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }

    private static final class ReferenceUrlContextFactoryWrapper implements ObjectFactory {

        private final ObjectFactory factory;
        private final String url;

        private ReferenceUrlContextFactoryWrapper(final ObjectFactory factory, final String url) {
            this.factory = factory;
            this.url = url;
        }

        @Override
        public Object getObjectInstance(final Object obj, final Name name, final Context nameCtx, final Hashtable<?, ?> environment) throws Exception {
            return factory.getObjectInstance(url, name, nameCtx, environment);
        }
    }
}
