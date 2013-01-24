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

import org.jboss.as.controller.transform.AttributeTransformationRequirementChecker;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
class RestrictedAttributeTransformationDescription implements AttributeTransformationDescription {

    private final AttributeTransformationRequirementChecker[] checks;
    RestrictedAttributeTransformationDescription(AttributeTransformationRequirementChecker... checks) {
        this.checks = checks;
    }

    @Override
    public TransformationType processAttribute(String name, ModelNode attributeValue, final TransformationRule.AbstractTransformationContext context) {
        final TransformationContext ctx = context.getContext();
        if(check(name, attributeValue, ctx)) {
            return TransformationType.REJECT;
        } else {
            return TransformationType.TRANSFORMED;
        }
    }

    boolean check(final String attributeName, final ModelNode attributeValue, TransformationContext context) {
        for(final AttributeTransformationRequirementChecker checker : checks) {
            if(checker.isAttributeTransformationRequired(attributeName, attributeValue, context)) {
                return true;
            }
        }
        return false;
    }

}
