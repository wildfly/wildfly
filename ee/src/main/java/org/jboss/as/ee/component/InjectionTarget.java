/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.component;

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.msc.value.Value;

/**
 * An injection target field or method in a class.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class InjectionTarget {
    private final String className;
    private final String name;
    private final String declaredValueClassName;

    /**
     *
     * @param className
     * @param name
     * @param declaredValueClassName
     */
    protected InjectionTarget(final String className, final String name, final String declaredValueClassName) {
        this.className = className;
        this.name = name;
        this.declaredValueClassName = declaredValueClassName;
    }

    /**
     * The injection target type.
     */
    public enum Type {
        /**
         * Method target type.
         */
        METHOD,
        /**
         * Field target type.
         */
        FIELD,
    }

    /**
     * Get the name of the target property.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the name of the target class.
     *
     * @return the class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Get the class name of the field or the parameter type declared for the target method.
     *
     * @return the class name
     */
    public String getDeclaredValueClassName() {
        return declaredValueClassName;
    }

    /**
     * Get an interceptor factory which will carry out injection into this target.
     *
     * @param targetContextKey the interceptor context key for the target
     * @param valueContextKey  the interceptor context key for the value
     * @param factoryValue     the value to inject
     * @param deploymentUnit   the deployment unit
     * @return the interceptor factory
     * @throws DeploymentUnitProcessingException
     *          if an error occurs
     */
    public abstract InterceptorFactory createInjectionInterceptorFactory(final Object targetContextKey, final Object valueContextKey, final Value<ManagedReferenceFactory> factoryValue, final DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException;

}
