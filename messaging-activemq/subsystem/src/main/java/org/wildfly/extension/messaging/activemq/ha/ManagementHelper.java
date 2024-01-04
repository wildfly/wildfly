/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.ha;

import static org.jboss.as.controller.OperationContext.Stage.MODEL;

import java.util.Collection;
import java.util.Set;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.messaging.activemq.ActiveMQReloadRequiredHandlers;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class ManagementHelper {

    /**
     * Create an ADD operation that can check that there is no other sibling when the resource is added.
     *
     * @param childType the type of children to check for the existence of siblings
     * @param allowSibling whether it is allowed to have sibling for the resource that is added.
     * @param attributes the attributes of the ADD operation
     */
    static AbstractAddStepHandler createAddOperation(final String childType, final boolean allowSibling, Collection<? extends AttributeDefinition> attributes) {
        return new ActiveMQReloadRequiredHandlers.AddStepHandler(attributes) {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                super.execute(context, operation);
                if (!allowSibling) {
                    context.addStep(checkNoOtherSibling(childType), MODEL);
                }
            }
        };
    }

    static OperationStepHandler checkNoOtherSibling(final String childType) {
        return new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                PathAddress parentAddress = context.getCurrentAddress().getParent();
                Resource parent = context.readResourceFromRoot(parentAddress, false);
                Set<String> children = parent.getChildrenNames(childType);
                if (children.size() > 1) {
                    throw MessagingLogger.ROOT_LOGGER.onlyOneChildIsAllowed(childType, children);
                }
            }
        };
    }
}
