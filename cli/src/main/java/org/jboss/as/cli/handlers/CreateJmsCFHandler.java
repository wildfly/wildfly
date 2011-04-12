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
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class CreateJmsCFHandler extends BatchModeCommandHandler {

    public CreateJmsCFHandler() {
        super("create-jms-cf", true, new SimpleTabCompleter(new String[]{
                "--help", "name=", "auto-group=", "entries=", "connector=",
                "block-on-acknowledge=", "block-on-durable-send=", "block-on-non-durable-send=",
                "cache-large-message-client=", "call-timeout=",
                "client-failure-check-period=", "client-id=", "confirmation-window-size=",
                "connection-ttl=", "connector=", "consumer-max-rate=",
                "consumer-window-size=", "discovery-group-name=", "dups-ok-batch-size=",
                "failover-on-initial-connection=", "failover-on-server-shutdown=",
                "group-id=", "max-retry-interval=", "min-large-message-size=",
                "pre-acknowledge=", "producer-max-rate=", "producer-window-size=",
                "reconnect-attempts=", "retry-interval=", "retry-interval-multiplier=",
                "scheduled-thread-pool-max-size=", "thread-pool-max-size=",
                "transaction-batch-size=", "use-global-pools="}));
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.handlers.CommandHandlerWithHelp#doHandle(org.jboss.as.cli.CommandContext)
     */
    @Override
    protected void doHandle(CommandContext ctx) {

        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
        builder.addNode("subsystem", "jms");
        builder.setOperationName("add");

        String name = null;
        String entriesStr = null;
        for(String argName : ctx.getArgumentNames()) {
            if(argName.equals("name")) {
                name = ctx.getNamedArgument(argName);
            } else if(argName.equals("entries")) {
                entriesStr = ctx.getNamedArgument(argName);
            } else {
                builder.addProperty(argName, ctx.getNamedArgument(argName));
            }
        }

        if(name == null) {
            ctx.printLine("Required argument 'name' is missing.");
            return;
        }

        builder.addNode("connection-factory", name);
        ModelNode entriesNode = builder.getModelNode().get("entries");
        if(entriesStr == null) {
            entriesNode.add(name);
        } else {
            String[] split = entriesStr.split(",");
            for(int i = 0; i < split.length; ++i) {
                String entry = split[i].trim();
                if(!entry.isEmpty()) {
                    entriesNode.add(entry);
                }
            }
        }

        ModelControllerClient client = ctx.getModelControllerClient();
        final ModelNode result;
        try {
            ModelNode request = builder.buildRequest();
            result = client.execute(request);
        } catch (Exception e) {
            ctx.printLine("Failed to perform operation: " + e.getLocalizedMessage());
            return;
        }

        if (!Util.isSuccess(result)) {
            ctx.printLine(Util.getFailureDescription(result));
            return;
        }

        ctx.printLine("Created connection factory " + name);
    }
}
