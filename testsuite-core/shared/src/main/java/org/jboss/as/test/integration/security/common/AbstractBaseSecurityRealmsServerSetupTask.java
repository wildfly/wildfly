/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2014, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */
package org.jboss.as.test.integration.security.common;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADVANCED_FILTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_EMPTY_PASSWORDS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHENTICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHORIZATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BASE_DN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONNECTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LDAP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LDAP_CONNECTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECRET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_IDENTITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SSL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TRUSTSTORE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERNAME_ATTRIBUTE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USER_DN;
import static org.jboss.as.domain.management.ModelDescriptionConstants.INITIAL_CONTEXT_FACTORY;
import static org.jboss.as.domain.management.ModelDescriptionConstants.SEARCH_CREDENTIAL;
import static org.jboss.as.domain.management.ModelDescriptionConstants.SEARCH_DN;
import static org.jboss.as.domain.management.ModelDescriptionConstants.SECURITY_REALM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.security.common.config.realm.Authentication;
import org.jboss.as.test.integration.security.common.config.realm.Authorization;
import org.jboss.as.test.integration.security.common.config.realm.LdapAuthentication;
import org.jboss.as.test.integration.security.common.config.realm.RealmKeystore;
import org.jboss.as.test.integration.security.common.config.realm.SecurityRealm;
import org.jboss.as.test.integration.security.common.config.realm.ServerIdentity;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 *
 * @see org.jboss.as.test.integration.security.common.config.realm.SecurityRealm
 * @author Josef Cacek
 */
public abstract class AbstractBaseSecurityRealmsServerSetupTask {

    private static final Logger LOGGER = Logger.getLogger(AbstractBaseSecurityRealmsServerSetupTask.class);
    private static final String KEYSTORE_PATH = "keystore-path";

    private SecurityRealm[] securityRealms;

    // Protected methods -----------------------------------------------------

    protected void setup(final ModelControllerClient modelControllerClient, String containerId) throws Exception {
        securityRealms = getSecurityRealms();

        if (securityRealms == null || securityRealms.length == 0) {
            LOGGER.warn("Empty security realm configuration.");
            return;
        }

        final List<ModelNode> updates = new LinkedList<ModelNode>();
        for (final SecurityRealm securityRealm : securityRealms) {
            final String securityRealmName = securityRealm.getName();
            LOGGER.info("Adding security realm " + securityRealmName);
            final ModelNode compositeOp = new ModelNode();
            compositeOp.get(OP).set(COMPOSITE);
            compositeOp.get(OP_ADDR).setEmptyList();
            ModelNode steps = compositeOp.get(STEPS);

            // /core-service=management/security-realm=foo:add
            final PathAddress realmAddr = getBaseAddress().append(CORE_SERVICE, MANAGEMENT)
                    .append(SECURITY_REALM, securityRealmName);
            ModelNode op = Util.createAddOperation(realmAddr);
            steps.add(op);

            final ServerIdentity serverIdentity = securityRealm.getServerIdentity();
            if (serverIdentity != null) {
                // /core-service=management/security-realm=foo/server-identity=secret:add(value="Q29ubmVjdGlvblBhc3N3b3JkMSE=")
                if (StringUtils.isNotEmpty(serverIdentity.getSecret())) {
                    final ModelNode secretModuleNode = Util.createAddOperation(realmAddr.append(SERVER_IDENTITY, SECRET));
                    secretModuleNode.get(Constants.VALUE).set(serverIdentity.getSecret());
                    secretModuleNode.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
                    steps.add(secretModuleNode);
                }
                // /core-service=management/security-realm=JBossTest/server-identity=ssl:add(keystore-path=server.keystore, keystore-password=123456)
                final RealmKeystore ssl = serverIdentity.getSsl();
                if (ssl != null) {
                    final ModelNode sslModuleNode = Util.createAddOperation(realmAddr.append(SERVER_IDENTITY, SSL));
                    sslModuleNode.get(KEYSTORE_PATH).set(ssl.getKeystorePath());
                    sslModuleNode.get(Constants.KEYSTORE_PASSWORD).set(ssl.getKeystorePassword());
                    sslModuleNode.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
                    steps.add(sslModuleNode);
                }
            }
            final Authentication authentication = securityRealm.getAuthentication();
            if (authentication != null) {
                final RealmKeystore truststore = authentication.getTruststore();
                if (truststore != null) {
                    final ModelNode sslModuleNode = Util.createAddOperation(realmAddr.append(AUTHENTICATION, TRUSTSTORE));
                    sslModuleNode.get(KEYSTORE_PATH).set(truststore.getKeystorePath());
                    sslModuleNode.get(Constants.KEYSTORE_PASSWORD).set(truststore.getKeystorePassword());
                    sslModuleNode.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
                    steps.add(sslModuleNode);
                }
                final LdapAuthentication ldap = authentication.getLdap();
                if (ldap != null) {
                    // /core-service=management/ldap-connection=bar:add
                    final PathAddress ldapConnectionAddr = getBaseAddress().append(CORE_SERVICE, MANAGEMENT)
                            .append(LDAP_CONNECTION, ldap.getConnection());
                    final ModelNode ldapConnectionNode = Util.createAddOperation(ldapConnectionAddr);
                    setModelAttribute(ldapConnectionNode, SEARCH_DN, ldap.getSearchDn());
                    setModelAttribute(ldapConnectionNode, SEARCH_CREDENTIAL, ldap.getSearchCredential());
                    setModelAttribute(ldapConnectionNode, SECURITY_REALM, ldap.getSecurityRealm());
                    setModelAttribute(ldapConnectionNode, URL, ldap.getUrl());
                    setModelAttribute(ldapConnectionNode, INITIAL_CONTEXT_FACTORY, ldap.getInitialContextFactory());
                    ldapConnectionNode.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
                    steps.add(ldapConnectionNode);

                    final ModelNode ldapNode = Util.createAddOperation(realmAddr.append(AUTHENTICATION, LDAP));
                    setModelAttribute(ldapNode, CONNECTION, ldap.getConnection());
                    setModelAttribute(ldapNode, ADVANCED_FILTER, ldap.getAdvancedFilter());
                    setModelAttribute(ldapNode, BASE_DN, ldap.getBaseDn());
                    setModelAttribute(ldapNode, USER_DN, ldap.getUserDn());
                    setModelAttribute(ldapNode, RECURSIVE, ldap.getRecursive());
                    setModelAttribute(ldapNode, USERNAME_ATTRIBUTE, ldap.getUsernameAttribute());
                    setModelAttribute(ldapNode, ALLOW_EMPTY_PASSWORDS, ldap.getAllowEmptyPasswords());
                    ldapNode.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
                    steps.add(ldapNode);
                }
            }
            final Authorization authorization = securityRealm.getAuthorization();
            if (authorization != null) {
                final ModelNode authorizationNode = Util.createAddOperation(realmAddr.append(AUTHORIZATION, PROPERTIES));
                setModelAttribute(authorizationNode, PATH, authorization.getPath());
                setModelAttribute(authorizationNode, RELATIVE_TO, authorization.getRelativeTo());
                authorizationNode.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
                steps.add(authorizationNode);
            }
            updates.add(compositeOp);
        }
        CoreUtils.applyUpdates(updates, modelControllerClient);
    }

