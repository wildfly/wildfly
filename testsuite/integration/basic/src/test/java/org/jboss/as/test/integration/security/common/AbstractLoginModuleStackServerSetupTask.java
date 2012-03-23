package org.jboss.as.test.integration.security.common;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.MODULE_OPTIONS;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.security.Constants;
import org.jboss.dmr.ModelNode;

/**
 * {@link ServerSetupTask} instance for stacking login-modules.
 * 
 * @author Josef Cacek
 */
public abstract class AbstractLoginModuleStackServerSetupTask extends AbstractSecurityDomainSetup {

    private static String SECURITY_DOMAIN_NAME = "test-security-domain";

    protected ManagementClient managementClient;

    // Protected methods -----------------------------------------------------

    /**
     * @return
     * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup#getSecurityDomainName()
     */
    @Override
    protected String getSecurityDomainName() {
        return SECURITY_DOMAIN_NAME;
    }

    /**
     * Returns configuration of the login
     * 
     * @return
     */
    protected abstract LoginModuleConfiguration[] getLoginModuleConfigurations();

    @Override
    public void setup(final ManagementClient managementClient, String containerId) throws Exception {
        this.managementClient = managementClient;
        final LoginModuleConfiguration[] configurations = getLoginModuleConfigurations();
        if (ArrayUtils.isEmpty(configurations)) {
            throw new IllegalStateException("No LoginModuleConfiguration provided.");
        }

        final List<ModelNode> updates = new ArrayList<ModelNode>();
        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        final String securityDomainName = getSecurityDomainName();
        op.get(OP_ADDR).add(SECURITY_DOMAIN, securityDomainName);
        updates.add(op);
        op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, securityDomainName);

        op.get(OP_ADDR).add(Constants.AUTHENTICATION, Constants.CLASSIC);
        for (final LoginModuleConfiguration config : configurations) {
            final ModelNode loginModule = op.get(Constants.LOGIN_MODULES).add();

            loginModule.get(ModelDescriptionConstants.CODE).set(config.getName());
            loginModule.get(FLAG).set(StringUtils.defaultIfEmpty(config.getFlag(), "required"));

            Map<String, String> configOptions = config.getOptions();
            if (configOptions == null) {
                configOptions = Collections.emptyMap();
            }
            final ModelNode moduleOptionsNode = loginModule.get(MODULE_OPTIONS);
            for (final Map.Entry<String, String> entry : configOptions.entrySet()) {
                moduleOptionsNode.add(entry.getKey(), entry.getValue());
            }

            op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        }
        updates.add(op);
        applyUpdates(managementClient.getControllerClient(), updates);
    }

    /**
     * 
     * @param managementClient
     * @param containerId
     * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup#tearDown(org.jboss.as.arquillian.container.ManagementClient,
     *      java.lang.String)
     */
    @Override
    public void tearDown(ManagementClient managementClient, String containerId) {
        super.tearDown(managementClient, containerId);
        this.managementClient = null;
    }

    // Embedded classes ------------------------------------------------------

    /**
     * A LoginModuleConfiguration.
     */
    public interface LoginModuleConfiguration {
        /**
         * Not-null name/code of the LoginModule.
         * 
         * @return
         */
        String getName();

        /**
         * Flag of the LoginModule. If it's empty, then "required" is used.
         * 
         * @return
         */
        String getFlag();

        /**
         * LoginModule options as a Map.
         * 
         * @return
         */
        Map<String, String> getOptions();
    }
}
