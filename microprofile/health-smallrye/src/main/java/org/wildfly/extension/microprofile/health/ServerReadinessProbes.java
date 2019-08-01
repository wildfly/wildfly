package org.wildfly.extension.microprofile.health;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BOOT_ERRORS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STATUS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.util.List;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

class ServerReadinessProbes {

    private static final ModelNode READ_SERVER_STATE_ATTRIBUTE;
    private static final ModelNode READ_BOOT_ERRORS;
    private static final ModelNode READ_DEPLOYMENTS_STATUS;

    static {
        READ_SERVER_STATE_ATTRIBUTE = new ModelNode();
        READ_SERVER_STATE_ATTRIBUTE.get(OP).set(READ_ATTRIBUTE_OPERATION);
        READ_SERVER_STATE_ATTRIBUTE.get(OP_ADDR).set(new ModelNode());
        READ_SERVER_STATE_ATTRIBUTE.get(NAME).set("server-state");

        READ_BOOT_ERRORS = new ModelNode();
        READ_BOOT_ERRORS.get(OP).set("read-boot-errors");
        READ_BOOT_ERRORS.get(OP_ADDR).add(CORE_SERVICE, MANAGEMENT);

        READ_DEPLOYMENTS_STATUS = new ModelNode();
        READ_DEPLOYMENTS_STATUS.get(OP).set(READ_ATTRIBUTE_OPERATION);
        READ_DEPLOYMENTS_STATUS.get(OP_ADDR).add(DEPLOYMENT, "*");
        READ_DEPLOYMENTS_STATUS.get(NAME).set(STATUS);

    }

    /**
     * Check that the server-state attribute value is "running"
     */
    static class ServerStateCheck implements HealthCheck {

        private LocalModelControllerClient modelControllerClient;

        ServerStateCheck(LocalModelControllerClient modelControllerClient) {
            this.modelControllerClient = modelControllerClient;
        }

        @Override
        public HealthCheckResponse call() {
            ModelNode response = modelControllerClient.execute(READ_SERVER_STATE_ATTRIBUTE);

            HealthCheckResponseBuilder check = HealthCheckResponse.named("server-state");

            if (!SUCCESS.equals(response.get(OUTCOME).asStringOrNull())) {
                return check.down().build();
            }
            if (response.hasDefined(FAILURE_DESCRIPTION)) {
                return check.down()
                        .withData(FAILURE_DESCRIPTION, response.get(FAILURE_DESCRIPTION).asString())
                        .build();
            }
            ModelNode result = response.get(RESULT);
            if (!result.isDefined()) {
                return check.down().build();
            }
            String value = result.asString();
            return check.state("running".equals(value))
                    .withData(VALUE, value)
                    .build();
        }
    }

    /**
     * Check that /core-service=management:read-boot-errors does not report any errors.
     */
    static class NoBootErrorsCheck implements HealthCheck {

        private LocalModelControllerClient modelControllerClient;

        NoBootErrorsCheck(LocalModelControllerClient modelControllerClient) {
            this.modelControllerClient = modelControllerClient;
        }

        @Override
        public HealthCheckResponse call() {
            ModelNode response = modelControllerClient.execute(READ_BOOT_ERRORS);
            HealthCheckResponseBuilder check = HealthCheckResponse.named(BOOT_ERRORS);

            if (!SUCCESS.equals(response.get(OUTCOME).asStringOrNull())) {
                return check.down().build();
            }
            if (response.hasDefined(FAILURE_DESCRIPTION)) {
                return check.down()
                        .withData(FAILURE_DESCRIPTION, response.get(FAILURE_DESCRIPTION).asString())
                        .build();
            }
            ModelNode result = response.get(RESULT);
            if (!result.isDefined()) {
                return check.down().build();
            }
            List<ModelNode> errors = result.asList();
            if (errors.isEmpty()) {
                return check.up().build();
            }
            return check.down()
                    .withData(BOOT_ERRORS, result.toJSONString(true))
                    .build();
        }
    }

    /**
     * Check that all deployments status are OK
     */
    static class DeploymentsStatusCheck implements HealthCheck {

        private LocalModelControllerClient modelControllerClient;

        DeploymentsStatusCheck(LocalModelControllerClient modelControllerClient) {
            this.modelControllerClient = modelControllerClient;
        }

        @Override
        public HealthCheckResponse call() {
            ModelNode responses = modelControllerClient.execute(READ_DEPLOYMENTS_STATUS);
            HealthCheckResponseBuilder check = HealthCheckResponse.named("deployments-status");

            if (!SUCCESS.equals(responses.get(OUTCOME).asStringOrNull())) {
                return check.down().build();
            }
            if (!responses.get(RESULT).isDefined()) {
                return check.down().build();
            }

            boolean globalStatus = true;
            for (ModelNode response : responses.get(RESULT).asList()) {
                boolean deploymentUp = false;
                String data = null;

                if (!SUCCESS.equals(response.get(OUTCOME).asStringOrNull())) {
                    data = "DMR Query failed";
                } else if (response.hasDefined(FAILURE_DESCRIPTION)) {
                    data = response.get(FAILURE_DESCRIPTION).asString();
                }
                ModelNode result = response.get(RESULT);
                if (!result.isDefined()) {
                    data = "status undefined";
                }
                String value = result.asString();
                if ("OK".equals(value)) {
                    deploymentUp = true;
                }
                if (data == null) {
                    data = value;
                }

                PathAddress address = PathAddress.pathAddress(response.get(OP_ADDR));
                String deploymentName = address.getElement(0).getValue();
                check.withData(deploymentName, data);
                globalStatus = globalStatus && deploymentUp;
            }

            return check.state(globalStatus)
                    .build();
        }
    }

}
