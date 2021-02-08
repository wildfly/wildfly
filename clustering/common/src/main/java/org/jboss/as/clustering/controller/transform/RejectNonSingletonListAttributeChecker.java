/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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
