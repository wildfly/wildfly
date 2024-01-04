/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

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
                throw MessagingLogger.ROOT_LOGGER.missingArguments();
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
            throw MessagingLogger.ROOT_LOGGER.missingName();
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
            throw MessagingLogger.ROOT_LOGGER.jndiWasNotFound(jndiName);
        }

        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
        builder.addNode("subsystem", "messaging");
        builder.addNode("server", serverName);
        builder.addNode(resource, jndiName);
        builder.setOperationName("remove");
        return builder.buildRequest();
    }
}
