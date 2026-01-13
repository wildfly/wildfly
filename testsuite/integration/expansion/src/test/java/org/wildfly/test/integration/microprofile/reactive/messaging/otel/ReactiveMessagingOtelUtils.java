/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.otel;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.wildfly.extension.microprofile.reactive.messaging.MicroProfileReactiveMessagingExtension.SUBSYSTEM_NAME;

import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.wildfly.extension.microprofile.reactive.messaging.MicroProfileReactiveMessagingConnectorOpenTelemetryTracingResourceDefinition;
import org.wildfly.microprofile.reactive.messaging.config.TracingType;

public class ReactiveMessagingOtelUtils {
    private static final PathAddress SUBSYSTEM_ADDRESS = PathAddress.pathAddress(SUBSYSTEM, SUBSYSTEM_NAME);
    private static final PathAddress RESOURCE_ADDRESS = SUBSYSTEM_ADDRESS.append(MicroProfileReactiveMessagingConnectorOpenTelemetryTracingResourceDefinition.PATH);

    static Set<String> readChildrenNames(ModelControllerClient client, PathAddress addr, String childType) throws Exception{
        ModelNode readChildren = Operations.createOperation(ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION, addr.toModelNode());
        readChildren.get(CHILD_TYPE).set(new ModelNode(childType));
        ModelNode result = client.execute(readChildren);
        Assert.assertTrue(Operations.isSuccessfulOutcome(result));
        result = Operations.readResult(result);
        return result.asList().stream().map(ModelNode::asString).collect(Collectors.toSet());
    }

    static void enableConnectorOpenTelemetryResource(ModelControllerClient client, boolean add) throws Exception {
        Set<String> names =
                readChildrenNames(client, SUBSYSTEM_ADDRESS, MicroProfileReactiveMessagingConnectorOpenTelemetryTracingResourceDefinition.PATH.getKey());


        ModelNode op = null;
        if (names.contains(MicroProfileReactiveMessagingConnectorOpenTelemetryTracingResourceDefinition.PATH.getValue())) {
            if (!add) {
                // Remove it
                op = Operations.createRemoveOperation(RESOURCE_ADDRESS.toModelNode());
            }
        } else {
            if (add) {
                // Add it
                op = Operations.createAddOperation(RESOURCE_ADDRESS.toModelNode());
            }
        }

        if (op != null) {
            ModelNode result = client.execute(op);
            boolean outcome = Operations.isSuccessfulOutcome(result);
            if (!outcome) {
                System.err.println(result);
            }
            Assert.assertTrue(outcome);
        }
    }

    static void setTracingConfigSystemProperty(ModelControllerClient client, String tracingPropertyName, Boolean value) throws Exception {
        Set<String> names =
                readChildrenNames(client, PathAddress.EMPTY_ADDRESS, SYSTEM_PROPERTY);

        PathAddress propAddr = PathAddress.pathAddress(SYSTEM_PROPERTY, tracingPropertyName);

        ModelNode op = null;
        if (value == null) {
            if (names.contains(tracingPropertyName)) {
                // Remove this
                op = Operations.createRemoveOperation(propAddr.toModelNode());
            }
        } else {
            if (names.contains(tracingPropertyName)) {
                // Write the value
                op = Operations.createWriteAttributeOperation(propAddr.toModelNode(), VALUE, value);
            } else {
                // Add the resource
                op = Operations.createAddOperation(propAddr.toModelNode());
                op.get(VALUE).set(new ModelNode(value));
            }
        }

        if (op != null) {
            ModelNode result = client.execute(op);
            Assert.assertTrue(Operations.isSuccessfulOutcome(result));
        }
    }

    static void setConnectorTracingType(ModelControllerClient client, String tracingAttributeName, TracingType tracingType) throws Exception {
        setConnectorTracingType(client, tracingAttributeName, tracingType.toString());
    }

    static void setConnectorTracingType(ModelControllerClient client, String tracingAttributeName, String tracingType) throws Exception {
        enableConnectorOpenTelemetryResource(client, true);
        ModelNode op =
                Operations.createWriteAttributeOperation(
                        RESOURCE_ADDRESS.toModelNode(), tracingAttributeName, tracingType);
            ModelNode result = client.execute(op);
            Assert.assertTrue(Operations.isSuccessfulOutcome(result));
    }

    static void reload(ModelControllerClient client) throws Exception {
        ServerReload.executeReloadAndWaitForCompletion(client);
    }
}
