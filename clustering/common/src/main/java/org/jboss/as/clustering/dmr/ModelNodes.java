/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.dmr;

import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleIdentifier;

/**
 * Utility methods for extracting values from a {@link ModelNode}.
 * @author Paul Ferraro
 */
public class ModelNodes {

    /**
     * Returns the value of the node as a string, or null if the node is undefined.
     * @param value a model node
     * @return the value of the node as a string, or null if the node is undefined.
     */
    public static String asString(ModelNode value) {
        return asString(value, null);
    }

    /**
     * Returns the value of the node as a string, or the specified default value if the node is undefined.
     * @param value a model node
     * @return the value of the node as a string, or the specified default value if the node is undefined.
     */
    public static String asString(ModelNode value, String defaultValue) {
        return value.isDefined() ? value.asString() : defaultValue;
    }

    /**
     * Returns the value of the node as a float, or null if the node is undefined.
     * @param value a model node
     * @return the value of the node as a float, or null if the node is undefined.
     */
    public static float asFloat(ModelNode value) {
        return Double.valueOf(value.asDouble()).floatValue();
    }

    /**
     * Returns the value of the node as a string, or the specified default value if the node is undefined.
     * @param value a model node
     * @return the value of the node as a string, or the specified default value if the node is undefined.
     */
    public static float asFloat(ModelNode value, float defaultValue) {
        return Double.valueOf(value.asDouble(defaultValue)).floatValue();
    }

    /**
     * Returns the value of the node as an Enum value, or null if the node is undefined.
     * @param value a model node
     * @return the value of the node as an Enum, or null if the node is undefined.
     */
    public static <E extends Enum<E>> E asEnum(ModelNode value, Class<E> targetClass) {
        return asEnum(value, targetClass, null);
    }

    /**
     * Returns the value of the node as an Enum value, or the specified default value if the node is undefined.
     * @param value a model node
     * @return the value of the node as an Enum, or the specified default if the node is undefined.
     */
    public static <E extends Enum<E>> E asEnum(ModelNode value, Class<E> targetClass, E defaultValue) {
        return value.isDefined() ? Enum.valueOf(targetClass, value.asString()) : defaultValue;
    }

    /**
     * Returns the value of the node as a module identifier, or null if the node is undefined.
     * @param value a model node
     * @return the value of the node as a module identifier, or null if the node is undefined.
     */
    public static ModuleIdentifier asModuleIdentifier(ModelNode value) {
        return asModuleIdentifier(value, null);
    }

    /**
     * Returns the value of the node as a module identifier, or the specified default if the node is undefined.
     * @param value a model node
     * @return the value of the node as a module identifier, or the specified default if the node is undefined.
     */
    public static ModuleIdentifier asModuleIdentifier(ModelNode value, ModuleIdentifier defaultValue) {
        return value.isDefined() ? ModuleIdentifier.fromString(value.asString()) : defaultValue;
    }

    private ModelNodes() {
        // Hide
    }
}
