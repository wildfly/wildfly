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
import java.io.FileInputStream;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.protocol.StreamUtils;
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
            ctx.printLine("Required argument is missing.");
            return;
        }

        final String filePath;
        final String name;
        final String runtimeName;

        int spaceInd = args.indexOf(' ');
        if(spaceInd < 0) {
            filePath = args;
        } else {
            filePath = args.substring(0, spaceInd);
        }

        File f = new File(filePath);
        if(!f.exists()) {
            ctx.printLine("The path doesn't exist: " + f.getAbsolutePath());
            return;
        }

        if(spaceInd < 0) {
            name = f.getName();
            runtimeName = null;
        } else {
            char ch = args.charAt(spaceInd++);
            while(spaceInd < args.length() && Character.isWhitespace(ch)) {
                ch = args.charAt(spaceInd++);
            }
            if(spaceInd == args.length()) {
                name = f.getName();
                runtimeName = null;
            } else {
                int nextSpace = args.indexOf(' ', spaceInd + 1);
                if(nextSpace < 0) {
                    name = args.substring(spaceInd - 1, args.length());
                    runtimeName = null;
                } else {
                    name = args.substring(spaceInd - 1, nextSpace);
                    runtimeName = args.substring(nextSpace).trim();
                }
            }
        }

        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();

        ModelNode result;

        // add
        builder = new DefaultOperationRequestBuilder();
        builder.setOperationName("add");
        builder.addNode("deployment", name);
        if(runtimeName != null) {
            builder.addProperty("runtime-name", runtimeName);
        }

        FileInputStream is = null;
        try {
            is = new FileInputStream(f);
            ModelNode request = builder.buildRequest();
            OperationBuilder op = OperationBuilder.Factory.create(request);
            op.addInputStream(is);
            request.get("input-stream-index").set(0);
            result = client.execute(op.build());
        } catch(Exception e) {
            ctx.printLine("Failed to add the deployment content to the repository: " + e.getLocalizedMessage());
            return;
        } finally {
            StreamUtils.safeClose(is);
        }
        if(!Util.isSuccess(result)) {
            ctx.printLine(Util.getFailureDescription(result));
            return;
        }

        // deploy
        builder = new DefaultOperationRequestBuilder();
        builder.setOperationName("deploy");
        builder.addNode("deployment", name);
        try {
            ModelNode request = builder.buildRequest();
            result = client.execute(request);
        } catch (Exception e) {
            ctx.printLine("Failed to deploy: " + e.getLocalizedMessage());
            return;
        }
        if (!Util.isSuccess(result)) {
            ctx.printLine(Util.getFailureDescription(result));
            return;
        }

        ctx.printLine("'" + name + "' deployed successfully.");
    }
}
