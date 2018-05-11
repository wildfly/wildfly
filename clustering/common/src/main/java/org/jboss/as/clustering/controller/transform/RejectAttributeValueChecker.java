/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller.transform;

import java.util.Map;

import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.dmr.ModelNode;

/**
 * @author Paul Ferraro
 */
public enum RejectAttributeValueChecker implements RejectAttributeChecker {
    NEGATIVE() {
        @Override
        boolean isRejected(ModelNode value) {
            return value.asLong() < 0;
        }

        @Override
        public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
            return ClusteringLogger.ROOT_LOGGER.attributesDoNotSupportNegativeValues(attributes.keySet());
        }
    },
    ZERO() {
        @Override
        boolean isRejected(ModelNode value) {
            return value.asLong() == 0L;
        }

        @Override
        public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
            return ClusteringLogger.ROOT_LOGGER.attributesDoNotSupportNegativeValues(attributes.keySet());
        }
    },
    ;

    @Override
    public boolean rejectOperationParameter(PathAddress address, String attributeName, ModelNode attributeValue, ModelNode operation, TransformationContext context) {
        return attributeValue.isDefined() && !RejectAttributeChecker.SIMPLE_EXPRESSIONS.rejectOperationParameter(address, attributeName, attributeValue, operation, context) && this.isRejected(attributeValue);
    }

    @Override
    public boolean rejectResourceAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
        return attributeValue.isDefined() && !RejectAttributeChecker.SIMPLE_EXPRESSIONS.rejectResourceAttribute(address, attributeName, attributeValue, context) && this.isRejected(attributeValue);
    }

    @Override
    public String getRejectionLogMessageId() {
        return this.getClass().getName();
    }

    abstract boolean isRejected(ModelNode value);
}
