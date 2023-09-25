/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller.transform;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.dmr.ModelNode;

/**
 * Simple {@link RejectAttributeChecker} that provides the operation or model context to a {@link Rejecter}.
 * @author Paul Ferraro
 */
public class SimpleRejectAttributeChecker implements RejectAttributeChecker {

    public interface Rejecter {
        boolean reject(PathAddress address, String name, ModelNode value, ModelNode model, TransformationContext context);

        String getRejectedMessage(Set<String> attributes);
    }

    private final String logMessageId = UUID.randomUUID().toString();
    private final Rejecter rejecter;

    public SimpleRejectAttributeChecker(Rejecter rejecter) {
        this.rejecter = rejecter;
    }

    @Override
    public boolean rejectOperationParameter(PathAddress address, String name, ModelNode value, ModelNode operation, TransformationContext context) {
        return this.rejecter.reject(address, name, value, operation, context);
    }

    @Override
    public boolean rejectResourceAttribute(PathAddress address, String name, ModelNode value, TransformationContext context) {
        return this.rejecter.reject(address, name, value, context.readResource(PathAddress.EMPTY_ADDRESS).getModel(), context);
    }

    @Override
    public String getRejectionLogMessageId() {
        return this.logMessageId;
    }

    @Override
    public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
        return this.rejecter.getRejectedMessage(attributes.keySet());
    }
}
