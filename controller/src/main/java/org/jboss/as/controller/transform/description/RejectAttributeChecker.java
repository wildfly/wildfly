/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
 * Checks whether an attribute should be rejected or not
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface RejectAttributeChecker {

    /**
     * Gets whether the given attribute or parameter value is not understandable by the target process and needs
     * to be rejected.
     *
     * @param attributeName the name of the attribute
     * @param attributeValue the value of the attribute
     * @param context the context of the transformation
     *
     * @return {@code true} if the attribute or parameter value is not understandable by the target process and so needs to be rejected, {@code false} otherwise.
     */
    boolean rejectAttribute(String attributeName, ModelNode attributeValue, TransformationContext context);

    RejectAttributeChecker SIMPLE_EXPRESSIONS = new RejectAttributeChecker() {

        @Override

        public boolean rejectAttribute(String attributeName, ModelNode attributeValue, TransformationContext context) {
            return AttributeTransformationRequirementChecker.SIMPLE_EXPRESSIONS.isAttributeTransformationRequired(attributeName, attributeValue, context);
        }
    };


    RejectAttributeChecker SIMPLE_LIST_EXPRESSIONS = new RejectAttributeChecker() {

        @Override
        public boolean rejectAttribute(String attributeName, ModelNode attributeValue, TransformationContext context) {
            return AttributeTransformationRequirementChecker.SIMPLE_LIST_EXPRESSIONS.isAttributeTransformationRequired(attributeName, attributeValue, context);
        }
    };




}
