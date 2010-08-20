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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Configuration object which maintains required information to create instances of managed beans.  
 *
 * @author John E. Bailey
 */
public class ManagedBeanConfiguration implements Serializable {
    private static final long serialVersionUID = 5339916057235989276L;

    private String name;
    private Class<?> type;
    private Method postConstructMethod;
    private Method preDestroyMethod;
    private List<ResourceConfiguration> resourceConfigurations;

    /**
     * Default constructor.
     */
    public ManagedBeanConfiguration() {
    }

    /**
     * Constructor taking the managed bean type.
     *
     * @param name The managed bean name
     * @param type the managed bean type
     */
    public ManagedBeanConfiguration(final String name, final Class<?> type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Get the managed bean name.
     *
     * @return the managed bean name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the managed bean name
     * @param name the managed bean name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the managed bean type
     *
     * @return the managed bean type
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * Get the post construct method.
     *
     * @return the post construct method
     */
    public Method getPostConstructMethod() {
        return postConstructMethod;
    }

    /**
     * Set the post construct method.
     * 
     * @param postConstructMethod the post construct method
     */
    public void setPostConstructMethod(Method postConstructMethod) {
        this.postConstructMethod = postConstructMethod;
    }

    /**
     * Get the pre destroy method.
     *
     * @return the pre destroy method
     */
    public Method getPreDestroyMethod() {
        return preDestroyMethod;
    }

    /**
     * Set the pre destroy method.
     *
     * @param preDestroyMethod the pre destroy method
     */
    public void setPreDestroyMethod(Method preDestroyMethod) {
        this.preDestroyMethod = preDestroyMethod;
    }

    /**
     * Get the resource injection configurations.
     *
     * @return the resource injection configurations
     */
    public List<ResourceConfiguration> getResourceInjectionConfigurations() {
        return resourceConfigurations;
    }

    /**
     * Set the resource injection configurations.
     *
     * @param resourceConfigurations the resource injection configurations
     */
    public void setResourceInjectionConfigurations(List<ResourceConfiguration> resourceConfigurations) {
        this.resourceConfigurations = resourceConfigurations;
    }
}
