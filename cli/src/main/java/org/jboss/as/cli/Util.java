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
package org.jboss.as.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 *
 * @author Alexey Loubyansky
 */
public class Util {

    public static final String ACCESS_TYPE = "access-type";
    public static final String ADD = "add";
    public static final String ADDRESS = "address";
    public static final String ATTRIBUTES = "attributes";
    public static final String BYTES = "bytes";
    public static final String CHILDREN = "children";
    public static final String COMPOSITE = "composite";
    public static final String CONCURRENT_GROUPS = "concurrent-groups";
    public static final String CONTENT = "content";
    public static final String DEPLOY = "deploy";
    public static final String DEPLOYMENT = "deployment";
    public static final String DESCRIPTION = "description";
    public static final String DOMAIN_FAILURE_DESCRIPTION = "domain-failure-description";
    public static final String EXPRESSIONS_ALLOWED = "expressions-allowed";
    public static final String HEAD_COMMENT_ALLOWED = "head-comment-allowed";
    public static final String FAILURE_DESCRIPTION = "failure-description";
    public static final String FULL_REPLACE_DEPLOYMENT = "full-replace-deployment";
    public static final String IN_SERIES = "in-series";
    public static final String INCLUDE_DEFAULTS = "include-defaults";
    public static final String INCLUDE_RUNTIME = "include-runtime";
    public static final String INPUT_STREAM_INDEX = "input-stream-index";
    public static final String MIN_OCCURS = "min-occurs";
    public static final String MAX_OCCURS = "max-occurs";
    public static final String NAME = "name";
    public static final String NILLABLE = "nillable";
    public static final String OPERATION = "operation";
    public static final String OPERATION_HEADERS = "operation-headers";
    public static final String OUTCOME = "outcome";
    public static final String PROFILE = "profile";
    public static final String READ_ATTRIBUTE = "read-attribute";
    public static final String READ_CHILDREN_NAMES = "read-children-names";
    public static final String READ_CHILDREN_TYPES = "read-children-types";
    public static final String READ_ONLY = "read-only";
    public static final String READ_OPERATION_DESCRIPTION = "read-operation-description";
    public static final String READ_OPERATION_NAMES = "read-operation-names";
    public static final String READ_WRITE = "read-write";
    public static final String READ_RESOURCE = "read-resource";
    public static final String READ_RESOURCE_DESCRIPTION = "read-resource-description";
    public static final String REQUEST_PROPERTIES = "request-properties";
    public static final String REQUIRED = "required";
    public static final String RESTART_REQUIRED = "restart-required";
    public static final String RESULT = "result";
    public static final String ROLLOUT_PLAN = "rollout-plan";
    public static final String RUNTIME_NAME = "runtime-name";
    public static final String SERVER_GROUP = "server-group";
    public static final String STEP_1 = "step-1";
    public static final String STEP_2 = "step-2";
    public static final String STEP_3 = "step-3";
    public static final String STEPS = "steps";
    public static final String STORAGE = "storage";
    public static final String SUCCESS = "success";
    public static final String TAIL_COMMENT_ALLOWED = "tail-comment-allowed";
    public static final String TRUE = "true";
    public static final String TYPE = "type";
    public static final String VALIDATE_ADDRESS = "validate-address";
    public static final String VALUE = "value";
    public static final String VALUE_TYPE = "value-type";
    public static final String WRITE_ATTRIBUTE = "write-attribute";

    public static boolean isWindows() {
        return SecurityActions.getSystemProperty("os.name").toLowerCase().indexOf("windows") >= 0;
    }

    public static boolean isSuccess(ModelNode operationResult) {
        if(operationResult != null) {
            return operationResult.hasDefined(Util.OUTCOME) && operationResult.get(Util.OUTCOME).asString().equals(Util.SUCCESS);
        }
        return false;
    }

    public static String getFailureDescription(ModelNode operationResult) {
        if(operationResult == null) {
            return null;
        }
        ModelNode descr = operationResult.get(Util.FAILURE_DESCRIPTION);
        if(descr == null) {
            return null;
        }
        if(descr.hasDefined(Util.DOMAIN_FAILURE_DESCRIPTION)) {
            descr = descr.get(Util.DOMAIN_FAILURE_DESCRIPTION);
        }
        return descr.asString();
    }

