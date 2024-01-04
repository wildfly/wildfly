/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * An attribute translator that converts a single value to/from a list of values.
 * @author Paul Ferraro
 */
public class ListAttributeTranslation implements AttributeTranslation {
    private static final AttributeValueTranslator READ_TRANSLATOR = new AttributeValueTranslator() {
        @Override
        public ModelNode translate(OperationContext context, ModelNode value) throws OperationFailedException {
            return value.isDefined() ? value.asList().get(0) : value;
        }
    };
    private static final AttributeValueTranslator WRITE_TRANSLATOR = new AttributeValueTranslator() {
        @Override
        public ModelNode translate(OperationContext context, ModelNode value) throws OperationFailedException {
            return new ModelNode().add(value);
        }
    };

    private final Attribute targetAttribute;

    public ListAttributeTranslation(Attribute targetAttribute) {
        this.targetAttribute = targetAttribute;
    }

    @Override
    public Attribute getTargetAttribute() {
        return this.targetAttribute;
    }

    @Override
    public AttributeValueTranslator getReadTranslator() {
        return READ_TRANSLATOR;
    }

    @Override
    public AttributeValueTranslator getWriteTranslator() {
        return WRITE_TRANSLATOR;
    }
}
