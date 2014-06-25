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

package org.wildfly.extension.mod_cluster;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.DefaultCheckersAndConverter;
import org.jboss.dmr.ModelNode;

import java.util.Map;

import static org.wildfly.extension.mod_cluster.LoadMetricDefinition.CAPACITY;

/**
 * Converts doubles to ints, rejects expressions or double values that are not equivalent to integers.
 * <p/>
 * Package protected solely to allow unit testing.
 */
class CapacityCheckerAndConverter extends DefaultCheckersAndConverter {

    static final CapacityCheckerAndConverter INSTANCE = new CapacityCheckerAndConverter();

    @Override
    public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
        return ModClusterLogger.ROOT_LOGGER.capacityIsExpressionOrGreaterThanIntegerMaxValue(attributes.get(CAPACITY.getName()));
    }

    @Override
    protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
        if (checkForExpression(attributeValue) || (attributeValue.isDefined() && !isIntegerValue(attributeValue.asDouble()))) {
            return true;
        }
        Long converted = convert(attributeValue);
        return (converted != null && (converted > Integer.MAX_VALUE || converted < Integer.MIN_VALUE));
    }

    @Override
    protected void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
        Long converted = convert(attributeValue);
        if (converted != null && converted <= Integer.MAX_VALUE && converted >= Integer.MIN_VALUE) {
            attributeValue.set((int) converted.longValue());
        }
    }

    @Override
    protected boolean isValueDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
        // Not used for discard
        return false;
    }

    private Long convert(ModelNode attributeValue) {
        if (attributeValue.isDefined() && !checkForExpression(attributeValue)) {
            double raw = attributeValue.asDouble();
            if (isIntegerValue(raw)) {
                return Math.round(raw);
            }
        }
        return null;
    }

    private boolean isIntegerValue(double raw) {
        return raw == (double) Math.round(raw);
    }

}
