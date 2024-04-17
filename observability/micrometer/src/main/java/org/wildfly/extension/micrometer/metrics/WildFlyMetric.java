/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer.metrics;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLES;
import static org.wildfly.extension.micrometer._private.MicrometerExtensionLogger.MICROMETER_LOGGER;

import java.util.OptionalDouble;

import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

public class WildFlyMetric implements Metric {

    private static final ModelNode UNDEFINED = new ModelNode();

    private LocalModelControllerClient modelControllerClient;
    private PathAddress address;
    private String attributeName;

    static {
        UNDEFINED.protect();
    }

    public WildFlyMetric() {

    }

    public WildFlyMetric(LocalModelControllerClient modelControllerClient, PathAddress address, String attributeName) {
        this.modelControllerClient = modelControllerClient;
        this.address = address;
        this.attributeName = attributeName;
    }

    @Override
    public OptionalDouble getValue() {
        ModelNode result = readAttributeValue(address, attributeName);
        if (result.isDefined()) {
            try {
                return OptionalDouble.of(result.asDouble());
            } catch (Exception e) {
                MICROMETER_LOGGER.unableToConvertAttribute(attributeName, address, e);
            }
        }
        return OptionalDouble.empty();
    }

    private ModelNode readAttributeValue(PathAddress address, String attributeName) {
        final ModelNode readAttributeOp = new ModelNode();
        readAttributeOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
        readAttributeOp.get(OP_ADDR).set(address.toModelNode());
        readAttributeOp.get(ModelDescriptionConstants.INCLUDE_UNDEFINED_METRIC_VALUES).set(false);
        readAttributeOp.get(NAME).set(attributeName);
        readAttributeOp.get(OPERATION_HEADERS).get(ROLES).add("Monitor");

        ModelNode response = modelControllerClient.execute(readAttributeOp);
        String error = getFailureDescription(response);
        if (error != null) {
            // [WFLY-11933] if the value can not be read if the management resource is not accessible due to RBAC,
            // it is logged it at a lower level.
            if (error.contains("WFLYCTL0216")) {
                MICROMETER_LOGGER.debugf("Unable to read attribute %s: %s.", attributeName, error);
            } else{
                MICROMETER_LOGGER.unableToReadAttribute(attributeName, address, error);
            }
            return UNDEFINED;
        }
        return  response.get(RESULT);
    }

    private String getFailureDescription(ModelNode result) {
        if (result.hasDefined(FAILURE_DESCRIPTION)) {
            return result.get(FAILURE_DESCRIPTION).toString();
        }
        return null;
    }
}
