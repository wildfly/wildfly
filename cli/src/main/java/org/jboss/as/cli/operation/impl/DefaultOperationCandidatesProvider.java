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
package org.jboss.as.cli.operation.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.as.cli.CommandArgument;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.operation.OperationCandidatesProvider;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.OperationRequestHeader;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class DefaultOperationCandidatesProvider implements OperationCandidatesProvider {

    private static final Map<String, OperationRequestHeader> HEADERS = Collections.<String, OperationRequestHeader>singletonMap(RolloutPlanRequestHeader.INSTANCE.getName(), RolloutPlanRequestHeader.INSTANCE);

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CandidatesProvider#getNodeNames(org.jboss.as.cli.Prefix)
     */
    @Override
    public List<String> getNodeNames(CommandContext ctx, OperationRequestAddress prefix) {

        ModelControllerClient client = ctx.getModelControllerClient();
        if(client == null) {
            return Collections.emptyList();
        }

        if(prefix.isEmpty()) {
            throw new IllegalArgumentException("The prefix must end on a type but it's empty.");
        }

        if(!prefix.endsOnType()) {
            throw new IllegalArgumentException("The prefix doesn't end on a type.");
        }

        final ModelNode request;
        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder(prefix);
        try {
            builder.setOperationName("read-children-names");
            builder.addProperty("child-type", prefix.getNodeType());
            request = builder.buildRequest();
        } catch (OperationFormatException e1) {
            throw new IllegalStateException("Failed to build operation", e1);
        }

        List<String> result;
        try {
            ModelNode outcome = client.execute(request);
            if (!Util.isSuccess(outcome)) {
                // TODO logging... exception?
                result = Collections.emptyList();
            } else {
                result = Util.getList(outcome);
            }
        } catch (Exception e) {
            result = Collections.emptyList();
        }
        return result;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CandidatesProvider#getNodeTypes(org.jboss.as.cli.Prefix)
     */
    @Override
    public List<String> getNodeTypes(CommandContext ctx, OperationRequestAddress prefix) {
        return Util.getNodeTypes(ctx.getModelControllerClient(), prefix);
    }

    @Override
    public List<String> getOperationNames(CommandContext ctx, OperationRequestAddress prefix) {

        ModelControllerClient client = ctx.getModelControllerClient();
        if(client == null) {
            return Collections.emptyList();
        }

        if(prefix.endsOnType()) {
            throw new IllegalArgumentException("The prefix isn't expected to end on a type.");
        }

        ModelNode request;
        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder(prefix);
        try {
            builder.setOperationName("read-operation-names");
            request = builder.buildRequest();
        } catch (OperationFormatException e1) {
            throw new IllegalStateException("Failed to build operation", e1);
        }

        List<String> result;
        try {
            ModelNode outcome = client.execute(request);
            if (!Util.isSuccess(outcome)) {
                // TODO logging... exception?
                result = Collections.emptyList();
            } else {
                result = Util.getList(outcome);
            }
        } catch (Exception e) {
            result = Collections.emptyList();
        }
        return result;
    }

    @Override
    public List<CommandArgument> getProperties(CommandContext ctx, String operationName, OperationRequestAddress address) {

        ModelControllerClient client = ctx.getModelControllerClient();
        if(client == null) {
            return Collections.emptyList();
        }

/*        if(address.endsOnType()) {
            throw new IllegalArgumentException("The prefix isn't expected to end on a type.");
        }
*/
        ModelNode request;
        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder(address);
        try {
            builder.setOperationName("read-operation-description");
            builder.addProperty("name", operationName);
            request = builder.buildRequest();
        } catch (OperationFormatException e1) {
            throw new IllegalStateException("Failed to build operation", e1);
        }

        List<CommandArgument> result;
        try {
            ModelNode outcome = client.execute(request);
            if (!Util.isSuccess(outcome)) {
                result = Collections.emptyList();
            } else {
                outcome.get("request-properties");
                final List<String> names = Util.getRequestPropertyNames(outcome);
                result = new ArrayList<CommandArgument>(names.size());
                for(final String name : names) {
                    result.add(new CommandArgument(){
                        final String argName = name;
                        @Override
                        public String getFullName() {
                            return argName;
                        }

                        @Override
                        public String getShortName() {
                            return null;
                        }

                        @Override
                        public int getIndex() {
                            return -1;
                        }

                        @Override
                        public boolean isPresent(ParsedCommandLine args) throws CommandFormatException {
                            return args.hasProperty(argName);
                        }

                        @Override
                        public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                            return !isPresent(ctx.getParsedCommandLine());
                        }

                        @Override
                        public String getValue(ParsedCommandLine args) throws CommandFormatException {
                            return args.getPropertyValue(argName);
                        }

                        @Override
                        public String getValue(ParsedCommandLine args, boolean required) throws CommandFormatException {
                            if(!isPresent(args)) {
                                throw new CommandFormatException("Property '" + argName + "' is missing required value.");
                            }
                            return args.getPropertyValue(argName);
                        }

                        @Override
                        public boolean isValueComplete(ParsedCommandLine args) throws CommandFormatException {
                            if(!isPresent(args)) {
                                return false;
                            }
                            if(argName.equals(args.getLastParsedPropertyName())) {
                                return false;
                            }
                            return true;
                        }

                        @Override
                        public boolean isValueRequired() {
                            return true;
                        }

                        @Override
                        public CommandLineCompleter getValueCompleter() {
                            return null;
                        }});
                }
            }
        } catch (Exception e) {
            result = Collections.emptyList();
        }
        return result;
    }

    @Override
    public Map<String, OperationRequestHeader> getHeaders(CommandContext ctx) {
        return HEADERS;
    }
}
