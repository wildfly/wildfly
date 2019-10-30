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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.AUTH_MODULE;
import static org.jboss.as.security.Constants.CLASSIC;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.JSSE;
import static org.jboss.as.security.Constants.KEYSTORE;
import static org.jboss.as.security.Constants.LOGIN_MODULE;
import static org.jboss.as.security.Constants.MODULE_OPTIONS;
import static org.jboss.as.security.Constants.PASSWORD;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;
import static org.jboss.as.security.Constants.TRUSTSTORE;
import static org.jboss.as.security.Constants.TYPE;
import static org.jboss.as.security.Constants.URL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.security.common.config.AuthnModule;
import org.jboss.as.test.integration.security.common.config.JSSE;
import org.jboss.as.test.integration.security.common.config.JaspiAuthn;
import org.jboss.as.test.integration.security.common.config.LoginModuleStack;
import org.jboss.as.test.integration.security.common.config.SecureStore;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.shared.ServerReload;
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

    /**
     * The type attribute value of mapping-modules used for role assignment.
     */
    private static final String ROLE = "role";
    /**
     * The SUBSYSTEM_SECURITY
     */
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

        // TODO remove this once security domains expose their own capability
        // Currently subsystem=security-domain exposes one, but the individual domains don't
        // which with WFCORE-1106 has the effect that any individual sec-domain op that puts
        // the server in reload-required means all ops for any sec-domain won't execute Stage.RUNTIME
        // So, for now we preemptively reload if needed
        ServerReload.BeforeSetupTask.INSTANCE.setup(managementClient, containerId);

        final List<ModelNode> updates = new LinkedList<ModelNode>();
        for (final SecurityDomain securityDomain : securityDomains) {
            final String securityDomainName = securityDomain.getName();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.trace("Adding security domain " + securityDomainName);
            }
            final ModelNode compositeOp = new ModelNode();
            compositeOp.get(OP).set(COMPOSITE);
            compositeOp.get(OP_ADDR).setEmptyList();
            ModelNode steps = compositeOp.get(STEPS);

            PathAddress opAddr = PathAddress.pathAddress()
                    .append(SUBSYSTEM, SUBSYSTEM_SECURITY)
                    .append(SECURITY_DOMAIN, securityDomainName);
            ModelNode op = Util.createAddOperation(opAddr);
            if (StringUtils.isNotEmpty(securityDomain.getCacheType())) {
                op.get(org.jboss.as.test.integration.security.common.Constants.CACHE_TYPE).set(securityDomain.getCacheType());
            }
            steps.add(op);

            //only one can occur - authenticationType or authenticationJaspiType
            final boolean authNodeAdded = createSecurityModelNode(org.jboss.as.test.integration.security.common.Constants.AUTHENTICATION, LOGIN_MODULE, FLAG,
                    org.jboss.as.test.integration.security.common.Constants.REQUIRED, securityDomain.getLoginModules(), securityDomainName, steps);
            if (!authNodeAdded) {
                final List<ModelNode> jaspiAuthnNodes = createJaspiAuthnNodes(securityDomain.getJaspiAuthn(), securityDomain.getName());
                if (jaspiAuthnNodes != null) {
                    for (ModelNode node : jaspiAuthnNodes) {
                        steps.add(node);
                    }
                }
            }
            createSecurityModelNode(org.jboss.as.test.integration.security.common.Constants.AUTHORIZATION, org.jboss.as.test.integration.security.common.Constants.POLICY_MODULE, FLAG, org.jboss.as.test.integration.security.common.Constants.REQUIRED, securityDomain.getAuthorizationModules(), securityDomainName, steps);
            createSecurityModelNode(org.jboss.as.test.integration.security.common.Constants.MAPPING, org.jboss.as.test.integration.security.common.Constants.MAPPING_MODULE, TYPE, ROLE, securityDomain.getMappingModules(), securityDomainName, steps);

            final ModelNode jsseNode = createJSSENode(securityDomain.getJsse(), securityDomain.getName());
            if (jsseNode != null) {
                steps.add(jsseNode);
            }
            updates.add(compositeOp);
        }
        CoreUtils.applyUpdates(updates, managementClient.getControllerClient());
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
                LOGGER.trace("Removing security domain " + domainName);
            }
            final ModelNode op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(SUBSYSTEM, "security");
            op.get(OP_ADDR).add(SECURITY_DOMAIN, domainName);
            // Don't rollback when the AS detects the war needs the module
            op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
            op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            updates.add(op);
        }
        CoreUtils.applyUpdates(updates, managementClient.getControllerClient());

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
            LOGGER.trace("No security configuration for JASPI module.");
            return null;
        }
        if (securityConfigurations.getAuthnModules() == null || securityConfigurations.getAuthnModules().length == 0
                || securityConfigurations.getLoginModuleStacks() == null
                || securityConfigurations.getLoginModuleStacks().length == 0) {
            throw new IllegalArgumentException("Missing mandatory part of JASPI configuration in the security domain.");
        }

        final List<ModelNode> steps = new ArrayList<ModelNode>();

        PathAddress domainAddress = PathAddress.pathAddress()
                .append(SUBSYSTEM, SUBSYSTEM_SECURITY)
                .append(SECURITY_DOMAIN, domainName);
        PathAddress jaspiAddress = domainAddress.append(org.jboss.as.test.integration.security.common.Constants.AUTHENTICATION, org.jboss.as.test.integration.security.common.Constants.JASPI);
        steps.add(Util.createAddOperation(jaspiAddress));

        for (final AuthnModule config : securityConfigurations.getAuthnModules()) {
            LOGGER.trace("Adding auth-module: " + config);
            final ModelNode securityModuleNode = Util.createAddOperation(jaspiAddress.append(AUTH_MODULE,config.getName()));
            steps.add(securityModuleNode);
            securityModuleNode.get(ModelDescriptionConstants.CODE).set(config.getName());
            if (config.getFlag() != null) {
                securityModuleNode.get(FLAG).set(config.getFlag());
            }
            if (config.getModule() != null) {
                securityModuleNode.get(org.jboss.as.test.integration.security.common.Constants.MODULE).set(config.getModule());
            }
            if (config.getLoginModuleStackRef() != null) {
                securityModuleNode.get(org.jboss.as.test.integration.security.common.Constants.LOGIN_MODULE_STACK_REF).set(config.getLoginModuleStackRef());
            }
            Map<String, String> configOptions = config.getOptions();
            if (configOptions == null) {
                LOGGER.trace("No module options provided.");
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


        for (final LoginModuleStack lmStack : securityConfigurations.getLoginModuleStacks()) {
            PathAddress lmStackAddress = jaspiAddress.append(org.jboss.as.test.integration.security.common.Constants.LOGIN_MODULE_STACK, lmStack.getName());
            steps.add(Util.createAddOperation(lmStackAddress));

            for (final SecurityModule config : lmStack.getLoginModules()) {
                final String code = config.getName();
                final ModelNode securityModuleNode = Util.createAddOperation(lmStackAddress.append(LOGIN_MODULE, code));

                final String flag = StringUtils.defaultIfEmpty(config.getFlag(), org.jboss.as.test.integration.security.common.Constants.REQUIRED);
                securityModuleNode.get(ModelDescriptionConstants.CODE).set(code);
                securityModuleNode.get(FLAG).set(flag);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.trace("Adding JASPI login module stack [code=" + code + ", flag=" + flag + "]");
                }
                Map<String, String> configOptions = config.getOptions();
                if (configOptions == null) {
                    LOGGER.trace("No module options provided.");
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
                securityModuleNode.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
                steps.add(securityModuleNode);
            }

        }

        return steps;
    }

    /**
     * Creates a {@link ModelNode} with the security component configuration. If the securityConfigurations array is empty or
     * null, then null is returned.
     *
     * @param securityComponent name of security component (e.g. {@link org.jboss.as.test.integration.security.common.Constants#AUTHORIZATION})
     * @param subnodeName       name of the security component subnode, which holds module configurations (e.g.
     *                          {@link org.jboss.as.test.integration.security.common.Constants#POLICY_MODULES})
     * @param flagAttributeName name of attribute to which the value of {@link SecurityModule#getFlag()} is set
     * @param flagDefaultValue  default value for flagAttributeName attr.
     * @param securityModules   configurations
     * @return ModelNode instance or null
     */
    private boolean createSecurityModelNode(String securityComponent, String subnodeName, String flagAttributeName,
                                            String flagDefaultValue, final SecurityModule[] securityModules, String domainName, ModelNode operations) {
        if (securityModules == null || securityModules.length == 0) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.trace("No security configuration for " + securityComponent + " module.");
            }
            return false;
        }
        PathAddress address = PathAddress.pathAddress()
                .append(SUBSYSTEM, SUBSYSTEM_SECURITY)
                .append(SECURITY_DOMAIN, domainName)
                .append(securityComponent, CLASSIC);
        operations.add(Util.createAddOperation(address));

        for (final SecurityModule config : securityModules) {
            final String code = config.getName();
            final ModelNode securityModuleNode = Util.createAddOperation(address.append(subnodeName, code));

            final String flag = StringUtils.defaultIfEmpty(config.getFlag(), flagDefaultValue);
            securityModuleNode.get(ModelDescriptionConstants.CODE).set(code);
            securityModuleNode.get(flagAttributeName).set(flag);
            Map<String, String> configOptions = config.getOptions();
            if (configOptions == null) {
                LOGGER.trace("No module options provided.");
                configOptions = Collections.emptyMap();
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.trace("Adding " + securityComponent + " module [code=" + code + ", " + flagAttributeName + "=" + flag
                        + ", options = " + configOptions + "]");
            }
            final ModelNode moduleOptionsNode = securityModuleNode.get(MODULE_OPTIONS);
            for (final Map.Entry<String, String> entry : configOptions.entrySet()) {
                final String optionName = entry.getKey();
                final String optionValue = entry.getValue();
                moduleOptionsNode.add(optionName, optionValue);
            }
            securityModuleNode.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            operations.add(securityModuleNode);
        }
        return true;
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
                LOGGER.trace("No security configuration for JSSE module.");
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
