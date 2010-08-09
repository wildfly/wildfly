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

package org.jboss.as.deployment.managedbean;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import javax.annotation.ManagedBean;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for managing the creation and life-cycle of a managed bean.  This service will return a new instance
 * of the manged bean each time getValue is called.  In essence this can be used as a factory to create instances of a specific managed
 * bean.
 * 
 * @author John E. Bailey
 */
public class ManagedBeanService<T> implements Service<T> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("managed", "bean");
    private final List<ResourceInjection<?>> resourceInjections = new ArrayList<ResourceInjection<?>>();
    private final InjectedValue<ClassLoader> classLoaderValue = new InjectedValue<ClassLoader>();
    private final ManagedBeanConfiguration managedBeanConfiguration;
    private ClassLoader classLoader;
    private Class<T> beanClass;
    private String name;
    private Method postConstructMethod;


    /**
     * Construct with managed bean configuration.
     * 
     * @param managedBeanConfiguration The managed bean configuration
     */
    public ManagedBeanService(final ManagedBeanConfiguration managedBeanConfiguration) {
        this.managedBeanConfiguration = managedBeanConfiguration;
    }

    /**
     * Start the managed bean.  This will do all the necessary classloading and reflection required to start the managed
     * bean instances.
     * 
     * @param context The service start context
     * @throws StartException if any problems occur
     */
    public void start(StartContext context) throws StartException {
        // Do all the classloader stuff first
        classLoader = classLoaderValue.getValue();
        try {
            beanClass = (Class<T>) classLoader.loadClass(managedBeanConfiguration.getType());
            final ManagedBean managedBeanAnnotation = beanClass.getAnnotation(ManagedBean.class);
            if(managedBeanAnnotation == null)
                throw new StartException("Can not find the @MangedBean annotation for class " + beanClass);
            name = managedBeanAnnotation.value() != null ? managedBeanAnnotation.value() : beanClass.getName();
        } catch (ClassNotFoundException e) {
            throw new StartException("Failed to load managed bean type: " + managedBeanConfiguration.getType(), e);
        }
        final String postConstructMethodName = managedBeanConfiguration.getPostConstructMethod();
        try {
            if (postConstructMethodName != null) {
                postConstructMethod = beanClass.getMethod(postConstructMethodName);
            }
        } catch (NoSuchMethodException e) {
            throw new StartException("Failed to get PostConstruct method '" + postConstructMethodName + "' for managed bean type: " + managedBeanConfiguration.getType(), e);
        }
    }

    /** {@inheritDoc} */
    public void stop(StopContext context) {
    }

    /**
     * Get the value of this managed bean.  This will return a new instance of the managed bean will all the injections
     * complete and the post-construct method called.
     *
     * @return a new instance of the managed bean
     *
     * @throws IllegalStateException if no bean class is available
     */
    public T getValue() throws IllegalStateException {
        if (beanClass == null) {
            throw new IllegalStateException("No class for MangedBean: " + managedBeanConfiguration.getType());
        }

        // Create instance
        final T managedBean;
        try {
            managedBean = beanClass.newInstance();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to instantiate instance of MangedBean: " + beanClass);
        }
        // Execute the injections
        for (ResourceInjection resourceInjection : resourceInjections) {
            resourceInjection.inject();
        }
        // Execute the post construct life-cycle
        if (postConstructMethod != null) {
            try {
                postConstructMethod.invoke(managedBean);
            } catch (Throwable t) {
                throw new RuntimeException("Failed to invoke post construct method '" + postConstructMethod.getName() + "' for class " + beanClass);
            }
        }
        return managedBean;
    }

    /**
     * Add a resource injection for this managed bean.
     * 
     * @param resourceInjection A resource injection
     */
    public void addResourceInjection(final ResourceInjection<?> resourceInjection) {
        this.resourceInjections.add(resourceInjection);
    }

    /**
     * Get the Injector used to inject the classloader into the service.
     *
     * @return the injector
     */
    public Injector<ClassLoader> getClassLoaderInjector() {
        return classLoaderValue;
    }

    /**
     * Get the name of the managed bean
     *
     * @return the managed bean name
     */
    public String getName() {
        return name;
    }
}
