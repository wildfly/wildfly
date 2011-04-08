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

import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandArgumentCompleter;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class DeleteJmsTopicHandler extends CommandHandlerWithHelp {

    public DeleteJmsTopicHandler() {
        super("delete-jms-topic", true,
                new SimpleTabCompleterWithDelegate(new String[]{"--help"/*, "name="*/},
                        new CommandArgumentCompleter() {
                    @Override
                    public int complete(CommandContext ctx, String buffer,
                            int cursor, List<String> candidates) {

                        int nextCharIndex = 0;
                        while (nextCharIndex < buffer.length()) {
                            if (!Character.isWhitespace(buffer.charAt(nextCharIndex))) {
                                break;
                            }
                            ++nextCharIndex;
                        }

                        if(ctx.getModelControllerClient() != null) {
                            List<String> deployments = Util.getJmsResources(ctx.getModelControllerClient(), "topic");
                            if(deployments.isEmpty()) {
                                return -1;
                            }

                            String opBuffer = buffer.substring(nextCharIndex).trim();
                            if (opBuffer.isEmpty()) {
                                candidates.addAll(deployments);
                            } else {
                                for(String name : deployments) {
                                    if(name.startsWith(opBuffer)) {
                                        candidates.add(name);
                                    }
                                }
                                Collections.sort(candidates);
                            }
                            return nextCharIndex;
                        } else {
                            return -1;
                        }
                    }}));
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.handlers.CommandHandlerWithHelp#doHandle(org.jboss.as.cli.CommandContext)
     */
    @Override
    protected void doHandle(CommandContext ctx) {

        if(!ctx.hasArguments()) {
            ctx.printLine("Missing required argument 'name'.");
            return;
        }

        String name = ctx.getNamedArgument("name");
        if(name == null) {
            List<String> args = ctx.getArguments();
            if(!args.isEmpty()) {
                name = args.get(0);
            }
        }

        if(name == null) {
            ctx.printLine("Missing required argument 'name'.");
            return;
        }

        ModelControllerClient client = ctx.getModelControllerClient();

        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
        builder.addNode("subsystem", "jms");
        builder.addNode("topic", name);
        builder.setOperationName("remove");

        final ModelNode result;
        try {
            ModelNode request = builder.buildRequest();
            result = client.execute(request);
        } catch (Exception e) {
            ctx.printLine("Failed to perform operation: " + e.getLocalizedMessage());
            return;
        }

        if (!Util.isSuccess(result)) {
            ctx.printLine("Failed to delete topic '" + name + "': " + Util.getFailureDescription(result));
            return;
        }

        ctx.printLine("Removed topic " + name);
    }
}
