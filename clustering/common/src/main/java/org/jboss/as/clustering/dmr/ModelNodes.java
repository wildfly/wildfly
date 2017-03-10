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

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Utility methods for extracting values from a {@link ModelNode}.
 * @author Paul Ferraro
 */
public class ModelNodes {

    /**
     * Returns the value of the node as a float.
     * @param value a model node
     * @return the value of the node as a float.
     */
    public static float asFloat(ModelNode value) {
        return Double.valueOf(value.asDouble()).floatValue();
    }

    /**
     * Returns the value of the node as an Enum value.
     * @param value a model node
     * @return the value of the node as an Enum.
     */
    public static <E extends Enum<E>> E asEnum(ModelNode value, Class<E> targetClass) {
        return Enum.valueOf(targetClass, value.asString());
    }

    /**
     * Returns the optional boolean value of the specified {@link ModelNode}.
     * @param value the model node
     * @return an optional value
     */
    public static Optional<Boolean> optionalBoolean(ModelNode value) {
        return value.isDefined() ? Optional.of(value.asBoolean()) : Optional.empty();
    }

    /**
     * Returns the optional double value of the specified {@link ModelNode}.
     * @param value the model node
     * @return an optional value
     */
    public static OptionalDouble optionalDouble(ModelNode value) {
        return value.isDefined() ? OptionalDouble.of(value.asDouble()) : OptionalDouble.empty();
    }

    /**
     * Returns the optional float value of the specified {@link ModelNode}.
     * @param value the model node
     * @return an optional value
     */
    public static Optional<Float> optionalFloat(ModelNode value) {
        return value.isDefined() ? Optional.of(asFloat(value)) : Optional.empty();
    }

    /**
     * Returns the optional int value of the specified {@link ModelNode}.
     * @param value the model node
     * @return an optional value
     */
    public static OptionalInt optionalInt(ModelNode value) {
        return value.isDefined() ? OptionalInt.of(value.asInt()) : OptionalInt.empty();
    }

    /**
     * Returns the optional long value of the specified {@link ModelNode}.
     * @param value the model node
     * @return an optional value
     */
    public static OptionalLong optionalLong(ModelNode value) {
        return value.isDefined() ? OptionalLong.of(value.asInt()) : OptionalLong.empty();
    }

    /**
     * Returns the optional string value of the specified {@link ModelNode}.
     * @param value the model node
     * @return an optional value
     */
    public static Optional<String> optionalString(ModelNode value) {
        return value.isDefined() ? Optional.of(value.asString()) : Optional.empty();
    }

    /**
     * Returns the optional property value of the specified {@link ModelNode}.
     * @param value the model node
     * @return an optional value
     */
    public static Optional<Property> optionalProperty(ModelNode value) {
        return value.isDefined() ? Optional.of(value.asProperty()) : Optional.empty();
    }

    /**
     * Returns the optional property list value of the specified {@link ModelNode}.
     * @param value the model node
     * @return an optional value
     */
    public static Optional<List<Property>> optionalPropertyList(ModelNode value) {
        return value.isDefined() ? Optional.of(value.asPropertyList()) : Optional.empty();
    }

    /**
     * Returns the optional list value of the specified {@link ModelNode}.
     * @param value the model node
     * @return an optional value
     */
    public static Optional<List<ModelNode>> optionalList(ModelNode value) {
        return value.isDefined() ? Optional.of(value.asList()) : Optional.empty();
    }

    /**
     * Returns the optional enum value of the specified {@link ModelNode}.
     * @param value the model node
     * @return an optional value
     */
    public static <E extends Enum<E>> Optional<E> optionalEnum(ModelNode value, Class<E> targetClass) {
        return value.isDefined() ? Optional.of(asEnum(value, targetClass)) : Optional.empty();
    }

    private ModelNodes() {
        // Hide
    }
}
