/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.Collection;
import java.util.EnumSet;
import java.util.stream.Stream;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Interface to be implemented by attribute enumerations.
 * @author Paul Ferraro
 */
public interface Attribute extends Definable<AttributeDefinition> {

    /**
     * Returns the name of this attribute.
     * @return the attribute name
     */
    default String getName() {
        return this.getDefinition().getName();
    }

    /**
     * Resolves the value of this attribute from the specified model applying any default value.
     * @param resolver an expression resolver
     * @param model the resource model
     * @return the resolved value
     * @throws OperationFailedException if the value was not valid
     */
    default ModelNode resolveModelAttribute(ExpressionResolver resolver, ModelNode model) throws OperationFailedException {
        return this.getDefinition().resolveModelAttribute(resolver, model);
    }

    /**
     * Convenience method that exposes an Attribute enum as a stream of {@link AttributeDefinition}s.
     * @param <E> the attribute enum type
     * @param enumClass the enum class
     * @return a stream of attribute definitions.
     */
    static <E extends Enum<E> & Attribute> Stream<AttributeDefinition> stream(Class<E> enumClass) {
        return stream(EnumSet.allOf(enumClass));
    }

    /**
     * Convenience method that exposes a set of attributes as a stream of {@link AttributeDefinition}s.
     * @param <A> the attribute type
     * @return a stream of attribute definitions.
     */
    static <A extends Attribute> Stream<AttributeDefinition> stream(Collection<A> attributes) {
        return attributes.stream().map(Attribute::getDefinition);
    }
}
