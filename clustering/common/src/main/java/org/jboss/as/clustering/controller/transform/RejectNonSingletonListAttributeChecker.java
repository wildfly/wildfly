/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.transform;

import java.util.Map;
import java.util.Set;

import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.dmr.ModelNode;

/**
 * Rejects a list attribute if it contains more than one element.
 * @author Paul Ferraro
 */
public enum RejectNonSingletonListAttributeChecker implements RejectAttributeChecker {
    INSTANCE;

    private final RejectAttributeChecker checker = new org.jboss.as.clustering.controller.transform.SimpleRejectAttributeChecker(new org.jboss.as.clustering.controller.transform.SimpleRejectAttributeChecker.Rejecter() {

        @Override
        public boolean reject(PathAddress address, String name, ModelNode value, ModelNode model, TransformationContext context) {
            return value.isDefined() && value.asList().size() > 1;
        }

        @Override
        public String getRejectedMessage(Set<String> attributes) {
            return ClusteringLogger.ROOT_LOGGER.rejectedMultipleValues(attributes);
        }
    });

    @Override
    public boolean rejectOperationParameter(PathAddress address, String attributeName, ModelNode attributeValue, ModelNode operation, TransformationContext context) {
        return this.checker.rejectOperationParameter(address, attributeName, attributeValue, operation, context);
    }

    @Override
    public boolean rejectResourceAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
        return this.checker.rejectResourceAttribute(address, attributeName, attributeValue, context);
    }

    @Override
    public String getRejectionLogMessageId() {
        return this.checker.getRejectionLogMessageId();
    }

    @Override
    public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
        return this.checker.getRejectionLogMessage(attributes);
    }
}