    protected void tearDown(ModelControllerClient modelControllerClient, String containerId) throws Exception {
        if (securityRealms == null || securityRealms.length == 0) {
            LOGGER.warn("Empty security realms configuration.");
            return;
        }

        final List<ModelNode> updates = new ArrayList<ModelNode>();
        List<SecurityRealm> realmsToRemove = Arrays.asList(securityRealms);
        Collections.reverse(realmsToRemove);
        for (final SecurityRealm securityRealm : realmsToRemove) {
            final String realmName = securityRealm.getName();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Removing security realm " + realmName);
            }
            final ModelNode op = Util.createRemoveOperation(getBaseAddress().append(CORE_SERVICE, MANAGEMENT)
                    .append(SECURITY_REALM, realmName));
            op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
            op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            updates.add(op);

            final Authentication authentication = securityRealm.getAuthentication();
            if (authentication != null && authentication.getLdap() != null) {
                // remove ldap connection too
                final ModelNode ldapOp = Util.createRemoveOperation(getBaseAddress().append(CORE_SERVICE, MANAGEMENT)
                        .append(LDAP_CONNECTION, authentication.getLdap().getConnection()));
                ldapOp.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
                ldapOp.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
                updates.add(ldapOp);
            }
        }
        CoreUtils.applyUpdates(updates, modelControllerClient);
        this.securityRealms = null;
    }

    /**
     * Gets the base address to be used for operations in setup/teardown.
     * @return
     */
    protected PathAddress getBaseAddress() {
        return PathAddress.EMPTY_ADDRESS;
    }

    /**
     * Returns configuration for creating security realms.
     *
     * @return array of SecurityRealm instances
     */
    protected abstract SecurityRealm[] getSecurityRealms() throws Exception;

    // Private methods -------------------------------------------------------

    private void setModelAttribute(ModelNode node, String attribute, String value) {
        if (value != null) {
            node.get(attribute).set(value);
        }
    }

    private void setModelAttribute(ModelNode node, String attribute, Boolean value) {
        if (value != null) {
            node.get(attribute).set(value);
        }
    }
}

