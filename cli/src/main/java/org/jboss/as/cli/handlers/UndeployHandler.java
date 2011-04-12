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
public class UndeployHandler extends BatchModeCommandHandler {

    public UndeployHandler() {
        super("undeploy", true, new SimpleTabCompleterWithDelegate(new String[]{"--help", "-l"},
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
                            List<String> deployments = Util.getDeployments(ctx.getModelControllerClient());
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

    @Override
    protected void doHandle(CommandContext ctx) {

        ModelControllerClient client = ctx.getModelControllerClient();
        if(!ctx.hasArguments()) {
            printList(ctx, Util.getDeployments(client));
            return;
        }

        String deployment = null;
        List<String> args = ctx.getArguments();
        if(args.size() > 0) {
            deployment = args.get(0);
        }

        if (deployment == null) {
            printList(ctx, Util.getDeployments(client));
            return;
        }

        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();

        // undeploy
        builder = new DefaultOperationRequestBuilder();
        builder.setOperationName("undeploy");
        builder.addNode("deployment", deployment);

        ModelNode result;
        try {
            ModelNode request = builder.buildRequest();
            result = client.execute(request);
         } catch(Exception e) {
             ctx.printLine("Failed to undeploy: " + e.getLocalizedMessage());
             return;
         }

         // TODO undeploy may fail if the content failed to deploy but remove should still be executed
         if(!Util.isSuccess(result)) {
             ctx.printLine("Undeploy failed: " + Util.getFailureDescription(result));
             return;
         }

        // remove
        builder = new DefaultOperationRequestBuilder();
        builder.setOperationName("remove");
        builder.addNode("deployment", deployment);
        try {
            ModelNode request = builder.buildRequest();
            result = client.execute(request);
        } catch(Exception e) {
            ctx.printLine("Failed to remove the deployment content from the repository: " + e.getLocalizedMessage());
            return;
        }
        if(!Util.isSuccess(result)) {
            ctx.printLine("Remove failed: " + Util.getFailureDescription(result));
            return;
        }

        ctx.printLine("'" + deployment + "' undeployed successfully.");
    }
}
