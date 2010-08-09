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
import java.util.List;

/**
 * Configuration object which maintains required information to create instances of managed beans.  
 *
 * @author John E. Bailey
 */
public class ManagedBeanConfiguration implements Serializable {
    private static final long serialVersionUID = 5339916057235989276L;

    private String name;
    private String type;
    private String postConstructMethod;
    private String preDestroyMethod;
    private List<ResourceInjectionConfiguration> resourceInjectionConfigurations;

    /**
     * Default constructor.
     */
    public ManagedBeanConfiguration() {
    }

    /**
     * Constructor taking the managed bean type.
     *
     * @param type the managed bean type
     */
    public ManagedBeanConfiguration(final String type) {
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
    public String getType() {
        return type;
    }

    /**
     * Set the managed bean type
     *
     * @param type the managed bean type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get the post construct method name.
     *
     * @return the post construct method name
     */
    public String getPostConstructMethod() {
        return postConstructMethod;
    }

    /**
     * Set the post construct method name.
     * 
     * @param postConstructMethod the post construct method name
     */
    public void setPostConstructMethod(String postConstructMethod) {
        this.postConstructMethod = postConstructMethod;
    }

    /**
     * Get the pre destroy method name.
     *
     * @return the pre destroy method name
     */
    public String getPreDestroyMethod() {
        return preDestroyMethod;
    }

    /**
     * Set the pre destroy method name.
     *
     * @param preDestroyMethod the pre destroy method name
     */
    public void setPreDestroyMethod(String preDestroyMethod) {
        this.preDestroyMethod = preDestroyMethod;
    }

    /**
     * Get the resource injection configurations.
     *
     * @return the resource injection configurations
     */
    public List<ResourceInjectionConfiguration> getResourceInjectionConfigurations() {
        return resourceInjectionConfigurations;
    }

    /**
     * Set the resource injection configurations.
     *
     * @param resourceInjectionConfigurations the resource injection configurations
     */
    public void setResourceInjectionConfigurations(List<ResourceInjectionConfiguration> resourceInjectionConfigurations) {
        this.resourceInjectionConfigurations = resourceInjectionConfigurations;
    }
}
