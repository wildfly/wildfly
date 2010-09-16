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
import java.lang.reflect.AccessibleObject;

/**
 * Configuration object used to store information for an @Resource injection for a managed bean.
 *
 * @author John E. Bailey
 */
public class ResourceConfiguration implements Serializable {
    private static final long serialVersionUID = 3405348115132260519L;

    /**
     * The target of the resource injection annotation.
     */
    public static enum TargetType { FIELD, METHOD, CLASS }

    private final String name;
    private final AccessibleObject target;
    private final TargetType targetType;
    private final Class<?> injectedType;
    private final String localContextName;
    private final String targetContextName;


    /**
     * Construct an instance.
     *
     * @param name The name of the target
     * @param target The injection target(field or method)
     * @param targetType The type of target (field or method)
     * @param injectedType The type of object to be injected
     * @param localContextName The name to use in the local context
     * @param targetContextName The name to retrieve the value form
     */
    public ResourceConfiguration(final String name, final AccessibleObject target, final TargetType targetType, final Class<?> injectedType, final String localContextName, final String targetContextName) {
        this.name = name;
        this.target = target;
        this.targetType = targetType;
        this.injectedType = injectedType;
        this.localContextName = localContextName;
        this.targetContextName = targetContextName;
    }


    /**
     * Construct an instance.
     *
     * @param name The name of the target
     * @param targetType The type of target (field or method)
     * @param localContextName The name to use in the local context
     * @param targetContextName The name to retrieve the value form
     */
    public ResourceConfiguration(final String name, final TargetType targetType, final String localContextName, final String targetContextName) {
        this.name = name;
        this.target = null;
        this.targetType = targetType;
        this.injectedType = null;
        this.localContextName = localContextName;
        this.targetContextName = targetContextName;
    }

    /**
     * Get the resource injection name.
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the injected type.
     *
     * @return The type
     */
    public Class<?> getInjectedType() {
        return injectedType;
    }

    /**
     * Get annotation type target.
     *
     * @return The target type
     */
    public TargetType getTargetType() {
        return targetType;
    }

    /**
     * Get the local context name.
     *
     * @return The local context name
     */
    public String getLocalContextName() {
        return localContextName;
    }

    /**
     * Get the target context name
     *
     * @return The target context name
     */
    public String getTargetContextName() {
        return targetContextName;
    }

    /**
     * Get the injection targer
     *
     * @return The injection target
     */
    public AccessibleObject getTarget() {
        return target;
    }
}
