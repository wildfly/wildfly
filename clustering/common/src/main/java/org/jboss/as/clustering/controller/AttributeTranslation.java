/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.function.UnaryOperator;

import org.jboss.as.controller.PathAddress;

/**
 * Defines an attribute translation.
 * @author Paul Ferraro
 */
@FunctionalInterface
public interface AttributeTranslation {
    // The translator used by an attribute alias
    AttributeValueTranslator IDENTITY_TRANSLATOR = (context, value) -> value;
    UnaryOperator<PathAddress> IDENTITY_ADDRESS_TRANSFORMATION = UnaryOperator.identity();

    Attribute getTargetAttribute();

    default AttributeValueTranslator getReadTranslator() {
        return IDENTITY_TRANSLATOR;
    }

    default AttributeValueTranslator getWriteTranslator() {
        return IDENTITY_TRANSLATOR;
    }

    default UnaryOperator<PathAddress> getPathAddressTransformation() {
        return IDENTITY_ADDRESS_TRANSFORMATION;
    }
}
