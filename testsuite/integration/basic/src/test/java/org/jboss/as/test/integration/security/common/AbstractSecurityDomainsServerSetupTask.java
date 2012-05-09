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
import static org.jboss.as.security.Constants.CLASSIC;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.JSSE;
import static org.jboss.as.security.Constants.KEYSTORE;
import static org.jboss.as.security.Constants.MODULE_OPTIONS;
import static org.jboss.as.security.Constants.PASSWORD;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;
import static org.jboss.as.security.Constants.TRUSTSTORE;
import static org.jboss.as.security.Constants.TYPE;
import static org.jboss.as.security.Constants.URL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.security.Constants;
import org.jboss.as.test.integration.security.common.config.AuthnModule;
import org.jboss.as.test.integration.security.common.config.JSSE;
import org.jboss.as.test.integration.security.common.config.JaspiAuthn;
import org.jboss.as.test.integration.security.common.config.LoginModuleStack;
import org.jboss.as.test.integration.security.common.config.SecureStore;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * {@link ServerSetupTask} instance for security domain setup. It supports JSSE configuration, JASPI authentication
 * configuration and stacks of login-modules (classic authentication), policy-modules and (role-)mapping-modules.
 * 
 * @author Josef Cacek
 */
public abstract class AbstractSecurityDomainsServerSetupTask implements ServerSetupTask {

    private static final Logger LOGGER = Logger.getLogger(AbstractSecurityDomainsServerSetupTask.class);

    /** The type attribute value of mapping-modules used for role assignment. */
    private static final String ROLE = "role";
    /** The SUBSYSTEM_SECURITY */
    private static final String SUBSYSTEM_SECURITY = "security";

    protected ManagementClient managementClient;

    private SecurityDomain[] securityDomains;

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
        this.managementClient = managementClient;
        securityDomains = getSecurityDomains();

        if (securityDomains == null || securityDomains.length == 0) {
            LOGGER.warn("Empty security domain configuration.");
            return;
        }

        final List<ModelNode> updates = new ArrayList<ModelNode>();
        for (final SecurityDomain securityDomain : securityDomains) {
            final String securityDomainName = securityDomain.getName();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Adding security domain " + securityDomainName);
            }

