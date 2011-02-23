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

/**
 * An injection target field or method in a class.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class InjectionTargetDescription {
    private String className;
    private String name;
    private String valueClassName;
    private Type type;

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
     * Set the name of the target property.
     *
     * @param name the name
     */
    public void setName(final String name) {
        this.name = name;
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
     * Set the name of the target class.
     *
     * @param className the class name
     */
    public void setClassName(final String className) {
        this.className = className;
    }

    /**
     * Get the class name which the field or setter method can accept.
     *
     * @return the class name
     */
    public String getValueClassName() {
        return valueClassName;
    }

    /**
     * Set the class name which the field or setter method can accept.
     *
     * @param valueClassName the class name
     */
    public void setValueClassName(final String valueClassName) {
        this.valueClassName = valueClassName;
    }

    /**
     * Get the injection target type.
     *
     * @return the type
     */
    public Type getType() {
        return type;
    }

    /**
     * Set the injection target type.
     *
     * @param type the type
     */
    public void setType(final Type type) {
        this.type = type;
    }
}
