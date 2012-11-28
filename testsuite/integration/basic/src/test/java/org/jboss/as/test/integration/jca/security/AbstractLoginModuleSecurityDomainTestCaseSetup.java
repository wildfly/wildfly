package org.jboss.as.test.integration.jca.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CODE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.LOGIN_MODULE;
import static org.jboss.as.security.Constants.MODULE_OPTIONS;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.security.Constants;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.dmr.ModelNode;

/**
 * @author Stuart Douglas
 */
public abstract class AbstractLoginModuleSecurityDomainTestCaseSetup extends AbstractSecurityDomainSetup {
    @Override
    protected abstract String getSecurityDomainName();

    protected abstract String getLoginModuleName();

    protected abstract boolean isRequired();

    protected abstract Map<String, String> getModuleOptions();

    @Override
    public void setup(final ManagementClient managementClient, String containerId) throws Exception {
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
        ModelNode loginModule = Util.createAddOperation(address.append(LOGIN_MODULE, getLoginModuleName()));

        loginModule.get(CODE).set(getLoginModuleName());
        if (!isRequired()) {
            loginModule.get(FLAG).set("optional");
        } else {
            loginModule.get(FLAG).set("required");
        }
        loginModule.get(MODULE_OPTIONS).add("password-stacking", "useFirstPass");
        Map<String, String> options = getModuleOptions();
        Set<String> keys = options.keySet();

        for (String key : keys) {
            loginModule.get(MODULE_OPTIONS).add(key, options.get(key));
        }

        loginModule.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        steps.add(loginModule);

        applyUpdates(managementClient.getControllerClient(), Arrays.asList(compositeOp));
    }
}
