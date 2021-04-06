/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.messaging.activemq.jms.cli;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.handlers.BatchModeCommandHandler;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
class DeleteJMSResourceHandler extends BatchModeCommandHandler {

    public DeleteJMSResourceHandler(CommandContext ctx) {
        super(ctx, "delete-jms-resource", true);
        this.addRequiredPath("/subsystem=messaging-activemq");
    }

    @Override
    public ModelNode buildRequestWithoutHeaders(CommandContext ctx)
            throws OperationFormatException {

        try {
            if(!ctx.getParsedCommandLine().hasProperties()) {
                throw new OperationFormatException("Arguments are missing");
            }
        } catch (CommandFormatException e) {
            throw new OperationFormatException(e.getLocalizedMessage());
        }

        //String target = null;
        String jndiName = null;
        String serverName = "default"; // TODO read server name from props

        String[] args = ctx.getArgumentsString().split("\\s+");
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
            resource = "jms-topic";
        } else if(Util.isQueue(client, jndiName)) {
            resource = "jms-queue";
        } else if(Util.isConnectionFactory(client, jndiName)) {
            resource = "connection-factory";
        } else {
            throw new OperationFormatException("'" + jndiName +"' wasn't found among existing JMS resources.");
        }

        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
        builder.addNode("subsystem", "messaging");
        builder.addNode("server", serverName);
        builder.addNode(resource, jndiName);
        builder.setOperationName("remove");
        return builder.buildRequest();
    }
}
