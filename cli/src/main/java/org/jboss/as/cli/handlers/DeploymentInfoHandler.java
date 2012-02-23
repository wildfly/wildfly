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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
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
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 *
 * @author Alexey Loubyansky
 */
public class DeploymentInfoHandler extends BaseOperationCommand {

    private final ArgumentWithValue name;

    private List<String> addedServerGroups;
    private List<String> otherServerGroups;

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
    public ModelNode buildRequestWithoutHeaders(CommandContext ctx) throws CommandFormatException {
        final ParsedCommandLine parsedCmd = ctx.getParsedCommandLine();
        if(!name.isPresent(parsedCmd)) {
            throw new CommandFormatException("Required argument " + name.getFullName() + " is missing.");
        }
        final String deploymentName = name.getValue(parsedCmd);

        final ModelNode request = new ModelNode();
        if(ctx.isDomainMode()) {
            final List<String> serverGroups = Util.getServerGroups(ctx.getModelControllerClient());
            addedServerGroups = null;
            otherServerGroups = null;
            {
                final ModelNode validateRequest = new ModelNode();
                validateRequest.get(Util.OPERATION).set(Util.COMPOSITE);
                validateRequest.get(Util.ADDRESS).setEmptyList();
                final ModelNode steps = validateRequest.get(Util.STEPS);
                for(String serverGroup : serverGroups) {
                    final ModelNode step = new ModelNode();
                    step.get(Util.ADDRESS).setEmptyList();
                    step.get(Util.OPERATION).set(Util.VALIDATE_ADDRESS);
                    final ModelNode value = step.get(Util.VALUE);
                    value.add(Util.SERVER_GROUP, serverGroup);
                    value.add(Util.DEPLOYMENT, deploymentName);
                    steps.add(step);
                }
                final ModelControllerClient client = ctx.getModelControllerClient();
                final ModelNode response;
                try {
                    response = client.execute(validateRequest);
                } catch (IOException e) {
                    throw new CommandFormatException("Failed to query server groups for deployment " + deploymentName, e);
                }

                if(!response.hasDefined(Util.RESULT)) {
                    throw new CommandFormatException("The validation response came back w/o result: " + response);
                }
                ModelNode result = response.get(Util.RESULT);
//                if(!result.hasDefined(Util.DOMAIN_RESULTS)) {
//                    throw new CommandFormatException(Util.DOMAIN_RESULTS + " aren't available for validation request: " + result);
//                }
//                result = result.get(Util.DOMAIN_RESULTS);

                // TODO could be this... could be that
                if(result.hasDefined(Util.DOMAIN_RESULTS)) {
                    result = result.get(Util.DOMAIN_RESULTS);
                }
                final List<Property> stepResponses = result.asPropertyList();
                for(int i = 0; i < serverGroups.size() ; ++i) {
                    final Property prop = stepResponses.get(i);
                    ModelNode stepResponse = prop.getValue();
                    if(stepResponse.has(prop.getName())) { // TODO remove when the structure is consistent
                        stepResponse = stepResponse.get(prop.getName());
                    }
                    if(stepResponse.hasDefined(Util.RESULT)) {
                        final ModelNode stepResult = stepResponse.get(Util.RESULT);
                        if(stepResult.hasDefined(Util.VALID) && stepResult.get(Util.VALID).asBoolean()) {
                            if(addedServerGroups == null) {
                                addedServerGroups = new ArrayList<String>();
                            }
                           addedServerGroups.add(serverGroups.get(i));
                        } else {
                            if(otherServerGroups == null) {
                                otherServerGroups = new ArrayList<String>();
                            }
                            otherServerGroups.add(serverGroups.get(i));
                        }
                    } else {
                        if(otherServerGroups == null) {
                            otherServerGroups = new ArrayList<String>();
                        }
                        otherServerGroups.add(serverGroups.get(i));
                    }
                }

            }

            request.get(Util.OPERATION).set(Util.COMPOSITE);
            request.get(Util.ADDRESS).setEmptyList();
            final ModelNode steps = request.get(Util.STEPS);

            ModelNode step = new ModelNode();
            ModelNode address = step.get(Util.ADDRESS);
            address.add(Util.DEPLOYMENT, deploymentName);
            step.get(Util.OPERATION).set(Util.READ_RESOURCE);
            steps.add(step);

            if(addedServerGroups != null) {
                for(String serverGroup : addedServerGroups) {
                    step = new ModelNode();
                    address = step.get(Util.ADDRESS);
                    address.add(Util.SERVER_GROUP, serverGroup);
                    address.add(Util.DEPLOYMENT, deploymentName);
                    step.get(Util.OPERATION).set(Util.READ_RESOURCE);
                    steps.add(step);
                }
            }
        } else {
            final ModelNode address = request.get(Util.ADDRESS);
            address.add(Util.DEPLOYMENT, deploymentName);
            request.get(Util.OPERATION).set(Util.READ_RESOURCE);
            request.get(Util.INCLUDE_RUNTIME).set(true);
        }
        return request;
    }

