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

import static org.jboss.as.controller.client.helpers.ClientConstants.DEPLOYMENT;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP_ADDR;

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

    public static boolean isWindows() {
        return SecurityActions.getSystemProperty("os.name").toLowerCase().indexOf("windows") >= 0;
    }

    public static boolean isSuccess(ModelNode operationResult) {
        if(operationResult != null) {
            ModelNode outcome = operationResult.get("outcome");
            return outcome != null && outcome.asString().equals("success");
        }
        return false;
    }

    public static String getFailureDescription(ModelNode operationResult) {
        if(operationResult == null) {
            return null;
        }
        ModelNode descr = operationResult.get("failure-description");
        if(descr == null) {
            return null;
        }
        return descr.asString();
    }

    public static List<String> getList(ModelNode operationResult) {
        if(!operationResult.hasDefined("result"))
            return Collections.emptyList();

        List<ModelNode> nodeList = operationResult.get("result").asList();
        if(nodeList.isEmpty())
            return Collections.emptyList();

        List<String> list = new ArrayList<String>(nodeList.size());
        for(ModelNode node : nodeList) {
            list.add(node.asString());
        }
        return list;
    }

    public static boolean listContains(ModelNode operationResult, String item) {
        if(!operationResult.hasDefined("result"))
            return false;

        List<ModelNode> nodeList = operationResult.get("result").asList();
        if(nodeList.isEmpty())
            return false;

        for(ModelNode node : nodeList) {
            if(node.asString().equals(item)) {
                return true;
            }
        }
        return false;
    }

    public static byte[] getHash(ModelNode operationResult) {
        if(!operationResult.hasDefined("result"))
            return null;
        return operationResult.get("result").asBytes();
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
            builder.operationName("read-children-names");
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
                builder.operationName("read-children-names");
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
                builder.operationName("read-children-names");
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
            builder.operationName("read-children-names");
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
            builder.operationName("read-children-names");
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
            builder.operationName("read-children-types");
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
            builder.operationName("read-children-names");
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
            builder.operationName("read-children-names");
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
            builder.operationName("read-children-names");
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
        op.get(OP).set(operationName);
        if (serverGroup != null) {
            op.get(OP_ADDR).add("server-group", serverGroup);
        }
        op.get(OP_ADDR).add(DEPLOYMENT, uniqueName);
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
}
