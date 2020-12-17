
/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.health;

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

import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

class ServerProbes {

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
    static class ServerStateCheck implements ServerProbe {

        private LocalModelControllerClient modelControllerClient;

        public ServerStateCheck(LocalModelControllerClient modelControllerClient) {
            this.modelControllerClient = modelControllerClient;
        }

        @Override
        public Outcome getOutcome() {
            ModelNode response = modelControllerClient.execute(READ_SERVER_STATE_ATTRIBUTE);

            if (!SUCCESS.equals(response.get(OUTCOME).asStringOrNull())) {
                return Outcome.FAILURE;
            }
            if (response.hasDefined(FAILURE_DESCRIPTION)) {
                ModelNode data = new ModelNode();
                data.add(FAILURE_DESCRIPTION, response.get(FAILURE_DESCRIPTION).asString());
                return new Outcome(false, data);
            }
            ModelNode result = response.get(RESULT);
            if (!result.isDefined()) {
                return Outcome.FAILURE;
            }
            String value = result.asString();
            ModelNode data = new ModelNode();
            data.add(VALUE, value);
            return new Outcome("running".equals(value), data);
        }

        @Override
        public String getName() {
            return "server-state";
        }
    }

    /**
     * Check that /core-service=management:read-boot-errors does not report any errors.
     */
    static class NoBootErrorsCheck implements ServerProbe {

        private LocalModelControllerClient modelControllerClient;

        NoBootErrorsCheck(LocalModelControllerClient modelControllerClient) {
            this.modelControllerClient = modelControllerClient;
        }

        @Override
        public Outcome getOutcome() {
            ModelNode response = modelControllerClient.execute(READ_BOOT_ERRORS);

            if (!SUCCESS.equals(response.get(OUTCOME).asStringOrNull())) {
                return Outcome.FAILURE;
            }
            if (response.hasDefined(FAILURE_DESCRIPTION)) {
                ModelNode data = new ModelNode();
                data.add(FAILURE_DESCRIPTION, response.get(FAILURE_DESCRIPTION).asString());
                return new Outcome(false, data);
            }
            ModelNode result = response.get(RESULT);
            if (!result.isDefined()) {
                return Outcome.FAILURE;
            }
            List<ModelNode> errors = result.asList();
            if (errors.isEmpty()) {
                return Outcome.SUCCESS;
            }

            ModelNode data = new ModelNode();
            data.add(BOOT_ERRORS, result.toJSONString(true));
            return new Outcome(false, data);
        }

        @Override
        public String getName() {
            return "boot-errors";
        }
    }


    /**
     * Check that all deployments status are OK
     */
    static class DeploymentsStatusCheck implements ServerProbe {

        private LocalModelControllerClient modelControllerClient;

        DeploymentsStatusCheck(LocalModelControllerClient modelControllerClient) {
            this.modelControllerClient = modelControllerClient;
        }

        @Override
        public Outcome getOutcome() {

            ModelNode responses = modelControllerClient.execute(READ_DEPLOYMENTS_STATUS);

            if (!SUCCESS.equals(responses.get(OUTCOME).asStringOrNull())) {
                return Outcome.FAILURE;
            }
            if (!responses.get(RESULT).isDefined()) {
                return Outcome.FAILURE;
            }

            ModelNode data = new ModelNode();
            boolean globalStatus = true;
            for (ModelNode response : responses.get(RESULT).asList()) {
                boolean deploymentUp = false;
                String info = null;

                if (!SUCCESS.equals(response.get(OUTCOME).asStringOrNull())) {
                    info = "DMR Query failed";
                } else if (response.hasDefined(FAILURE_DESCRIPTION)) {
                    info = response.get(FAILURE_DESCRIPTION).asString();
                }
                ModelNode result = response.get(RESULT);
                if (!result.isDefined()) {
                    info = "status undefined";
                }
                String value = result.asString();
                if ("OK".equals(value)) {
                    deploymentUp = true;
                }
                if (info == null) {
                    info = value;
                }

                PathAddress address = PathAddress.pathAddress(response.get(OP_ADDR));
                String deploymentName = address.getElement(0).getValue();
                data.add(deploymentName, info);
                globalStatus = globalStatus && deploymentUp;
            }

            return new Outcome(globalStatus, data);
        }

        @Override
        public String getName() {
            return "deployments-status";
        }
    }


}