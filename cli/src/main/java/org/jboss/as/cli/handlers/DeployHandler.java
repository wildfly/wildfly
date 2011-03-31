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
            ctx.printColumns(Util.getDeployments(client));
            return;
        }

        boolean force = false;
        String filePath = null;
        String name = null;
        String runtimeName = null;

        String[] arr = args.split("\\s+");
        for(int i = 0; i < arr.length; ++i) {
            String arg = arr[i];
            if ("-f".equals(arg)) {
                force = true;
            } else if (filePath == null) {
                filePath = arg;
            } else if (name == null) {
                name = arg;
            } else {
                runtimeName = arg;
            }
        }

        if(filePath == null) {
            ctx.printLine("File path is missing.");
            return;
        }

        File f = new File(filePath);
        if(!f.exists()) {
            ctx.printLine("The path doesn't exist: " + f.getAbsolutePath());
            return;
        }

        if(name == null) {
            name = f.getName();
        }

        if(Util.isDeployed(name, ctx.getModelControllerClient())) {
            if(force) {
                DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();

                ModelNode result;

                // replace
                builder = new DefaultOperationRequestBuilder();
                builder.setOperationName("full-replace-deployment");
                builder.addProperty("name", name);
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
                    ctx.printLine("Failed to replace the deployment: " + e.getLocalizedMessage());
                    return;
                } finally {
                    StreamUtils.safeClose(is);
                }
                if(!Util.isSuccess(result)) {
                    ctx.printLine(Util.getFailureDescription(result));
                    return;
                }

                ctx.printLine("'" + name + "' re-deployed successfully.");
            } else {
                ctx.printLine("'" + name + "' is already deployed (use -f to force re-deploy).");
            }

            return;
        } else {

            DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();

            ModelNode result;

            // add
            builder = new DefaultOperationRequestBuilder();
            builder.setOperationName("add");
            builder.addNode("deployment", name);
            if (runtimeName != null) {
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
            } catch (Exception e) {
                ctx.printLine("Failed to add the deployment content to the repository: "
                        + e.getLocalizedMessage());
                return;
            } finally {
                StreamUtils.safeClose(is);
            }
            if (!Util.isSuccess(result)) {
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
}
