package org.jboss.as.test.integration.security.common;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.security.Constants;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.LOGIN_MODULE;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;

/**
 * @author Stuart Douglas
 */
public abstract class AbstractSecurityDomainSetup implements ServerSetupTask {
    private static final Logger LOGGER = Logger.getLogger(AbstractSecurityDomainSetup.class);

    protected static void applyUpdates(final ModelControllerClient client, final List<ModelNode> updates) {
        for (ModelNode update : updates) {
            try {
                applyUpdate(client, update, false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected static void applyUpdate(final ModelControllerClient client, ModelNode update, boolean allowFailure) throws IOException {
        ModelNode result = client.execute(new OperationBuilder(update).build());
        if (result.hasDefined("outcome") && (allowFailure || "success".equals(result.get("outcome").asString()))) {
            if (result.hasDefined("result")) {
                LOGGER.trace(result.get("result"));
            }
        } else if (result.hasDefined("failure-description")) {
            throw new RuntimeException(result.get("failure-description").toString());
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
        }
    }

    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId) {

        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(Constants.SECURITY_DOMAIN, getSecurityDomainName());
        // Don't rollback when the AS detects the war needs the module
        op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);

        try {
            applyUpdate(managementClient.getControllerClient(), op, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract String getSecurityDomainName();


    protected void createSecurityDomain(final Class loginModuleClass, final Map<String, String> moduleOptionsCache, final ModelControllerClient client) {
        final ModelNode compositeOp = new ModelNode();
        compositeOp.get(OP).set(COMPOSITE);
        compositeOp.get(OP_ADDR).setEmptyList();
        ModelNode steps = compositeOp.get(STEPS);
        PathAddress address = PathAddress.pathAddress()
                .append(SUBSYSTEM, "security")
                .append(SECURITY_DOMAIN, getSecurityDomainName());

        steps.add(Util.createAddOperation(address));
        address = address.append(Constants.AUTHENTICATION, Constants.CLASSIC);
        steps.add(Util.createAddOperation(address));

        ModelNode loginModule = Util.createAddOperation(address.append(LOGIN_MODULE, loginModuleClass.getName()));
        loginModule.get(ModelDescriptionConstants.CODE).set(loginModuleClass.getName());
        loginModule.get(FLAG).set("required");
        loginModule.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);

        ModelNode moduleOptions = loginModule.get("module-options");
        for (Map.Entry<String, String> entry : moduleOptionsCache.entrySet()) {
            moduleOptions.get(entry.getKey()).set(entry.getValue());
        }
        steps.add(loginModule);

        try {
            applyUpdates(client, Arrays.asList(compositeOp));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
