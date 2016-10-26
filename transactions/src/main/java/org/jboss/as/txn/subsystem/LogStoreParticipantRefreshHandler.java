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

package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public class LogStoreParticipantRefreshHandler implements OperationStepHandler {

    static final LogStoreParticipantRefreshHandler INSTANCE = new LogStoreParticipantRefreshHandler();

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        MBeanServer mbs = TransactionExtension.getMBeanServer(context);
        final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);

        try {
            final ObjectName on = LogStoreResource.getObjectName(resource);
            final ModelNode model = resource.getModel().clone();

            AttributeList attributes = mbs.getAttributes(on, LogStoreConstants.PARTICIPANT_JMX_NAMES);

            for (javax.management.Attribute attribute : attributes.asList()) {
                String modelName = LogStoreConstants.jmxNameToModelName(LogStoreConstants.MODEL_TO_JMX_PARTICIPANT_NAMES, attribute.getName());

                if (modelName != null) {
                    ModelNode aNode = model.get(modelName);
                    Object value = attribute.getValue();

                    if (aNode != null)
                        aNode.set(value == null ? "" : value.toString());
                }
            }
            // Replace the model
            resource.writeModel(model);
        } catch (Exception e) {
            throw new OperationFailedException("JMX error: " + e.getMessage());
        }

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }
}
