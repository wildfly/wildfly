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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
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

    /**
     * Returns the value of the node as a property list, returning an empty list if the node is undefined.
     * @param value a model node
     * @return the value of the node as a property list, returning an empty list if the node is undefined.
     */
    public static List<Property> asPropertyList(ModelNode value) {
        return value.isDefined() ? value.asPropertyList() : Collections.<Property>emptyList();
    }

    /**
     * Returns the value of the node as a list of strings, returning an empty list if the node is undefined.
     * @param value a model node
     * @return the value of the node as a list of strings, returning an empty list if the node is undefined.
     */
    public static List<String> asStringList(ModelNode value) {
        if (!value.isDefined()) {
            return Collections.emptyList();
        }
        List<ModelNode> nodes = value.asList();
        List<String> result = new ArrayList<>(nodes.size());
        for (ModelNode node : value.asList()) {
            result.add(node.asString());
        }
        return result;
    }

    /**
     * Returns the value of the node as a properties map, returning an empty properties map if the node is undefined.
     * @param value a model node
     * @return the value of the node as a properties map, returning an empty properties map if the node is undefined.
     */
    public static Properties asProperties(ModelNode value) {
        Properties properties = new Properties();
        for (Property property : asPropertyList(value)) {
            properties.setProperty(property.getName(), property.getValue().asString());
        }
        return properties;
    }

    private ModelNodes() {
        // Hide
    }
}
