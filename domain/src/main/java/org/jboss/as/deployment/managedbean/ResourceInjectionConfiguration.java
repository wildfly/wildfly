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

/**
 * Configuration object used to store information for an @Resource injection for a managed bean. 
 *
 * @author John E. Bailey
 */
public class ResourceInjectionConfiguration implements Serializable {
    private static final long serialVersionUID = 3405348115132260519L;

    /**
     * The target of the resource injection annotation.
     */
    public static enum TargetType {
        FIELD, METHOD;
    }

    private final String name;
    private final String injectedType;
    private final TargetType targetType;

    /**
     * Construct an instance.
     *
     * @param name The name of the target
     * @param injectedType The class type of the object being injected
     * @param targetType The type of target (field or method)
     */
    public ResourceInjectionConfiguration(String name, String injectedType, TargetType targetType) {
        this.name = name;
        this.injectedType = injectedType;
        this.targetType = targetType;
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
    public String getInjectedType() {
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
}
