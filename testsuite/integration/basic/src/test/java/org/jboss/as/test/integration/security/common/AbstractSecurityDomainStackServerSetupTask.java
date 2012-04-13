package org.jboss.as.test.integration.security.common;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.MODULE_OPTIONS;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.security.Constants;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * {@link ServerSetupTask} instance for security domain setup. It support stack of login-modules and policy-modules too.
 * 
 * @author Josef Cacek
 */
public abstract class AbstractSecurityDomainStackServerSetupTask implements ServerSetupTask {

    private static final Logger LOGGER = Logger.getLogger(AbstractSecurityDomainStackServerSetupTask.class);

    /** The SUBSYSTEM_SECURITY */
    private static final String SUBSYSTEM_SECURITY = "security";
    public static final String SECURITY_DOMAIN_NAME = "test-security-domain";

    protected ManagementClient managementClient;

    // Public methods --------------------------------------------------------

    public final void setup(final ManagementClient managementClient, String containerId) throws Exception {
        final String securityDomainName = getSecurityDomainName();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Adding security domain " + securityDomainName);
        }
        this.managementClient = managementClient;

        final List<ModelNode> updates = new ArrayList<ModelNode>();
        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_SECURITY);
        op.get(OP_ADDR).add(SECURITY_DOMAIN, securityDomainName);
        updates.add(op);

        final ModelNode authnNode = createSecurityModelNode(Constants.AUTHENTICATION, Constants.LOGIN_MODULES,
                getLoginModuleConfigurations());
        if (authnNode != null) {
            updates.add(authnNode);
        }
        final ModelNode authzNode = createSecurityModelNode(Constants.AUTHORIZATION, Constants.POLICY_MODULES,
                getAuthorizationModuleConfigurations());
        if (authzNode != null) {
            updates.add(authzNode);
        }

        applyUpdates(managementClient.getControllerClient(), updates);
    }

    /**
     * 
     * @param managementClient
     * @param containerId
     * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup#tearDown(org.jboss.as.arquillian.container.ManagementClient,
     *      java.lang.String)
     */
    public final void tearDown(ManagementClient managementClient, String containerId) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Removing security domain " + getSecurityDomainName());
        }
        final ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(Constants.SECURITY_DOMAIN, getSecurityDomainName());
        // Don't rollback when the AS detects the war needs the module
        op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);

        try {
            applyUpdate(managementClient.getControllerClient(), op, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.managementClient = null;
    }

    // Protected methods -----------------------------------------------------

    /**
     * @return
     * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup#getSecurityDomainName()
     */
    protected String getSecurityDomainName() {
        return SECURITY_DOMAIN_NAME;
    }

    /**
     * Returns configuration of the login modules.
     * 
     * @return
     */
    protected SecurityModuleConfiguration[] getLoginModuleConfigurations() {
        return null;
    }

    /**
     * Returns configuration of the authorization modules.
     * 
     * @return
     */
    protected SecurityModuleConfiguration[] getAuthorizationModuleConfigurations() {
        return null;
    }

    /**
     * Applies given list of updates in the given client.
     * 
     * @param client
     * @param updates
     */
    protected static final void applyUpdates(final ModelControllerClient client, final List<ModelNode> updates) {
        for (ModelNode update : updates) {
            try {
                applyUpdate(client, update, false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 
     * @param client
     * @param update
     * @param allowFailure
     * @throws Exception
     */
    protected static final void applyUpdate(final ModelControllerClient client, ModelNode update, boolean allowFailure)
            throws Exception {
        ModelNode result = client.execute(new OperationBuilder(update).build());
        if (result.hasDefined("outcome") && (allowFailure || "success".equals(result.get("outcome").asString()))) {
            if (result.hasDefined("result")) {
                System.out.println(result.get("result"));
            }
        } else if (result.hasDefined("failure-description")) {
            throw new RuntimeException(result.get("failure-description").toString());
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
        }
    }

    // Private methods -------------------------------------------------------

    /**
     * Creates a {@link ModelNode} with the security component configuration. If the securityConfigurations array is empty or
     * null, then null is returned.
     * 
     * @param securityComponent name of security component {@link Constants#AUTHORIZATION}/{@link Constants#AUTHENTICATION}
     * @param subnodeName name of the security component subnode, which holds module configurations
     * @param securityConfigurations configurations
     * @return ModelNode instance or null
     */
    private ModelNode createSecurityModelNode(String securityComponent, String subnodeName,
            final SecurityModuleConfiguration[] securityConfigurations) {
        if (securityConfigurations == null || securityConfigurations.length == 0) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("No security configuration for " + securityComponent + " module.");
            }
            return null;
        }
        final ModelNode securityComponentNode = new ModelNode();
        securityComponentNode.get(OP).set(ADD);
        securityComponentNode.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_SECURITY);
        securityComponentNode.get(OP_ADDR).add(SECURITY_DOMAIN, getSecurityDomainName());

        securityComponentNode.get(OP_ADDR).add(securityComponent, Constants.CLASSIC);
        for (final SecurityModuleConfiguration config : securityConfigurations) {
            final ModelNode securityModuleNode = securityComponentNode.get(subnodeName).add();

            final String code = config.getName();
            final String flag = StringUtils.defaultIfEmpty(config.getFlag(), Constants.REQUIRED);
            securityModuleNode.get(ModelDescriptionConstants.CODE).set(code);
            securityModuleNode.get(FLAG).set(flag);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Adding " + securityComponent + " module [code=" + code + ", flag=" + flag + "]");
            }

            Map<String, String> configOptions = config.getOptions();
            if (configOptions == null) {
                LOGGER.info("No module options provided.");
                configOptions = Collections.emptyMap();
            }
            final ModelNode moduleOptionsNode = securityModuleNode.get(MODULE_OPTIONS);
            for (final Map.Entry<String, String> entry : configOptions.entrySet()) {
                final String optionName = entry.getKey();
                final String optionValue = entry.getValue();
                moduleOptionsNode.add(optionName, optionValue);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Adding module option [" + optionName + "=" + optionValue + "]");
                }
            }

            securityComponentNode.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        }
        return securityComponentNode;
    }

    // Embedded classes ------------------------------------------------------

    /**
     * A SecurityModuleConfiguration.
     */
    public interface SecurityModuleConfiguration {
        /**
         * Not-null name/code of this security module.
         * 
         * @return
         */
        String getName();

        /**
         * Flag of the security module. If it's empty, then "required" is used.
         * 
         * @return
         */
        String getFlag();

        /**
         * Security module options as a Map.
         * 
         * @return
         */
        Map<String, String> getOptions();
    }

    /**
     * A simple abstract implementation of {@link SecurityModuleConfiguration}, which uses default flag and default (i.e. empty)
     * module options. Subclasses has to implement {@link #getName()} method.
     */
    public static abstract class AbstractSecurityModuleConfiguration implements SecurityModuleConfiguration {

        /**
         * @return <code>null</code>
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainStackServerSetupTask.SecurityModuleConfiguration#getFlag()
         */
        public String getFlag() {
            return null;
        }

        /**
         * @return <code>null</code>
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainStackServerSetupTask.SecurityModuleConfiguration#getOptions()
         */
        public Map<String, String> getOptions() {
            return null;
        }

    }

}
