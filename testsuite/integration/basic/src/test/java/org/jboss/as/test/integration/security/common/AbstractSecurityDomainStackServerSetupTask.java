/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
import static org.jboss.as.security.Constants.TYPE;

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
 * {@link ServerSetupTask} instance for security domain setup. It supports stacks of login-modules, policy-modules and
 * (role-)mapping-modules.
 * 
 * @author Josef Cacek
 */
public abstract class AbstractSecurityDomainStackServerSetupTask implements ServerSetupTask {

    private static final Logger LOGGER = Logger.getLogger(AbstractSecurityDomainStackServerSetupTask.class);

    /** The type attribute value of mapping-modules used for role assignment. */
    private static final String ROLE = "role";
    /** The SUBSYSTEM_SECURITY */
    private static final String SUBSYSTEM_SECURITY = "security";
    public static final String SECURITY_DOMAIN_NAME = "test-security-domain";

    protected ManagementClient managementClient;

    // Public methods --------------------------------------------------------

    /**
     * Adds a security domain represented by this class to the AS configuration.
     * 
     * @param managementClient
     * @param containerId
     * @throws Exception
     * @see org.jboss.as.arquillian.api.ServerSetupTask#setup(org.jboss.as.arquillian.container.ManagementClient,
     *      java.lang.String)
     */
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

        final ModelNode authnNode = createSecurityModelNode(Constants.AUTHENTICATION, Constants.LOGIN_MODULES, FLAG,
                Constants.REQUIRED, getLoginModuleConfigurations());
        if (authnNode != null) {
            updates.add(authnNode);
        }
        final ModelNode authzNode = createSecurityModelNode(Constants.AUTHORIZATION, Constants.POLICY_MODULES, FLAG,
                Constants.REQUIRED, getAuthorizationModuleConfigurations());
        if (authzNode != null) {
            updates.add(authzNode);
        }
        final ModelNode mappingNode = createSecurityModelNode(Constants.MAPPING, Constants.MAPPING_MODULES, TYPE, ROLE,
                getMappingModuleConfigurations());
        if (mappingNode != null) {
            updates.add(mappingNode);
        }
        applyUpdates(managementClient.getControllerClient(), updates);
    }

    /**
     * Removes the security domain from the AS configuration.
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
     * Returns name of the security domain. The default value is {@value #SECURITY_DOMAIN_NAME}.
     * 
     * @return
     * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup#getSecurityDomainName()
     */
    protected String getSecurityDomainName() {
        return SECURITY_DOMAIN_NAME;
    }

    /**
     * Returns configuration of the login modules.<br/>
     * It can return null or empty array if no such module is necessary for this security domain.
     * 
     * @return array of SecurityModuleConfiguration
     */
    protected SecurityModuleConfiguration[] getLoginModuleConfigurations() {
        return null;
    }

    /**
     * Returns configuration of the authorization modules.<br/>
     * It can return null or empty array if no such module is necessary for this security domain.
     * 
     * @return array of SecurityModuleConfiguration
     */
    protected SecurityModuleConfiguration[] getAuthorizationModuleConfigurations() {
        return null;
    }

    /**
     * Returns configuration of the (role-)mapping modules. The {@link SecurityModuleConfiguration#getFlag()} method should
     * return value of <code>type</code> attribute in the mapping module, it can also return <code>null</code> - then the
     * default value {@value #ROLE} is used.<br/>
     * This method can return null or empty array if no such module is necessary for this security domain.
     * 
     * @return array of SecurityModuleConfiguration
     */
    protected SecurityModuleConfiguration[] getMappingModuleConfigurations() {
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
     * Executes updates defined in the given ModelNode instance in the ModelControllerClient.
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
     * @param securityComponent name of security component (e.g. {@link Constants#AUTHORIZATION})
     * @param subnodeName name of the security component subnode, which holds module configurations (e.g.
     *        {@link Constants#POLICY_MODULES})
     * @param flagAttributeName name of attribute to which the value of {@link SecurityModuleConfiguration#getFlag()} is set
     * @param flagDefaultValue default value for flagAttributeName attr.
     * @param securityConfigurations configurations
     * @return ModelNode instance or null
     */
    private ModelNode createSecurityModelNode(String securityComponent, String subnodeName, String flagAttributeName,
            String flagDefaultValue, final SecurityModuleConfiguration[] securityConfigurations) {
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
            final String flag = StringUtils.defaultIfEmpty(config.getFlag(), flagDefaultValue);
            securityModuleNode.get(ModelDescriptionConstants.CODE).set(code);
            securityModuleNode.get(flagAttributeName).set(flag);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Adding " + securityComponent + " module [code=" + code + ", " + flagAttributeName + "=" + flag
                        + "]");
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
     * This interface represents a single security module configuration.
     */
    public interface SecurityModuleConfiguration {
        /**
         * Not-null name/code of this security module.
         * 
         * @return
         */
        String getName();

        /**
         * Returns Value of flag (authentication, authorization) or type (value mapping) attribute of the security module. If
         * leaved empty or null, then a default value for the given security module is used.
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