    public static List<String> getList(ModelNode operationResult) {
        if(!operationResult.hasDefined(RESULT))
            return Collections.emptyList();
        List<ModelNode> nodeList = operationResult.get(RESULT).asList();
        if(nodeList.isEmpty())
            return Collections.emptyList();
        List<String> list = new ArrayList<String>(nodeList.size());
        for(ModelNode node : nodeList) {
            list.add(node.asString());
        }
        return list;
    }

    public static boolean listContains(ModelNode operationResult, String item) {
        if(!operationResult.hasDefined(RESULT))
            return false;

        List<ModelNode> nodeList = operationResult.get(RESULT).asList();
        if(nodeList.isEmpty())
            return false;

        for(ModelNode node : nodeList) {
            if(node.asString().equals(item)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> getRequestPropertyNames(ModelNode operationResult) {
        if(!operationResult.hasDefined("result"))
            return Collections.emptyList();

        ModelNode result = operationResult.get("result");
        if(!result.hasDefined("request-properties"))
            return Collections.emptyList();

        List<Property> nodeList = result.get("request-properties").asPropertyList();
        if(nodeList.isEmpty())
            return Collections.emptyList();

        List<String> list = new ArrayList<String>(nodeList.size());
        for(Property node : nodeList) {
            list.add(node.getName());
        }
        return list;
    }

    public static boolean isDeploymentInRepository(String name, ModelControllerClient client) {
        return getDeployments(client).contains(name);
    }

    public static boolean isDeployedAndEnabledInStandalone(String name, ModelControllerClient client) {

        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
        ModelNode request;
        try {
            builder.setOperationName("read-children-names");
            builder.addProperty("child-type", "deployment");
            request = builder.buildRequest();
        } catch (OperationFormatException e) {
            throw new IllegalStateException("Failed to build operation", e);
        }

        try {
            ModelNode outcome = client.execute(request);
            if (isSuccess(outcome)) {
                if(!listContains(outcome, name)) {
                    return false;
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        builder = new DefaultOperationRequestBuilder();
        builder.addNode("deployment", name);
        builder.setOperationName("read-attribute");
        builder.addProperty("name", "enabled");
        try {
            request = builder.buildRequest();
        } catch (OperationFormatException e) {
            throw new IllegalStateException("Failed to build operation", e);
        }

        try {
            ModelNode outcome = client.execute(request);
            if (isSuccess(outcome)) {
                if(!outcome.hasDefined("result")) {
                    return false;
                }
                return outcome.get("result").asBoolean();
            }
        } catch(Exception e) {
        }
        return false;
    }

    public static List<String> getAllEnabledServerGroups(String deploymentName, ModelControllerClient client) {

        List<String> serverGroups = getServerGroups(client);
        if(serverGroups.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<String>();
        for(String serverGroup : serverGroups) {
            DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
            ModelNode request;
            try {
                builder.setOperationName("read-children-names");
                builder.addNode("server-group", serverGroup);
                builder.addProperty("child-type", "deployment");
                request = builder.buildRequest();
            } catch (OperationFormatException e) {
                throw new IllegalStateException("Failed to build operation", e);
            }

            try {
                ModelNode outcome = client.execute(request);
                if (isSuccess(outcome)) {
                    if(!listContains(outcome, deploymentName)) {
                        continue;
                    }
                } else {
                    continue;
                }
            } catch (Exception e) {
                continue;
            }

            builder = new DefaultOperationRequestBuilder();
            builder.addNode("server-group", serverGroup);
            builder.addNode("deployment", deploymentName);
            builder.setOperationName("read-attribute");
            builder.addProperty("name", "enabled");
            try {
                request = builder.buildRequest();
            } catch (OperationFormatException e) {
                throw new IllegalStateException("Failed to build operation", e);
            }

            try {
                ModelNode outcome = client.execute(request);
                if (isSuccess(outcome)) {
                    if(!outcome.hasDefined("result")) {
                        continue;
                    }
                    if(outcome.get("result").asBoolean()) {
                        result.add(serverGroup);
                    }
                }
            } catch(Exception e) {
                continue;
            }
        }

        return result;
    }

    public static List<String> getAllReferencingServerGroups(String deploymentName, ModelControllerClient client) {

        List<String> serverGroups = getServerGroups(client);
        if(serverGroups.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<String>();
        for(String serverGroup : serverGroups) {
            DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
            ModelNode request;
            try {
                builder.setOperationName("read-children-names");
                builder.addNode("server-group", serverGroup);
                builder.addProperty("child-type", "deployment");
                request = builder.buildRequest();
            } catch (OperationFormatException e) {
                throw new IllegalStateException("Failed to build operation", e);
            }

            try {
                ModelNode outcome = client.execute(request);
                if (isSuccess(outcome)) {
                    if(listContains(outcome, deploymentName)) {
                        result.add(serverGroup);
                    }
                }
            } catch (Exception e) {
            }
        }
        return result;
    }

    public static List<String> getDeployments(ModelControllerClient client) {

        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
        final ModelNode request;
        try {
            builder.setOperationName("read-children-names");
            builder.addProperty("child-type", "deployment");
            request = builder.buildRequest();
        } catch (OperationFormatException e) {
            throw new IllegalStateException("Failed to build operation", e);
        }

        try {
            ModelNode outcome = client.execute(request);
            if (isSuccess(outcome)) {
                return getList(outcome);
            }
        } catch (Exception e) {
        }

        return Collections.emptyList();
    }

    public static List<String> getServerGroups(ModelControllerClient client) {

        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
        final ModelNode request;
        try {
            builder.setOperationName("read-children-names");
            builder.addProperty("child-type", "server-group");
            request = builder.buildRequest();
        } catch (OperationFormatException e) {
            throw new IllegalStateException("Failed to build operation", e);
        }

        try {
            ModelNode outcome = client.execute(request);
            if (isSuccess(outcome)) {
                return getList(outcome);
            }
        } catch (Exception e) {
        }

        return Collections.emptyList();
    }

    public static List<String> getNodeTypes(ModelControllerClient client, OperationRequestAddress address) {
        if(client == null) {
            return Collections.emptyList();
        }

        if(address.endsOnType()) {
            throw new IllegalArgumentException("The prefix isn't expected to end on a type.");
        }

        ModelNode request;
        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder(address);
        try {
            builder.setOperationName(Util.READ_CHILDREN_TYPES);
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

    public static List<String> getNodeNames(ModelControllerClient client, OperationRequestAddress address, String type) {
        if(client == null) {
            return Collections.emptyList();
        }

        if(address != null && address.endsOnType()) {
            throw new IllegalArgumentException("The address isn't expected to end on a type.");
        }

        final ModelNode request;
        DefaultOperationRequestBuilder builder = address == null ? new DefaultOperationRequestBuilder() : new DefaultOperationRequestBuilder(address);
        try {
            builder.setOperationName("read-children-names");
            builder.addProperty("child-type", type);
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

    public static List<String> getJmsResources(ModelControllerClient client, String profile, String type) {

        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
        final ModelNode request;
        try {
            if(profile != null) {
                builder.addNode("profile", profile);
            }
            builder.addNode("subsystem", "messaging");
            builder.setOperationName("read-children-names");
            builder.addProperty("child-type", type);
            request = builder.buildRequest();
        } catch (OperationFormatException e) {
            throw new IllegalStateException("Failed to build operation", e);
        }

        try {
            ModelNode outcome = client.execute(request);
            if (isSuccess(outcome)) {
                return getList(outcome);
            }
        } catch (Exception e) {
        }

        return Collections.emptyList();
    }

    public static List<String> getDatasources(ModelControllerClient client, String profile, String dsType) {

        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
        final ModelNode request;
        try {
            if(profile != null) {
                builder.addNode("profile", profile);
            }
            builder.addNode("subsystem", "datasources");
            builder.setOperationName("read-children-names");
            builder.addProperty("child-type", dsType);
            request = builder.buildRequest();
        } catch (OperationFormatException e) {
            throw new IllegalStateException("Failed to build operation", e);
        }

        try {
            ModelNode outcome = client.execute(request);
            if (isSuccess(outcome)) {
                return getList(outcome);
            }
        } catch (Exception e) {
        }

        return Collections.emptyList();
    }

    public static boolean isTopic(ModelControllerClient client, String name) {
        List<String> topics = getJmsResources(client, null, "jms-topic");
        return topics.contains(name);
    }

    public static boolean isQueue(ModelControllerClient client, String name) {
        List<String> queues = getJmsResources(client, null, "jms-queue");
        return queues.contains(name);
    }

    public static boolean isConnectionFactory(ModelControllerClient client, String name) {
        List<String> cf = getJmsResources(client, null, "connection-factory");
        return cf.contains(name);
    }

    public static ModelNode configureDeploymentOperation(String operationName, String uniqueName, String serverGroup) {
        ModelNode op = new ModelNode();
        op.get(OPERATION).set(operationName);
        if (serverGroup != null) {
            op.get(ADDRESS).add("server-group", serverGroup);
        }
        op.get(ADDRESS).add(DEPLOYMENT, uniqueName);
        return op;
    }

    public static String getCommonStart(List<String> list) {
        final int size = list.size();
        if(size == 0) {
            return null;
        }
        if(size == 1) {
            return list.get(0);
        }
        final String first = list.get(0);
        final String last = list.get(size - 1);

        int minSize = Math.min(first.length(), last.length());
        for(int i = 0; i < minSize; ++i) {
            if(first.charAt(i) != last.charAt(i)) {
                if(i == 0) {
                    return null;
                } else {
                    return first.substring(0, i);
                }
            }
        }
        return first.substring(0, minSize);
    }

    public static String escapeString(String name, EscapeSelector selector) {
        for(int i = 0; i < name.length(); ++i) {
            char ch = name.charAt(i);
            if(selector.isEscape(ch)) {
                StringBuilder builder = new StringBuilder();
                builder.append(name, 0, i);
                builder.append('\\').append(ch);
                for(int j = i + 1; j < name.length(); ++j) {
                    ch = name.charAt(j);
                    if(selector.isEscape(ch)) {
                        builder.append('\\');
                    }
                    builder.append(ch);
                }
                return builder.toString();
            }
        }
        return name;
    }

    public static void sortAndEscape(List<String> candidates, EscapeSelector selector) {
        Collections.sort(candidates);
        final String common = Util.getCommonStart(candidates);
        if (common != null) {
            final String escapedCommon = Util.escapeString(common, selector);
            if (common.length() != escapedCommon.length()) {
                for (int i = 0; i < candidates.size(); ++i) {
                    candidates.set(i, escapedCommon + candidates.get(i).substring(common.length()));
                }
            }
        }
    }

    public static void setRequestProperty(ModelNode request, String name, String value) {
        if(name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("The argument name is not specified: '" + name + "'");
        if(value == null || value.trim().isEmpty())
            throw new IllegalArgumentException("The argument value is not specified: '" + value + "'");
        ModelNode toSet = null;
        try {
            toSet = ModelNode.fromString(value);
        } catch (Exception e) {
            // just use the string
            toSet = new ModelNode().set(value);
        }
        request.get(name).set(toSet);
    }

    public static ModelNode buildRequest(CommandContext ctx, final OperationRequestAddress address, String operation)
            throws CommandFormatException {
        final ModelNode request = new ModelNode();
        request.get(Util.OPERATION).set(operation);
        final ModelNode addressNode = request.get(Util.ADDRESS);
        if (address.isEmpty()) {
            addressNode.setEmptyList();
        } else {
            if(address.endsOnType()) {
                throw new CommandFormatException("The address ends on a type: " + address.getNodeType());
            }
            for(OperationRequestAddress.Node node : address) {
                addressNode.add(node.getType(), node.getName());
            }
        }
        return request;
    }
}
