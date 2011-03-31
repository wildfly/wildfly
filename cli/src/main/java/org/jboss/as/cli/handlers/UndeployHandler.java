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
public class UndeployHandler extends CommandHandlerWithHelp {

    public UndeployHandler() {
        super("undeploy");
    }

    @Override
    protected void handle(CommandContext ctx, String args) {

        ModelControllerClient client = ctx.getModelControllerClient();
        if(client == null) {
            ctx.printLine("The controller client is not available. Make sure you are connected to the controller.");
            return;
        }

        if(args == null) {
            ctx.printLine("The argument is missing.");
            return;
        }

        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();

        // undeploy
        builder = new DefaultOperationRequestBuilder();
        builder.setOperationName("undeploy");
        builder.addNode("deployment", args);

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
        builder.addNode("deployment", args);
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

        ctx.printLine("'" + args + "' undeployed successfully.");
    }
}
