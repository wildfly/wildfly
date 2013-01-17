/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.transform.description;

import java.util.Collections;
import java.util.List;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * A transformer for a single attribute
 *
 * @author Emanuel Muckenhuber
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class AttributeTransformationDescription {

    final String name;
    final List<RejectAttributeChecker> checks;
    final String newName;
    final DiscardAttributeChecker discardChecker;
    final AttributeConverter converter;
    final AttributeConverter addConverter;


    AttributeTransformationDescription(String name, List<RejectAttributeChecker> checks, String newName, DiscardAttributeChecker discardChecker, AttributeConverter converter, AttributeConverter addConverter) {
        this.name = name;
        this.checks = checks != null ? checks : Collections.<RejectAttributeChecker>emptyList();
        this.newName = newName;
        this.discardChecker = discardChecker;
        this.converter = converter;
        this.addConverter = addConverter;
    }

    boolean shouldDiscard(ModelNode attributeValue, TransformationRule.AbstractTransformationContext context) {
        if (discardChecker == null) {
            return false;
        }

        if (discardChecker.isDiscardUndefined() && !attributeValue.isDefined()) {
            return true;
        }

        if (!discardChecker.isAllowExpressions() && attributeValue.getType() == ModelType.EXPRESSION) {
            return false;
        }

        if (discardChecker.isValueDiscardable(name, attributeValue, context.getContext())) {
            return true;
        }
        return false;
    }

    String getNewName() {
        return newName;
    }

    /**
     * Checks that an attribute can be transformed
     *
     * @param name the attribute name
     * @param attributeValue the attribtue value
     * @param context the context
     * @return {@code true} if it can be transformed, {@code false} otherwise
     */
    boolean checkAttributeValueIsValid(ModelNode attributeValue, TransformationRule.AbstractTransformationContext context) {
        for (RejectAttributeChecker checker : checks) {
            if (checker.rejectAttribute(name, attributeValue, context.getContext())) {
                return false;
            }
        }
        return true;
    }

    void convertValue(ModelNode attributeValue, TransformationRule.AbstractTransformationContext context) {
        if (converter != null) {
            converter.convertAttribute(name, attributeValue, context.getContext());
        }
    }


    ModelNode addAttribute(TransformationRule.AbstractTransformationContext context) {
        if (addConverter != null) {
            ModelNode attributeValue = new ModelNode();
            addConverter.convertAttribute(name, attributeValue, context.getContext());
            return attributeValue;
        }
        return null;
    }

}