            ModelNode op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_SECURITY);
            op.get(OP_ADDR).add(SECURITY_DOMAIN, securityDomainName);
            updates.add(op);

            //only one can occur - authenticationType or authenticationJaspiType
            final ModelNode authnNode = createSecurityModelNode(Constants.AUTHENTICATION, Constants.LOGIN_MODULES, FLAG,
                    Constants.REQUIRED, securityDomain.getLoginModules(), securityDomain.getName());
            if (authnNode != null) {
                updates.add(authnNode);
            } else {
                final List<ModelNode> jaspiAuthnNodes = createJaspiAuthnNodes(securityDomain.getJaspiAuthn(),
                        securityDomain.getName());
                if (jaspiAuthnNodes != null) {
                    updates.addAll(jaspiAuthnNodes);
                }
            }
            final ModelNode authzNode = createSecurityModelNode(Constants.AUTHORIZATION, Constants.POLICY_MODULES, FLAG,
                    Constants.REQUIRED, securityDomain.getAuthorizationModules(), securityDomain.getName());
            if (authzNode != null) {
                updates.add(authzNode);
            }
            final ModelNode mappingNode = createSecurityModelNode(Constants.MAPPING, Constants.MAPPING_MODULES, TYPE, ROLE,
                    securityDomain.getMappingModules(), securityDomain.getName());
            if (mappingNode != null) {
                updates.add(mappingNode);
            }
            final ModelNode jsseNode = createJSSENode(securityDomain.getJsse(), securityDomain.getName());
            if (jsseNode != null) {
                updates.add(jsseNode);
            }
        }
        Utils.applyUpdates(updates, managementClient.getControllerClient());
    }

    /**
     * Removes the security domain from the AS configuration.
     * 
     * @param managementClient
     * @param containerId
     * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup#tearDown(org.jboss.as.arquillian.container.ManagementClient,
     *      java.lang.String)
     */
    public final void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        if (securityDomains == null || securityDomains.length == 0) {
            LOGGER.warn("Empty security domain configuration.");
            return;
        }

        final List<ModelNode> updates = new ArrayList<ModelNode>();
        for (final SecurityDomain securityDomain : securityDomains) {
            final String domainName = securityDomain.getName();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Removing security domain " + domainName);
            }
            final ModelNode op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(SUBSYSTEM, "security");
            op.get(OP_ADDR).add(Constants.SECURITY_DOMAIN, domainName);
            // Don't rollback when the AS detects the war needs the module
            op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
            updates.add(op);
        }
        Utils.applyUpdates(updates, managementClient.getControllerClient());

        this.managementClient = null;
    }

    // Protected methods -----------------------------------------------------

    /**
     * Returns configuration for creating security domains.
     * 
     * @return array of SecurityDomain
     */
    protected abstract SecurityDomain[] getSecurityDomains() throws Exception;

    // Private methods -------------------------------------------------------

    /**
     * Creates authenticaton=>jaspi node and its child nodes.
     * 
     * @param securityConfigurations
     * @return
     */
    private List<ModelNode> createJaspiAuthnNodes(JaspiAuthn securityConfigurations, String domainName) {
        if (securityConfigurations == null) {
            LOGGER.info("No security configuration for JASPI module.");
            return null;
        }
        if (securityConfigurations.getAuthnModules() == null || securityConfigurations.getAuthnModules().length == 0
                || securityConfigurations.getLoginModuleStacks() == null
                || securityConfigurations.getLoginModuleStacks().length == 0) {
            throw new IllegalArgumentException("Missing mandatory part of JASPI configuration in the security domain.");
        }

        final List<ModelNode> result = new ArrayList<ModelNode>();

        final ModelNode securityComponentNode = new ModelNode();
        securityComponentNode.get(OP).set(ADD);
        securityComponentNode.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_SECURITY);
        securityComponentNode.get(OP_ADDR).add(SECURITY_DOMAIN, domainName);
        securityComponentNode.get(OP_ADDR).add(Constants.AUTHENTICATION, Constants.JASPI);

        for (final AuthnModule config : securityConfigurations.getAuthnModules()) {
            LOGGER.info("Adding auth-module: " + config);
            final ModelNode securityModuleNode = securityComponentNode.get(Constants.AUTH_MODULES).add();
            securityModuleNode.get(ModelDescriptionConstants.CODE).set(config.getName());
            if (config.getFlag() != null) {
                securityModuleNode.get(Constants.FLAG).set(config.getFlag());
            }
            if (config.getModule() != null) {
                securityModuleNode.get(Constants.MODULE).set(config.getModule());
            }
            if (config.getLoginModuleStackRef() != null) {
                securityModuleNode.get(Constants.LOGIN_MODULE_STACK_REF).set(config.getLoginModuleStackRef());
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
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Adding module option [" + optionName + "=" + optionValue + "]");
                }
            }
        }
        //Unable to use securityComponentNode.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true) because the login-module-stack is empty 
        result.add(securityComponentNode);

        for (final LoginModuleStack lmStack : securityConfigurations.getLoginModuleStacks()) {
            final ModelNode stackNode = new ModelNode();
            stackNode.get(OP).set(ADD);
            stackNode.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_SECURITY);
            stackNode.get(OP_ADDR).add(SECURITY_DOMAIN, domainName);
            stackNode.get(OP_ADDR).add(Constants.AUTHENTICATION, Constants.JASPI);
            stackNode.get(OP_ADDR).add(Constants.LOGIN_MODULE_STACK, lmStack.getName());

            for (final SecurityModule config : lmStack.getLoginModules()) {
                final ModelNode securityModuleNode = stackNode.get(Constants.LOGIN_MODULES).add();

                final String code = config.getName();
                final String flag = StringUtils.defaultIfEmpty(config.getFlag(), Constants.REQUIRED);
                securityModuleNode.get(ModelDescriptionConstants.CODE).set(code);
                securityModuleNode.get(Constants.FLAG).set(flag);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Adding JASPI login module stack [code=" + code + ", flag=" + flag + "]");
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
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Adding module option [" + optionName + "=" + optionValue + "]");
                    }
                }
            }
            stackNode.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            result.add(stackNode);
        }

        return result;
    }

    /**
     * Creates a {@link ModelNode} with the security component configuration. If the securityConfigurations array is empty or
     * null, then null is returned.
     * 
     * @param securityComponent name of security component (e.g. {@link Constants#AUTHORIZATION})
     * @param subnodeName name of the security component subnode, which holds module configurations (e.g.
     *        {@link Constants#POLICY_MODULES})
     * @param flagAttributeName name of attribute to which the value of {@link SecurityModuleConfiguration#getFlag()} is set
     * @param flagDefaultValue default value for flagAttributeName attr.
     * @param securityModules configurations
     * @return ModelNode instance or null
     */
    private ModelNode createSecurityModelNode(String securityComponent, String subnodeName, String flagAttributeName,
            String flagDefaultValue, final SecurityModule[] securityModules, String domainName) {
        if (securityModules == null || securityModules.length == 0) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("No security configuration for " + securityComponent + " module.");
            }
            return null;
        }
        final ModelNode securityComponentNode = new ModelNode();
        securityComponentNode.get(OP).set(ADD);
        securityComponentNode.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_SECURITY);
        securityComponentNode.get(OP_ADDR).add(SECURITY_DOMAIN, domainName);

        securityComponentNode.get(OP_ADDR).add(securityComponent, Constants.CLASSIC);
        for (final SecurityModule config : securityModules) {
            final ModelNode securityModuleNode = securityComponentNode.get(subnodeName).add();

            final String code = config.getName();
            final String flag = StringUtils.defaultIfEmpty(config.getFlag(), flagDefaultValue);
            securityModuleNode.get(ModelDescriptionConstants.CODE).set(code);
            securityModuleNode.get(flagAttributeName).set(flag);
            Map<String, String> configOptions = config.getOptions();
            if (configOptions == null) {
                LOGGER.info("No module options provided.");
                configOptions = Collections.emptyMap();
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Adding " + securityComponent + " module [code=" + code + ", " + flagAttributeName + "=" + flag
                        + ", options = " + configOptions + "]");
            }
            final ModelNode moduleOptionsNode = securityModuleNode.get(MODULE_OPTIONS);
            for (final Map.Entry<String, String> entry : configOptions.entrySet()) {
                final String optionName = entry.getKey();
                final String optionValue = entry.getValue();
                moduleOptionsNode.add(optionName, optionValue);
            }
        }
        securityComponentNode.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        return securityComponentNode;
    }

    /**
     * Creates a {@link ModelNode} with configuration of the JSSE part of security domain.
     * 
     * @param jsse
     * @param domainName
     * @return
     */
    private ModelNode createJSSENode(final JSSE jsse, String domainName) {
        if (jsse == null) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("No security configuration for JSSE module.");
            }
            return null;
        }
        final ModelNode securityComponentNode = new ModelNode();
        securityComponentNode.get(OP).set(ADD);
        securityComponentNode.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_SECURITY);
        securityComponentNode.get(OP_ADDR).add(SECURITY_DOMAIN, domainName);

        securityComponentNode.get(OP_ADDR).add(JSSE, CLASSIC);
        addSecureStore(jsse.getTrustStore(), TRUSTSTORE, securityComponentNode);
        addSecureStore(jsse.getKeyStore(), KEYSTORE, securityComponentNode);
        securityComponentNode.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        return securityComponentNode;
    }

    /**
     * Adds given secureStore to a JSSE configuration represented by given ModelNode.
     * 
     * @param secureStore
     * @param storeName
     * @param jsseNode
     */
    private void addSecureStore(SecureStore secureStore, String storeName, ModelNode jsseNode) {
        if (secureStore == null) {
            return;
        }
        if (secureStore.getUrl() != null) {
            jsseNode.get(storeName, URL).set(secureStore.getUrl().toExternalForm());
        }
        if (secureStore.getPassword() != null) {
            jsseNode.get(storeName, PASSWORD).set(secureStore.getPassword());
        }
        if (secureStore.getType() != null) {
            jsseNode.get(storeName, TYPE).set(secureStore.getType());
        }
    }
}
