/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

import java.util.Locale;
import java.util.Set;

/**
 * Removes the messaging subsystem.
 *
 * @author Emanuel Muckenhuber
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class MessagingSubsystemRemove implements OperationStepHandler {

    static final MessagingSubsystemRemove INSTANCE = new MessagingSubsystemRemove();

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

        final Resource resource = context.removeResource(PathAddress.EMPTY_ADDRESS);
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

                // TODO should we make the runtime change by default, or require a header indicating that's valid?

                Set<String> serverNames = resource.getChildrenNames(CommonAttributes.HORNETQ_SERVER);
                for (String serverName : serverNames) {
                    PathElement path = PathElement.pathElement(CommonAttributes.HORNETQ_SERVER, serverName);
                    Resource server = resource.getChild(path);
                    HornetQServerRemove.removeHornetQServer(serverName, context, server);
                }


                if(context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                    //  TODO recover
                }
            }
        }, OperationContext.Stage.RUNTIME);

        context.completeStep();
    }
}
