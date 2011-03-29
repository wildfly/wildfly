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

import java.io.File;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class DeployHandler extends CommandHandlerWithHelp {

    public DeployHandler() {
        super("deploy");
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

        File f = new File(args);
        if(!f.exists()) {
            ctx.printLine("The path doesn't exist: " + f.getAbsolutePath());
            return;
        }

        final String name = f.getName();
        if(name.isEmpty()) {
            ctx.printLine("The path is empty.");
        }
        final String runtimeName = name;

        final String url;
        try {
            url = f.toURI().toURL().toExternalForm();
        } catch(Exception e) {
            ctx.printLine("Failed to create a URL from '" + args + "': " + e.getLocalizedMessage());
            return;
        }

        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();

        // upload
        builder.setOperationName("upload-deployment-url");
        builder.addProperty("name", name);
        builder.addProperty("runtime-name", runtimeName);
        builder.addProperty("url", url);
        ModelNode result;
        try {
            ModelNode request = builder.buildRequest();
            result = client.execute(request);
        } catch(Exception e) {
            ctx.printLine("Failed to upload content: " + e.getLocalizedMessage());
            return;
        }

        if(!Util.isSuccess(result)) {
            ctx.printLine(Util.getFailureDescription(result));
            return;
        }

        byte[] hash = Util.getHash(result);
        if (hash == null) {
            ctx.printLine("Failed to obtain the hash of the deployment.");
            return;
        }

        // add
        builder = new DefaultOperationRequestBuilder();
        builder.setOperationName("add");
        builder.addNode("deployment", name);
        builder.getModelNode().get("hash").set(hash);
        try {
            ModelNode request = builder.buildRequest();
            result = client.execute(request);
        } catch(Exception e) {
            ctx.printLine("Failed to add the deployment content to the repository: " + e.getLocalizedMessage());
            return;
        }
        if(!Util.isSuccess(result)) {
            ctx.printLine(Util.getFailureDescription(result));
            return;
        }

        //deploy
        builder = new DefaultOperationRequestBuilder();
        builder.setOperationName("deploy");
        builder.addNode("deployment", name);

        try {
            ModelNode request = builder.buildRequest();
            result = client.execute(request);
         } catch(Exception e) {
             ctx.printLine("Failed to deploy: " + e.getLocalizedMessage());
             return;
         }

         if(!Util.isSuccess(result)) {
             ctx.printLine(Util.getFailureDescription(result));
             return;
         }

         ctx.printLine("'" + name + "' deployed successfully.");
    }
}
