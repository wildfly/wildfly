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
import javax.naming.Referenceable;
import javax.naming.directory.Attributes;
import javax.naming.spi.DirObjectFactory;
import javax.naming.spi.ObjectFactory;

import org.jboss.as.naming.ServiceAwareObjectFactory;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.modules.Module;

import static org.jboss.as.naming.NamingMessages.MESSAGES;

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
     * @param obj The object bound in the naming context
     * @param environment The environment information
     * @return The object factory the object resolves to
     * @throws NamingException If any problems occur
     */
    public ObjectFactory createObjectFactory(final Object obj, Hashtable<?, ?> environment) throws NamingException {
        try {
            if (obj instanceof Reference) {
                return factoryFromReference((Reference) obj, environment);
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
        final ClassLoader classLoader = SecurityActions.getContextClassLoader();
        if(classLoader == null) {
            return ref;
        }
        final String factoriesProp = (String)environment.get(Context.OBJECT_FACTORIES);
        if(factoriesProp != null) {
            final String[] classes = factoriesProp.split(":");
            for(String className : classes) {
                try {
                    final Class<?> factoryClass = classLoader.loadClass(className);
                    final ObjectFactory objectFactory = ObjectFactory.class.cast(factoryClass.newInstance());
                    final Object result = objectFactory.getObjectInstance(ref, name, nameCtx, environment);
                    if(result != null) {
                        return result;
                    }
                } catch(Throwable ignored) {
                }
            }
        }
        return ref;
    }

    /**
     * Create an object instance.
     *
     * @param ref Object containing reference information
     * @param name The name relative to nameCtx
     * @param nameCtx The naming context
     * @param environment The environment information
     * @param attributes The directory attributes
     * @return The object
     * @throws Exception If any error occur
     */
    public Object getObjectInstance(final Object ref, final Name name, final Context nameCtx, final Hashtable<?, ?> environment, final Attributes attributes) throws Exception {
        final ClassLoader classLoader = SecurityActions.getContextClassLoader();
        if(classLoader == null) {
            return ref;
        }
        final String factoriesProp = (String)environment.get(Context.OBJECT_FACTORIES);
        if(factoriesProp != null) {
            final String[] classes = factoriesProp.split(":");
            for(String className : classes) {
                try {
                    final Class<?> factoryClass = classLoader.loadClass(className);
                    final ObjectFactory objectFactory = ObjectFactory.class.cast(factoryClass.newInstance());
                    final Object result;
                    if(objectFactory instanceof DirObjectFactory) {
                        result = DirObjectFactory.class.cast(objectFactory).getObjectInstance(ref, name, nameCtx, environment, attributes);
                    } else {
                        result = objectFactory.getObjectInstance(ref, name, nameCtx, environment);
                    }
                    if(result != null) {
                        return result;
                    }
                } catch(Throwable ignored) {
                }
            }
        }
        return ref;
    }

    private ObjectFactory factoryFromReference(final Reference reference, final Hashtable<?, ?> environment) throws Exception {
        if (reference instanceof ModularReference) {
            return factoryFromModularReference(ModularReference.class.cast(reference), environment);
        }
        return factoryFromReference(reference, SecurityActions.getContextClassLoader(), environment);
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
                ((ServiceAwareObjectFactory) factory).injectServiceRegistry(CurrentServiceContainer.getServiceContainer());
            }
            return factory;
        } catch (Throwable t) {
            throw MESSAGES.objectFactoryCreationFailure(t);
        }
    }
}
