/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.transform;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.dmr.ModelNode;

/**
 * Discards an attribute if its list value contains a single entry.
 * @author Paul Ferraro
 */
public enum DiscardSingletonListAttributeChecker implements DiscardAttributeChecker {
    INSTANCE;

    private final DiscardAttributeChecker checker = new DiscardAttributeChecker.DefaultDiscardAttributeChecker() {
        @Override
        protected boolean isValueDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
            return !attributeValue.isDefined() || attributeValue.asList().size() <= 1;
        }
    };

    @Override
    public boolean isDiscardExpressions() {
        return this.checker.isDiscardExpressions();
    }

    @Override
    public boolean isDiscardUndefined() {
        return this.checker.isDiscardUndefined();
    }

    @Override
    public boolean isOperationParameterDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, ModelNode operation, TransformationContext context) {
        return this.checker.isOperationParameterDiscardable(address, attributeName, attributeValue, operation, context);
    }

    @Override
    public boolean isResourceAttributeDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
        return this.checker.isResourceAttributeDiscardable(address, attributeName, attributeValue, context);
    }
}
