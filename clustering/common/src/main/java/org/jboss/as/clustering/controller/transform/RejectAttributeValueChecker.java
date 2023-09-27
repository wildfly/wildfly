/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
