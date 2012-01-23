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

import java.util.Collection;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.DefaultCompleter;
import org.jboss.as.cli.impl.DefaultCompleter.CandidatesProvider;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.cli.util.SimpleTable;
import org.jboss.as.cli.util.StrictSizeTable;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 *
 * @author Alexey Loubyansky
 */
public class DeploymentInfoHandler extends BaseOperationCommand {

    private final ArgumentWithValue name;

    private List<String> serverGroups;

    public DeploymentInfoHandler(CommandContext ctx) {
        super(ctx, "deployment-info", true);
        name = new ArgumentWithValue(this, new DefaultCompleter(new CandidatesProvider(){
            @Override
            public Collection<String> getAllCandidates(CommandContext ctx) {
                return Util.getDeployments(ctx.getModelControllerClient());
            }}), "--name");
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.OperationCommand#buildRequest(org.jboss.as.cli.CommandContext)
     */
    @Override
    public ModelNode buildRequest(CommandContext ctx) throws CommandFormatException {
        final ParsedCommandLine parsedCmd = ctx.getParsedCommandLine();
        if(!name.isPresent(parsedCmd)) {
            throw new CommandFormatException("Required argument " + name.getFullName() + " is missing.");
        }
        final String deploymentName = name.getValue(parsedCmd);

        final ModelNode request = new ModelNode();
        if(ctx.isDomainMode()) {
            request.get(Util.OPERATION).set(Util.COMPOSITE);
            request.get(Util.ADDRESS).setEmptyList();
            final ModelNode steps = request.get(Util.STEPS);

            ModelNode readResource = new ModelNode();
            ModelNode address = readResource.get(Util.ADDRESS);
            address.add(Util.DEPLOYMENT, deploymentName);
            readResource.get(Util.OPERATION).set(Util.READ_RESOURCE);
            steps.add(readResource);

            serverGroups = Util.getServerGroups(ctx.getModelControllerClient());
            for(String serverGroup : serverGroups) {
                // this was supposed to be a read-resource on deployment
                // but if the deployment isn't added to a server-group
                // it will fail the whole composite op and will not return the desired info in the response
                readResource = new ModelNode();
                address = readResource.get(Util.ADDRESS);
                address.add(Util.SERVER_GROUP, serverGroup);
                readResource.get(Util.OPERATION).set(Util.READ_CHILDREN_NAMES);
                readResource.get(Util.CHILD_TYPE).set(Util.DEPLOYMENT);
                steps.add(readResource);
            }

            request.get(Util.OPERATION_HEADERS, Util.ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        } else {
            final ModelNode address = request.get(Util.ADDRESS);
            address.add(Util.DEPLOYMENT, deploymentName);
            request.get(Util.OPERATION).set(Util.READ_RESOURCE);
            request.get(Util.INCLUDE_RUNTIME).set(true);
        }
        return request;
    }

    @Override
    protected void handleResponse(CommandContext ctx, ModelNode response, boolean composite) {
        try {
            if(!response.hasDefined(Util.RESULT)) {
                ctx.error("The operation response came back w/o result: " + response);
                return;
            }
            final ModelNode result = response.get(Util.RESULT);

            if(ctx.isDomainMode()) {
                final List<Property> steps = result.asPropertyList();
                if(steps.isEmpty()) {
                    ctx.error("Response for the main resource info of the deployment is missing.");
                    return;
                }

                // /deployment=<name>
                ModelNode step = steps.get(0).getValue();
                if(!step.has(Util.RESULT)) {
                    ctx.error("Failed to read the main resource info of the deployment.");
                    return;
                }
                ModelNode stepResponse = step.get(Util.RESULT);
                final StrictSizeTable table = new StrictSizeTable(1);
                table.addCell(Util.NAME, stepResponse.get(Util.NAME).asString());
                table.addCell(Util.RUNTIME_NAME, stepResponse.get(Util.RUNTIME_NAME).asString());
                ctx.printLine(table.toString());

                if(serverGroups == null) {
                    ctx.error("Server group list is lost.");
                    return;
                }
                if(serverGroups.size() != steps.size() - 1) {
                    ctx.error("Expected results for " + serverGroups.size() + " server groups but received " + (steps.size() - 1));
                    return;
                }

                final String deploymentName = name.getValue(ctx.getParsedCommandLine());
                final SimpleTable groups = new SimpleTable(new String[]{"SERVER GROUP", "ENABLED"});
                for(int i = 1; i < steps.size(); ++i) {
                    stepResponse = steps.get(i).getValue();
/*
                    String enabled = "no";
                    if(stepResponse.hasDefined(Util.RESULT)) {
                        final ModelNode stepResult = stepResponse.get(Util.RESULT);
                        if(stepResult.has(Util.ENABLED)) {
                            if(stepResult.get(Util.ENABLED).asBoolean()) {
                                enabled = "yes";
                            }
                        }
                    }
                    groups.addLine(new String[]{serverGroups.get(i - 1), enabled});
*/
                    // not nice
                    // this is just a check whether the deployment is present
                    // but it's not checking whether the deployment is enabled
                    // if the cli was used to deploy/undeploy then the presence means it's enabled
                    // but there could be other tools too...
                    if(Util.listContains(stepResponse, deploymentName)) {
                        groups.addLine(new String[]{serverGroups.get(i - 1), "yes"});
                    } else {
                        groups.addLine(new String[]{serverGroups.get(i - 1), "no"});
                    }
                }
                ctx.printLine(groups.toString(true));
        } else {
            final StrictSizeTable table = new StrictSizeTable(1);
            table.addCell(Util.NAME, result.get(Util.NAME).asString());
            table.addCell(Util.RUNTIME_NAME, result.get(Util.RUNTIME_NAME).asString());
            table.addCell(Util.ENABLED, result.get(Util.ENABLED).asString());
            table.addCell(Util.STATUS, result.get(Util.STATUS).asString());
            ctx.printLine(table.toString());
        }
        } finally {
            serverGroups = null;
        }
    }
}
