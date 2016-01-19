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