    @Override
    protected void handleResponse(CommandContext ctx, ModelNode response, boolean composite) throws CommandFormatException {
        try {
            if(!response.hasDefined(Util.RESULT)) {
                throw new CommandFormatException("The operation response came back w/o result: " + response);
            }
            ModelNode result = response.get(Util.RESULT);

            if(ctx.isDomainMode()) {
//                if(!result.hasDefined(Util.DOMAIN_RESULTS)) {
//                    throw new CommandFormatException(Util.DOMAIN_RESULTS + " aren't available " + result);
//                    return;
//                }
                // TODO it could be... could be not...
                if(result.hasDefined(Util.DOMAIN_RESULTS)) {
                    result = result.get(Util.DOMAIN_RESULTS);
                }

                final Iterator<Property> steps = result.asPropertyList().iterator();
                if(!steps.hasNext()) {
                    throw new CommandFormatException("Response for the main resource info of the deployment is missing: " + result);
                }

                // /deployment=<name>
                ModelNode step = steps.next().getValue();
                if(step.has(Util.STEP_1)) { // TODO remove when the structure is consistent
                    step = step.get(Util.STEP_1);
                }
                if(!step.has(Util.RESULT)) {
                    throw new CommandFormatException("Failed to read the main resource info of the deployment: " + Util.getFailureDescription(step));
                }
                ModelNode stepResponse = step.get(Util.RESULT);
                final StrictSizeTable table = new StrictSizeTable(1);
                table.addCell(Util.NAME, stepResponse.get(Util.NAME).asString());
                table.addCell(Util.RUNTIME_NAME, stepResponse.get(Util.RUNTIME_NAME).asString());
                ctx.printLine(table.toString());

                final SimpleTable groups = new SimpleTable(new String[]{"SERVER GROUP", "STATE"});
                if(addedServerGroups == null) {
                    if(steps.hasNext()) {
                        throw new CommandFormatException("Didn't expect results for server groups but received " + (result.asPropertyList().size() - 1) + " more steps.");
                    }
                } else {
                    for(String sg : addedServerGroups) {
                        final Property prop = steps.next();
                        stepResponse = prop.getValue();
                        if(stepResponse.has(prop.getName())) { // TODO remove when the structure is consistent
                            stepResponse = stepResponse.get(prop.getName());
                        }

                        if(stepResponse.hasDefined(Util.RESULT)) {
                            final ModelNode stepResult = stepResponse.get(Util.RESULT);
                            if(stepResult.hasDefined(Util.ENABLED)) {
                                groups.addLine(new String[]{sg, stepResult.get(Util.ENABLED).asBoolean() ? Util.ENABLED : "added"});
                            } else {
                                groups.addLine(new String[]{sg, "n/a"});
                            }
                        } else {
                            groups.addLine(new String[]{sg, "no response"});
                        }
                    }
                }

                if(otherServerGroups != null) {
                    for(String sg : otherServerGroups) {
                        groups.addLine(new String[]{sg, "not added"});
                    }
                }
                ctx.printLine(groups.toString(true));
        } else {
            final StrictSizeTable table = new StrictSizeTable(1);
            table.addCell(Util.NAME, result.get(Util.NAME).asString());
            table.addCell(Util.RUNTIME_NAME, result.get(Util.RUNTIME_NAME).asString());
            table.addCell(Util.PERSISTENT, result.get(Util.PERSISTENT).asString());
            table.addCell(Util.ENABLED, result.get(Util.ENABLED).asString());
            table.addCell(Util.STATUS, result.get(Util.STATUS).asString());
            ctx.printLine(table.toString());
        }
        } finally {
            addedServerGroups = null;
            otherServerGroups = null;
        }
    }
}
