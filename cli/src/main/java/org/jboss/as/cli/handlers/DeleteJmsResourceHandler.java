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
package org.jboss.as.cli.handlers;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class DeleteJmsResourceHandler extends BatchModeCommandHandler {

    public DeleteJmsResourceHandler() {
        super("delete-jms-resource", true);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.handlers.CommandHandlerWithHelp#doHandle(org.jboss.as.cli.CommandContext)
     */
    @Override
    protected void doHandle(CommandContext ctx) {

        ModelNode request;
        try {
            request = buildRequest(ctx);
        } catch (OperationFormatException e1) {
            ctx.printLine(e1.getLocalizedMessage());
            return;
        }

        ModelControllerClient client = ctx.getModelControllerClient();

        final ModelNode result;
        try {
            result = client.execute(request);
        } catch (Exception e) {
            ctx.printLine("Failed to perform operation: " + e.getLocalizedMessage());
            return;
        }

        if (!Util.isSuccess(result)) {
            ctx.printLine(Util.getFailureDescription(result));
            return;
        }
    }

    @Override
    public ModelNode buildRequest(CommandContext ctx)
            throws OperationFormatException {

        if(!ctx.hasArguments()) {
            throw new OperationFormatException("Arguments are missing");
        }

        //String target = null;
        String jndiName = null;

        String[] args = ctx.getCommandArguments().split("\\s+");
        int i = 0;
        while(i < args.length) {
            String arg = args[i++];
            if(arg.equals("--target")) {
//                if(i < args.length) {
//                    target = args[i++];
//                }
            } else {
                jndiName = arg;
            }
        }

        if(jndiName == null) {
            throw new OperationFormatException("name is missing.");
        }

        ModelControllerClient client = ctx.getModelControllerClient();
        final String resource;
        if(Util.isTopic(client, jndiName)) {
            resource = "topic";
        } else if(Util.isQueue(client, jndiName)) {
            resource = "queue";
        } else if(Util.isConnectionFactory(client, jndiName)) {
            resource = "connection-factory";
        } else {
            throw new OperationFormatException("'" + jndiName +"' wasn't found among existing JMS resources.");
        }

        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
        builder.addNode("subsystem", "jms");
        builder.addNode(resource, jndiName);
        builder.setOperationName("remove");
        return builder.buildRequest();
    }
}
