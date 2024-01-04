/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * Read handler for XML deployed Jakarta Messaging queues
 *
 * @author Stuart Douglas
 */
public class JMSQueueConfigurationRuntimeHandler extends AbstractJMSRuntimeHandler<ModelNode> {

    public static final JMSQueueConfigurationRuntimeHandler INSTANCE = new JMSQueueConfigurationRuntimeHandler();


    private JMSQueueConfigurationRuntimeHandler() {

    }

    @Override
    protected void executeReadAttribute(final String attributeName, final OperationContext context, final ModelNode destination, final PathAddress address, final boolean includeDefault) {

        if (destination.hasDefined(attributeName)) {
            context.getResult().set(destination.get(attributeName));
        } else if(includeDefault) {
            for (AttributeDefinition attr : JMSQueueDefinition.DEPLOYMENT_ATTRIBUTES) {
                if(attr.getName().equals(attributeName)) {
                    ModelNode resultNode = context.getResult();
                    ModelNode defaultValue = attr.getDefaultValue();
                    if (defaultValue != null) {
                        resultNode.set(defaultValue);
                    }
                }
            }
        }
    }
}
