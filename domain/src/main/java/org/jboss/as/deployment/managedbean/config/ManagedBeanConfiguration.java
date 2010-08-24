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

package org.jboss.as.deployment.managedbean.config;

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
    private List<Method> postConstructMethods;
    private List<Method> preDestroyMethods;
    private List<ResourceConfiguration> resourceConfigurations;
    private List<InterceptorConfiguration> interceptorConfigurations;

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
    public void setName(final String name) {
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
     * Get the post-construct methods.
     *
     * @return the post-construct methods
     */
    public List<Method> getPostConstructMethods() {
        return postConstructMethods;
    }

    /**
     * Set the post-construct methods.
     * 
     * @param postConstructMethods the post-construct methods
     */
    public void setPostConstructMethods(final List<Method> postConstructMethods) {
        this.postConstructMethods = postConstructMethods;
    }

    /**
     * Get the pre-destroy methods.
     *
     * @return the pre-destroy methods
     */
    public List<Method> getPreDestroyMethods() {
        return preDestroyMethods;
    }

    /**
     * Set the pre-destroy methods.
     *
     * @param preDestroyMethods the pre-destroy methods
     */
    public void setPreDestroyMethods(final List<Method> preDestroyMethods) {
        this.preDestroyMethods = preDestroyMethods;
    }

    /**
     * Get the resource configurations.
     *
     * @return the resource configurations
     */
    public List<ResourceConfiguration> getResourceConfigurations() {
        return resourceConfigurations;
    }

    /**
     * Set the resource configurations.
     *
     * @param resourceConfigurations the resource configurations
     */
    public void setResourceConfigurations(List<ResourceConfiguration> resourceConfigurations) {
        this.resourceConfigurations = resourceConfigurations;
    }

    /**
     * Get the interceptor configurations.
     *
     * @return The interceptor configurations.
     */
    public List<InterceptorConfiguration> getInterceptorConfigurations() {
        return interceptorConfigurations;
    }

    /**
     * Set the interceptor configurations.
     *
     * @param interceptorConfigurations The interceptor configurations.
     */
    public void setInterceptorConfigurations(List<InterceptorConfiguration> interceptorConfigurations) {
        this.interceptorConfigurations = interceptorConfigurations;
    }
}
