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

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import static org.jboss.as.naming.util.NamingUtils.asReference;
import static org.jboss.as.naming.util.NamingUtils.namingException;
import org.jboss.modules.Module;

/**
 * ObjectFactoryBuilder implementation used to support custom object factories being loaded from modules. This class
 * also provides the default object factory implementation.
 *
 * @author John Bailey
 */
public class ObjectFactoryBuilder implements javax.naming.spi.ObjectFactoryBuilder, ObjectFactory {

    public static final ObjectFactoryBuilder INSTANCE = new ObjectFactoryBuilder();

    private ObjectFactoryBuilder() {
    }

    /**
     * Create an object factory.  If the object parameter is a reference it will attempt to create an {@link javax.naming.spi.ObjectFactory}
     * from the reference.  If the parameter is not a reference, or the reference does not create an {@link javax.naming.spi.ObjectFactory}
     * it will return {@code this} as the {@link javax.naming.spi.ObjectFactory} to use.
     *
     * @param obj The object bound in the naming context
     * @param environment The environment information
     * @return The object factory the object resolves to
     * @throws NamingException If any problems occur
     */
    public ObjectFactory createObjectFactory(final Object obj, Hashtable<?, ?> environment) throws NamingException {
        try {
            if (obj instanceof Reference) {
                return factoryFromReference(asReference(obj), environment);
            }
        } catch(Throwable ignored) {
        }
        return this;
    }

    /**
     * Create an object instance.
     *
     * @param ref Object containing reference information
     * @param name The name relative to nameCtx
     * @param nameCtx The naming context
     * @param environment The environment information
     * @return The object
     * @throws Exception If any error occur
     */
    public Object getObjectInstance(final Object ref, final Name name, final Context nameCtx, final Hashtable<?, ?> environment) throws Exception {
        ClassLoader classLoader;
        if(ref instanceof ReferenceWithClassLoader) {
            classLoader = ReferenceWithClassLoader.class.cast(ref).getBindingLoader();
        } else {
            classLoader = getClassLoader();
        }
        Object result = getObjectInstanceFromFactories(ref, name, nameCtx, environment, classLoader);
        if(result == null) {
            result = ref;
        }
        return result;
    }

    private Object getObjectInstanceFromFactories(final Object obj, final Name name, final Context nameCtx, final Hashtable<?, ?> environment, final ClassLoader classLoader) {
        final String factoriesProp = (String)environment.get(Context.OBJECT_FACTORIES);
        if(factoriesProp != null) {
            final String[] classes = factoriesProp.split(":");
            for(String className : classes) {
                try {
                    final Class<?> factoryClass = classLoader.loadClass(className);
                    final ObjectFactory objectFactory = ObjectFactory.class.cast(factoryClass.newInstance());
                    final Object result = objectFactory.getObjectInstance(obj, name, nameCtx, environment);
                    if(result != null) {
                        return result;
                    }
                } catch(Throwable ignored) {
                }
            }
        }
        return null;
    }

    private ObjectFactory factoryFromReference(final Reference reference, final Hashtable<?, ?> environment) throws Exception {
        if (reference instanceof ModularReference) {
            return factoryFromModularReference(ModularReference.class.cast(reference), environment);
        } else if(reference instanceof ReferenceWithClassLoader) {
                return factoryFromReferenceClassLoader(ReferenceWithClassLoader.class.cast(reference), environment);
        }
        return factoryFromReference(reference, getClassLoader(), environment);
    }

    private ObjectFactory factoryFromModularReference(ModularReference modularReference, final Hashtable<?, ?> environment) throws Exception {
        final Module module = Module.getCurrentModuleLoader().loadModule(modularReference.getModuleIdentifier());
        final ClassLoader classLoader = module.getClassLoader();
        return factoryFromReference(modularReference, classLoader, environment);
    }

    private ObjectFactory factoryFromReference(final Reference reference, final ClassLoader classLoader, final Hashtable<?, ?> environment) throws Exception {
        try {
            final Class<?> factoryClass = classLoader.loadClass(reference.getFactoryClassName());
            return ObjectFactory.class.cast(factoryClass.newInstance());
        } catch (Throwable t) {
            throw namingException("Failed to create object factory from classloader.", t);
        }
    }

    private ObjectFactory factoryFromReferenceClassLoader(final ReferenceWithClassLoader referenceWithClassLoader, final Hashtable<?, ?> environment) throws Exception {
        return new ReferenceClassLoaderFactory(factoryFromReference(referenceWithClassLoader, referenceWithClassLoader.getBindingLoader(), environment));
    }

    private ClassLoader getClassLoader() {
        ClassLoader classLoader = SecurityActions.getContextClassLoader();
        if(classLoader == null) {
            classLoader = SecurityActions.getCallingClassLoader();
        }
        return classLoader;
    }

    private static class ReferenceClassLoaderFactory implements ObjectFactory {
        final ObjectFactory actualFactory;

        private ReferenceClassLoaderFactory(ObjectFactory actualFactory) {
            this.actualFactory = actualFactory;
        }

        public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
            return actualFactory.getObjectInstance(obj, name, nameCtx, environment);
        }
    }
}
