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
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

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
