package org.jboss.as.test.integration.jca.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHENTICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CODE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.CLASSIC;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.LOGIN_MODULES;
import static org.jboss.as.security.Constants.MODULE_OPTIONS;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;

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
    public void setup(final ManagementClient managementClient) throws Exception {
            final List<ModelNode> updates = new ArrayList<ModelNode>();
            ModelNode op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(SUBSYSTEM, "security");
            op.get(OP_ADDR).add(SECURITY_DOMAIN, getSecurityDomainName());
            updates.add(op);
            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(SUBSYSTEM, "security");
            op.get(OP_ADDR).add(SECURITY_DOMAIN, getSecurityDomainName());

            op.get(OP_ADDR).add(AUTHENTICATION, CLASSIC);


            ModelNode loginModule = op.get(LOGIN_MODULES).add();

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

            op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            updates.add(op);

            applyUpdates(managementClient.getControllerClient(), updates);
    }
}
