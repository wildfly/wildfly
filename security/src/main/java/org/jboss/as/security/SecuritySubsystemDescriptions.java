/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.security.Constants.ACL;
import static org.jboss.as.security.Constants.ADDITIONAL_PROPERTIES;
import static org.jboss.as.security.Constants.AUDIT;
import static org.jboss.as.security.Constants.AUDIT_MANAGER_CLASS_NAME;
import static org.jboss.as.security.Constants.AUTHENTICATION;
import static org.jboss.as.security.Constants.AUTHENTICATION_JASPI;
import static org.jboss.as.security.Constants.AUTHENTICATION_MANAGER_CLASS_NAME;
import static org.jboss.as.security.Constants.AUTHORIZATION;
import static org.jboss.as.security.Constants.AUTHORIZATION_MANAGER_CLASS_NAME;
import static org.jboss.as.security.Constants.AUTH_MODULE;
import static org.jboss.as.security.Constants.CACHE_TYPE;
import static org.jboss.as.security.Constants.CIPHER_SUITES;
import static org.jboss.as.security.Constants.CLIENT_ALIAS;
import static org.jboss.as.security.Constants.CLIENT_AUTH;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.DEEP_COPY_SUBJECT_MODE;
import static org.jboss.as.security.Constants.DEFAULT_CALLBACK_HANDLER_CLASS_NAME;
import static org.jboss.as.security.Constants.EXTENDS;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.IDENTITY_TRUST;
import static org.jboss.as.security.Constants.IDENTITY_TRUST_MANAGER_CLASS_NAME;
import static org.jboss.as.security.Constants.JSSE;
import static org.jboss.as.security.Constants.KEYSTORE_PASSWORD;
import static org.jboss.as.security.Constants.KEYSTORE_PROVIDER;
import static org.jboss.as.security.Constants.KEYSTORE_PROVIDER_ARGUMENT;
import static org.jboss.as.security.Constants.KEYSTORE_TYPE;
import static org.jboss.as.security.Constants.KEYSTORE_URL;
import static org.jboss.as.security.Constants.KEY_MANAGER_FACTORY_ALGORITHM;
import static org.jboss.as.security.Constants.KEY_MANAGER_FACTORY_PROVIDER;
import static org.jboss.as.security.Constants.LOGIN_MODULE_STACK;
import static org.jboss.as.security.Constants.LOGIN_MODULE_STACK_REF;
import static org.jboss.as.security.Constants.MAPPING;
import static org.jboss.as.security.Constants.MAPPING_MANAGER_CLASS_NAME;
import static org.jboss.as.security.Constants.MODULE_OPTIONS;
import static org.jboss.as.security.Constants.NAME;
import static org.jboss.as.security.Constants.PROTOCOLS;
import static org.jboss.as.security.Constants.SECURITY_PROPERTIES;
import static org.jboss.as.security.Constants.SERVER_ALIAS;
import static org.jboss.as.security.Constants.SERVICE_AUTH_TOKEN;
import static org.jboss.as.security.Constants.SUBJECT_FACTORY_CLASS_NAME;
import static org.jboss.as.security.Constants.TRUSTSTORE_PASSWORD;
import static org.jboss.as.security.Constants.TRUSTSTORE_PROVIDER;
import static org.jboss.as.security.Constants.TRUSTSTORE_PROVIDER_ARGUMENT;
import static org.jboss.as.security.Constants.TRUSTSTORE_TYPE;
import static org.jboss.as.security.Constants.TRUSTSTORE_URL;
import static org.jboss.as.security.Constants.TRUST_MANAGER_FACTORY_ALGORITHM;
import static org.jboss.as.security.Constants.TRUST_MANAGER_FACTORY_PROVIDER;
import static org.jboss.as.security.Constants.VALUE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Subsystem descriptions for the security subsystem.
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author Brian Stansberry
 */
class SecuritySubsystemDescriptions {

    static final String RESOURCE_NAME = SecuritySubsystemDescriptions.class.getPackage().getName() + ".LocalDescriptions";

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return Descriptions.getSubsystem(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_ADD = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return Descriptions.getSubsystemAdd(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_DESCRIBE = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }
    };

    static final DescriptionProvider SECURITY_DOMAIN = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return Descriptions.getSecurityDomain(locale);
        }
    };

    static final DescriptionProvider SECURITY_DOMAIN_ADD = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return Descriptions.getSecurityDomainAdd(locale);
        }
    };

    static final DescriptionProvider SECURITY_DOMAIN_REMOVE = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return Descriptions.getSecurityDomainRemove(locale);
        }
    };

    static final DescriptionProvider SECURITY_PROPERTIES_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return Descriptions.getProperties(locale);
        }
    };

    static final DescriptionProvider SECURITY_PROPERTIES_ADD = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return Descriptions.getPropertiesAdd(locale);
        }
    };

    static final DescriptionProvider SECURITY_PROPERTIES_REMOVE = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return Descriptions.getPropertiesRemove(locale);
        }
    };

    static final DescriptionProvider LIST_CACHED_PRINCIPALS = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return Descriptions.getListCachedPrincipals(locale);
        }
    };

    static final DescriptionProvider FLUSH_CACHE = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return Descriptions.getFlushCache(locale);
        }
    };

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

    private static class Descriptions {
        static ModelNode getSubsystem(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode subsystem = new ModelNode();
            subsystem.get(DESCRIPTION).set(bundle.getString("security"));
            subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
            subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
            subsystem.get(NAMESPACE).set(Namespace.CURRENT.getUriString());

            subsystem.get(ATTRIBUTES, AUTHENTICATION_MANAGER_CLASS_NAME, DESCRIPTION).set(
                    bundle.getString("authentication-manager"));
            subsystem.get(ATTRIBUTES, AUTHENTICATION_MANAGER_CLASS_NAME, TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, AUTHENTICATION_MANAGER_CLASS_NAME, REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, AUTHENTICATION_MANAGER_CLASS_NAME, DEFAULT).set("default");

            subsystem.get(ATTRIBUTES, DEEP_COPY_SUBJECT_MODE, DESCRIPTION).set(bundle.getString("deep-copy-subject-mode"));
            subsystem.get(ATTRIBUTES, DEEP_COPY_SUBJECT_MODE, TYPE).set(ModelType.BOOLEAN);
            subsystem.get(ATTRIBUTES, DEEP_COPY_SUBJECT_MODE, REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, DEEP_COPY_SUBJECT_MODE, DEFAULT).set(false);

            subsystem.get(ATTRIBUTES, DEFAULT_CALLBACK_HANDLER_CLASS_NAME, DESCRIPTION).set(
                    bundle.getString("default-callback-handler-class-name"));
            subsystem.get(ATTRIBUTES, DEFAULT_CALLBACK_HANDLER_CLASS_NAME, TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, DEFAULT_CALLBACK_HANDLER_CLASS_NAME, REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, DEFAULT_CALLBACK_HANDLER_CLASS_NAME, DEFAULT).set("default");

            subsystem.get(ATTRIBUTES, SUBJECT_FACTORY_CLASS_NAME, DESCRIPTION).set(
                    bundle.getString("subject-factory-class-name"));
            subsystem.get(ATTRIBUTES, SUBJECT_FACTORY_CLASS_NAME, TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, SUBJECT_FACTORY_CLASS_NAME, REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, SUBJECT_FACTORY_CLASS_NAME, DEFAULT).set("default");

            subsystem.get(ATTRIBUTES, AUTHORIZATION_MANAGER_CLASS_NAME, DESCRIPTION).set(
                    bundle.getString("authorization-manager-class-name"));
            subsystem.get(ATTRIBUTES, AUTHORIZATION_MANAGER_CLASS_NAME, TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, AUTHORIZATION_MANAGER_CLASS_NAME, REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, AUTHORIZATION_MANAGER_CLASS_NAME, DEFAULT).set("default");

            subsystem.get(ATTRIBUTES, AUDIT_MANAGER_CLASS_NAME, DESCRIPTION).set(bundle.getString("audit-manager-class-name"));
            subsystem.get(ATTRIBUTES, AUDIT_MANAGER_CLASS_NAME, TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, AUDIT_MANAGER_CLASS_NAME, REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, AUDIT_MANAGER_CLASS_NAME, DEFAULT).set("default");

            subsystem.get(ATTRIBUTES, IDENTITY_TRUST_MANAGER_CLASS_NAME, DESCRIPTION).set(
                    bundle.getString("identity-trust-manager-class-name"));
            subsystem.get(ATTRIBUTES, IDENTITY_TRUST_MANAGER_CLASS_NAME, TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, IDENTITY_TRUST_MANAGER_CLASS_NAME, REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, IDENTITY_TRUST_MANAGER_CLASS_NAME, DEFAULT).set("default");

            subsystem.get(ATTRIBUTES, MAPPING_MANAGER_CLASS_NAME, DESCRIPTION).set(
                    bundle.getString("mapping-manager-class-name"));
            subsystem.get(ATTRIBUTES, MAPPING_MANAGER_CLASS_NAME, TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, MAPPING_MANAGER_CLASS_NAME, REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, MAPPING_MANAGER_CLASS_NAME, DEFAULT).set("default");

            subsystem.get(SECURITY_PROPERTIES).set(getProperties(locale));

            subsystem.get(CHILDREN, Constants.SECURITY_DOMAIN, DESCRIPTION).set(bundle.getString("security-domain-children"));
            subsystem.get(CHILDREN, Constants.SECURITY_DOMAIN, REQUIRED).set(false);
            subsystem.get(CHILDREN, Constants.SECURITY_DOMAIN, MIN_OCCURS).set(0);
            subsystem.get(CHILDREN, Constants.SECURITY_DOMAIN, MAX_OCCURS).set(Integer.MAX_VALUE);
            subsystem.get(CHILDREN, Constants.SECURITY_DOMAIN, MODEL_DESCRIPTION).setEmptyObject();

            subsystem.get(Constants.VAULT).set(getVault(locale));

            subsystem.get(CHILDREN, Constants.VAULT_OPTION, DESCRIPTION).set(bundle.getString("vault.option"));
            subsystem.get(CHILDREN, Constants.VAULT_OPTION, REQUIRED).set(false);
            subsystem.get(CHILDREN, Constants.VAULT_OPTION, MIN_OCCURS).set(0);
            subsystem.get(CHILDREN, Constants.VAULT_OPTION, MAX_OCCURS).set(Integer.MAX_VALUE);
            subsystem.get(CHILDREN, Constants.VAULT_OPTION, MODEL_DESCRIPTION).setEmptyObject();

            return subsystem;
        }

        static ModelNode getSubsystemAdd(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set(ADD);
            op.get(DESCRIPTION).set(bundle.getString("security.add"));

            op.get(REQUEST_PROPERTIES, AUTHENTICATION_MANAGER_CLASS_NAME, DESCRIPTION).set(
                    bundle.getString("authentication-manager"));
            op.get(REQUEST_PROPERTIES, AUTHENTICATION_MANAGER_CLASS_NAME, TYPE).set(ModelType.STRING);
            op.get(REQUEST_PROPERTIES, AUTHENTICATION_MANAGER_CLASS_NAME, REQUIRED).set(false);
            op.get(REQUEST_PROPERTIES, AUTHENTICATION_MANAGER_CLASS_NAME, DEFAULT).set("default");

            op.get(REQUEST_PROPERTIES, DEEP_COPY_SUBJECT_MODE, DESCRIPTION).set(bundle.getString("deep-copy-subject-mode"));
            op.get(REQUEST_PROPERTIES, DEEP_COPY_SUBJECT_MODE, TYPE).set(ModelType.BOOLEAN);
            op.get(REQUEST_PROPERTIES, DEEP_COPY_SUBJECT_MODE, REQUIRED).set(false);
            op.get(REQUEST_PROPERTIES, DEEP_COPY_SUBJECT_MODE, DEFAULT).set(false);

            op.get(REQUEST_PROPERTIES, DEFAULT_CALLBACK_HANDLER_CLASS_NAME, DESCRIPTION).set(
                    bundle.getString("default-callback-handler-class-name"));
            op.get(REQUEST_PROPERTIES, DEFAULT_CALLBACK_HANDLER_CLASS_NAME, TYPE).set(ModelType.STRING);
            op.get(REQUEST_PROPERTIES, DEFAULT_CALLBACK_HANDLER_CLASS_NAME, REQUIRED).set(false);
            op.get(REQUEST_PROPERTIES, DEFAULT_CALLBACK_HANDLER_CLASS_NAME, DEFAULT).set("default");

            op.get(REQUEST_PROPERTIES, SUBJECT_FACTORY_CLASS_NAME, DESCRIPTION).set(
                    bundle.getString("subject-factory-class-name"));
            op.get(REQUEST_PROPERTIES, SUBJECT_FACTORY_CLASS_NAME, TYPE).set(ModelType.STRING);
            op.get(REQUEST_PROPERTIES, SUBJECT_FACTORY_CLASS_NAME, REQUIRED).set(false);
            op.get(REQUEST_PROPERTIES, SUBJECT_FACTORY_CLASS_NAME, DEFAULT).set("default");

            op.get(REQUEST_PROPERTIES, AUTHORIZATION_MANAGER_CLASS_NAME, DESCRIPTION).set(
                    bundle.getString("authorization-manager-class-name"));
            op.get(REQUEST_PROPERTIES, AUTHORIZATION_MANAGER_CLASS_NAME, TYPE).set(ModelType.STRING);
            op.get(REQUEST_PROPERTIES, AUTHORIZATION_MANAGER_CLASS_NAME, REQUIRED).set(false);
            op.get(REQUEST_PROPERTIES, AUTHORIZATION_MANAGER_CLASS_NAME, DEFAULT).set("default");

            op.get(REQUEST_PROPERTIES, AUDIT_MANAGER_CLASS_NAME, DESCRIPTION).set(bundle.getString("audit-manager-class-name"));
            op.get(REQUEST_PROPERTIES, AUDIT_MANAGER_CLASS_NAME, TYPE).set(ModelType.STRING);
            op.get(REQUEST_PROPERTIES, AUDIT_MANAGER_CLASS_NAME, REQUIRED).set(false);
            op.get(REQUEST_PROPERTIES, AUDIT_MANAGER_CLASS_NAME, DEFAULT).set("default");

            op.get(REQUEST_PROPERTIES, IDENTITY_TRUST_MANAGER_CLASS_NAME, DESCRIPTION).set(
                    bundle.getString("identity-trust-manager-class-name"));
            op.get(REQUEST_PROPERTIES, IDENTITY_TRUST_MANAGER_CLASS_NAME, TYPE).set(ModelType.STRING);
            op.get(REQUEST_PROPERTIES, IDENTITY_TRUST_MANAGER_CLASS_NAME, REQUIRED).set(false);
            op.get(REQUEST_PROPERTIES, IDENTITY_TRUST_MANAGER_CLASS_NAME, DEFAULT).set("default");

            op.get(REQUEST_PROPERTIES, MAPPING_MANAGER_CLASS_NAME, DESCRIPTION).set(
                    bundle.getString("mapping-manager-class-name"));
            op.get(REQUEST_PROPERTIES, MAPPING_MANAGER_CLASS_NAME, TYPE).set(ModelType.STRING);
            op.get(REQUEST_PROPERTIES, MAPPING_MANAGER_CLASS_NAME, REQUIRED).set(false);
            op.get(REQUEST_PROPERTIES, MAPPING_MANAGER_CLASS_NAME, DEFAULT).set("default");

            op.get(REQUEST_PROPERTIES, SECURITY_PROPERTIES, DESCRIPTION).set(
                    bundle.getString("security-properties"));
            op.get(REQUEST_PROPERTIES, SECURITY_PROPERTIES, TYPE).set(ModelType.OBJECT);
            op.get(REQUEST_PROPERTIES, SECURITY_PROPERTIES, REQUIRED).set(false);

            op.get(SECURITY_PROPERTIES).set(getPropertiesAdd(locale));

            op.get(REQUEST_PROPERTIES, Constants.VAULT, DESCRIPTION).set(
                    bundle.getString("vault"));
            op.get(REQUEST_PROPERTIES, Constants.VAULT, TYPE).set(ModelType.OBJECT);
            op.get(REQUEST_PROPERTIES, Constants.VAULT, REQUIRED).set(false);

            op.get(REPLY_PROPERTIES).setEmptyObject();

            return op;
        }

        static ModelNode getSecurityDomain(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode op = new ModelNode();
            op.get(DESCRIPTION).set(bundle.getString("security-domain"));
            op.get(HEAD_COMMENT_ALLOWED).set(true);
            op.get(TAIL_COMMENT_ALLOWED).set(true);
            op.get(ATTRIBUTES, EXTENDS, DESCRIPTION).set(bundle.getString("extends"));
            op.get(ATTRIBUTES, EXTENDS, TYPE).set(ModelType.STRING);
            op.get(ATTRIBUTES, EXTENDS, REQUIRED).set(false);
            op.get(ATTRIBUTES, CACHE_TYPE, DESCRIPTION).set(bundle.getString("cache-type"));
            op.get(ATTRIBUTES, CACHE_TYPE, TYPE).set(ModelType.STRING);
            op.get(ATTRIBUTES, CACHE_TYPE, REQUIRED).set(false);
            op.get(CHILDREN, AUTHENTICATION).set(getAuthentication(locale));
            op.get(CHILDREN, AUTHENTICATION_JASPI).set(getAuthenticationJaspi(locale));
            op.get(CHILDREN, AUTHORIZATION).set(getAuthorization(locale));
            op.get(CHILDREN, ACL).set(getAcl(locale));
            op.get(CHILDREN, AUDIT).set(getAudit(locale));
            op.get(CHILDREN, IDENTITY_TRUST).set(getIdentityTrust(locale));
            op.get(CHILDREN, MAPPING).set(getMapping(locale));
            op.get(CHILDREN, JSSE).set(getJSSE(locale));

            return op;
        }

        static ModelNode getSecurityDomainAdd(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set(ADD);
            op.get(DESCRIPTION).set(bundle.getString("security-domain.add"));
            op.get(REQUEST_PROPERTIES, EXTENDS, DESCRIPTION).set(bundle.getString("extends"));
            op.get(REQUEST_PROPERTIES, EXTENDS, TYPE).set(ModelType.STRING);
            op.get(REQUEST_PROPERTIES, EXTENDS, REQUIRED).set(false);
            op.get(REQUEST_PROPERTIES, CACHE_TYPE, DESCRIPTION).set(bundle.getString("cache-type"));
            op.get(REQUEST_PROPERTIES, CACHE_TYPE, TYPE).set(ModelType.STRING);
            op.get(REQUEST_PROPERTIES, CACHE_TYPE, REQUIRED).set(false);

            op.get(REQUEST_PROPERTIES, AUTHENTICATION, DESCRIPTION).set(bundle.getString("authentication"));
            op.get(REQUEST_PROPERTIES, AUTHENTICATION, TYPE).set(ModelType.OBJECT);
            op.get(REQUEST_PROPERTIES, AUTHENTICATION, REQUIRED).set(false);

            op.get(REQUEST_PROPERTIES, AUTHENTICATION_JASPI, DESCRIPTION).set(bundle.getString("authentication-jaspi"));
            op.get(REQUEST_PROPERTIES, AUTHENTICATION_JASPI, TYPE).set(ModelType.OBJECT);
            op.get(REQUEST_PROPERTIES, AUTHENTICATION_JASPI, REQUIRED).set(false);

            op.get(REQUEST_PROPERTIES, AUTHORIZATION, DESCRIPTION).set(bundle.getString("authorization"));
            op.get(REQUEST_PROPERTIES, AUTHORIZATION, TYPE).set(ModelType.OBJECT);
            op.get(REQUEST_PROPERTIES, AUTHORIZATION, REQUIRED).set(false);

            op.get(REQUEST_PROPERTIES, ACL, DESCRIPTION).set(bundle.getString("acl"));
            op.get(REQUEST_PROPERTIES, ACL, TYPE).set(ModelType.OBJECT);
            op.get(REQUEST_PROPERTIES, ACL, REQUIRED).set(false);

            op.get(REQUEST_PROPERTIES, AUDIT, DESCRIPTION).set(bundle.getString("audit"));
            op.get(REQUEST_PROPERTIES, AUDIT, TYPE).set(ModelType.OBJECT);
            op.get(REQUEST_PROPERTIES, AUDIT, REQUIRED).set(false);

            op.get(REQUEST_PROPERTIES, IDENTITY_TRUST, DESCRIPTION).set(bundle.getString("identity-trust"));
            op.get(REQUEST_PROPERTIES, IDENTITY_TRUST, TYPE).set(ModelType.OBJECT);
            op.get(REQUEST_PROPERTIES, IDENTITY_TRUST, REQUIRED).set(false);

            op.get(REQUEST_PROPERTIES, MAPPING, DESCRIPTION).set(bundle.getString("mapping"));
            op.get(REQUEST_PROPERTIES, MAPPING, TYPE).set(ModelType.OBJECT);
            op.get(REQUEST_PROPERTIES, MAPPING, REQUIRED).set(false);

            op.get(REQUEST_PROPERTIES, JSSE, DESCRIPTION).set(bundle.getString("jsse"));
            op.get(REQUEST_PROPERTIES, JSSE, TYPE).set(ModelType.OBJECT);
            op.get(REQUEST_PROPERTIES, JSSE, REQUIRED).set(false);

           /* op.get(CHILDREN, AUTHENTICATION).set(getAuthenticationAdd(locale));
            op.get(CHILDREN, AUTHENTICATION_JASPI).set(getAuthenticationJaspiAdd(locale));
            op.get(CHILDREN, AUTHORIZATION).set(getAuthorizationAdd(locale));
            op.get(CHILDREN, ACL).set(getAclAdd(locale));
            op.get(CHILDREN, AUDIT).set(getAuditAdd(locale));
            op.get(CHILDREN, IDENTITY_TRUST).set(getIdentityTrustAdd(locale));
            op.get(CHILDREN, MAPPING).set(getMappingAdd(locale));
            op.get(CHILDREN, JSSE).set(getJSSEAdd(locale));*/
            return op;
        }

        static ModelNode getSecurityDomainRemove(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set(REMOVE);
            op.get(DESCRIPTION).set(bundle.getString("security-domain.remove"));

            return op;
        }

        static ModelNode getModuleOptions(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(TYPE).set(ModelType.LIST);
            node.get(DESCRIPTION).set(bundle.getString("module-options"));
            node.get(REQUIRED).set(false);
            node.get(ATTRIBUTES, NAME, DESCRIPTION).set(bundle.getString("module-options.name"));
            node.get(ATTRIBUTES, NAME, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, NAME, REQUIRED).set(true);
            node.get(ATTRIBUTES, VALUE, DESCRIPTION).set(bundle.getString("module-options.value"));
            node.get(ATTRIBUTES, VALUE, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, VALUE, REQUIRED).set(true);

            return node;
        }

        static ModelNode getModuleOptionsAdd(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(TYPE).set(ModelType.LIST);
            node.get(DESCRIPTION).set(bundle.getString("module-options"));
            node.get(REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, NAME, DESCRIPTION).set(bundle.getString("module-options.name"));
            node.get(REQUEST_PROPERTIES, NAME, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, NAME, REQUIRED).set(true);
            node.get(REQUEST_PROPERTIES, VALUE, DESCRIPTION).set(bundle.getString("module-options.value"));
            node.get(REQUEST_PROPERTIES, VALUE, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, VALUE, REQUIRED).set(true);

            return node;
        }

        static ModelNode getProperties(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(TYPE).set(ModelType.LIST);
            node.get(DESCRIPTION).set(bundle.getString("security-properties"));
            node.get(REQUIRED).set(false);
            node.get(ATTRIBUTES, NAME, DESCRIPTION).set(bundle.getString("security-properties.name"));
            node.get(ATTRIBUTES, NAME, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, NAME, REQUIRED).set(true);
            node.get(ATTRIBUTES, VALUE, DESCRIPTION).set(bundle.getString("security-properties.value"));
            node.get(ATTRIBUTES, VALUE, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, VALUE, REQUIRED).set(true);

            return node;
        }

        static ModelNode getPropertiesAdd(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(TYPE).set(ModelType.LIST);
            node.get(DESCRIPTION).set(bundle.getString("security-properties"));
            node.get(REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, NAME, DESCRIPTION).set(bundle.getString("security-properties.name"));
            node.get(REQUEST_PROPERTIES, NAME, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, NAME, REQUIRED).set(true);
            node.get(REQUEST_PROPERTIES, VALUE, DESCRIPTION).set(bundle.getString("security-properties.value"));
            node.get(REQUEST_PROPERTIES, VALUE, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, VALUE, REQUIRED).set(true);

            return node;
        }

        static ModelNode getPropertiesRemove(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set(REMOVE);
            op.get(DESCRIPTION).set(bundle.getString("security-properties.remove"));

            return op;
        }

        static ModelNode getVault(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(TYPE).set(ModelType.LIST);
            node.get(DESCRIPTION).set(bundle.getString("vault"));
            node.get(REQUIRED).set(false);
            node.get(ATTRIBUTES, CODE, DESCRIPTION).set(bundle.getString("vault.code"));
            node.get(ATTRIBUTES, CODE, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, NAME, REQUIRED).set(false);
            return node;
        }

        static ModelNode getAuthentication(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(TYPE).set(ModelType.LIST);
            node.get(DESCRIPTION).set(bundle.getString("authentication"));
            node.get(REQUIRED).set(false);
            node.get(ATTRIBUTES, CODE, DESCRIPTION).set(bundle.getString("code"));
            node.get(ATTRIBUTES, CODE, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, CODE, REQUIRED).set(true);
            node.get(ATTRIBUTES, FLAG, DESCRIPTION).set(bundle.getString("flag"));
            node.get(ATTRIBUTES, FLAG, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, FLAG, REQUIRED).set(true);
            node.get(MODULE_OPTIONS).set(getModuleOptions(locale));

            return node;
        }

        static ModelNode getAuthenticationAdd(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(TYPE).set(ModelType.LIST);
            node.get(DESCRIPTION).set(bundle.getString("authentication"));
            node.get(REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, CODE, DESCRIPTION).set(bundle.getString("code"));
            node.get(REQUEST_PROPERTIES, CODE, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, CODE, REQUIRED).set(true);
            node.get(REQUEST_PROPERTIES, FLAG, DESCRIPTION).set(bundle.getString("flag"));
            node.get(REQUEST_PROPERTIES, FLAG, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, FLAG, REQUIRED).set(true);
            node.get(MODULE_OPTIONS).set(getModuleOptionsAdd(locale));

            return node;
        }

        static ModelNode getAuthenticationJaspi(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(TYPE).set(ModelType.LIST);
            node.get(DESCRIPTION).set(bundle.getString("authentication-jaspi"));
            node.get(REQUIRED).set(false);
            node.get(LOGIN_MODULE_STACK).set(getLoginModuleStack(locale));
            node.get(AUTH_MODULE).set(getAuthModule(locale));

            return node;
        }

        static ModelNode getAuthenticationJaspiAdd(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(TYPE).set(ModelType.LIST);
            node.get(DESCRIPTION).set(bundle.getString("authentication-jaspi"));
            node.get(REQUIRED).set(false);
            node.get(LOGIN_MODULE_STACK).set(getLoginModuleStackAdd(locale));
            node.get(AUTH_MODULE).set(getAuthModuleAdd(locale));

            return node;
        }

        static ModelNode getLoginModuleStack(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(TYPE).set(ModelType.OBJECT);
            node.get(DESCRIPTION).set(bundle.getString("authentication-jaspi.login-module-stack"));
            node.get(REQUIRED).set(false);
            node.get(ATTRIBUTES, NAME, DESCRIPTION).set(bundle.getString("authentication-jaspi.login-module-stack.name"));
            node.get(ATTRIBUTES, NAME, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, NAME, REQUIRED).set(true);
            node.get(ATTRIBUTES, CODE, DESCRIPTION).set(bundle.getString("code"));
            node.get(ATTRIBUTES, CODE, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, CODE, REQUIRED).set(true);
            node.get(ATTRIBUTES, FLAG, DESCRIPTION).set(bundle.getString("flag"));
            node.get(ATTRIBUTES, FLAG, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, FLAG, REQUIRED).set(true);
            node.get(MODULE_OPTIONS).set(getModuleOptions(locale));

            return node;
        }

        static ModelNode getLoginModuleStackAdd(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(TYPE).set(ModelType.OBJECT);
            node.get(DESCRIPTION).set(bundle.getString("authentication-jaspi.login-module-stack"));
            node.get(REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, NAME, DESCRIPTION).set(
                    bundle.getString("authentication-jaspi.login-module-stack.name"));
            node.get(REQUEST_PROPERTIES, NAME, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, NAME, REQUIRED).set(true);
            node.get(REQUEST_PROPERTIES, CODE, DESCRIPTION).set(bundle.getString("code"));
            node.get(REQUEST_PROPERTIES, CODE, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, CODE, REQUIRED).set(true);
            node.get(REQUEST_PROPERTIES, FLAG, DESCRIPTION).set(bundle.getString("flag"));
            node.get(REQUEST_PROPERTIES, FLAG, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, FLAG, REQUIRED).set(true);
            node.get(MODULE_OPTIONS).set(getModuleOptionsAdd(locale));

            return node;
        }

        static ModelNode getAuthModule(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(TYPE).set(ModelType.OBJECT);
            node.get(DESCRIPTION).set(bundle.getString("authentication-jaspi.auth-module"));
            node.get(REQUIRED).set(false);
            node.get(ATTRIBUTES, CODE, DESCRIPTION).set(bundle.getString("code"));
            node.get(ATTRIBUTES, CODE, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, CODE, REQUIRED).set(true);
            node.get(ATTRIBUTES, LOGIN_MODULE_STACK_REF, DESCRIPTION).set(bundle.getString("login-module-stack-ref"));
            node.get(ATTRIBUTES, LOGIN_MODULE_STACK_REF, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, LOGIN_MODULE_STACK_REF, REQUIRED).set(false);
            node.get(MODULE_OPTIONS).set(getModuleOptions(locale));

            return node;
        }

        static ModelNode getAuthModuleAdd(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(TYPE).set(ModelType.OBJECT);
            node.get(DESCRIPTION).set(bundle.getString("authentication-jaspi.auth-module"));
            node.get(REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, CODE, DESCRIPTION).set(bundle.getString("code"));
            node.get(REQUEST_PROPERTIES, CODE, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, CODE, REQUIRED).set(true);
            node.get(REQUEST_PROPERTIES, LOGIN_MODULE_STACK_REF, DESCRIPTION).set(bundle.getString("login-module-stack-ref"));
            node.get(REQUEST_PROPERTIES, LOGIN_MODULE_STACK_REF, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, LOGIN_MODULE_STACK_REF, REQUIRED).set(false);
            node.get(MODULE_OPTIONS).set(getModuleOptionsAdd(locale));

            return node;
        }

        static ModelNode getAuthorization(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(TYPE).set(ModelType.LIST);
            node.get(DESCRIPTION).set(bundle.getString("authorization"));
            node.get(REQUIRED).set(false);
            node.get(ATTRIBUTES, CODE, DESCRIPTION).set(bundle.getString("code"));
            node.get(ATTRIBUTES, CODE, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, CODE, REQUIRED).set(true);
            node.get(ATTRIBUTES, FLAG, DESCRIPTION).set(bundle.getString("flag"));
            node.get(ATTRIBUTES, FLAG, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, FLAG, REQUIRED).set(true);
            node.get(MODULE_OPTIONS).set(getModuleOptions(locale));

            return node;
        }

        static ModelNode getAuthorizationAdd(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(TYPE).set(ModelType.LIST);
            node.get(DESCRIPTION).set(bundle.getString("authorization"));
            node.get(REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, CODE, DESCRIPTION).set(bundle.getString("code"));
            node.get(REQUEST_PROPERTIES, CODE, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, CODE, REQUIRED).set(true);
            node.get(REQUEST_PROPERTIES, FLAG, DESCRIPTION).set(bundle.getString("flag"));
            node.get(REQUEST_PROPERTIES, FLAG, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, FLAG, REQUIRED).set(true);
            node.get(MODULE_OPTIONS).set(getModuleOptionsAdd(locale));

            return node;
        }

        static ModelNode getAcl(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(TYPE).set(ModelType.LIST);
            node.get(DESCRIPTION).set(bundle.getString("acl"));
            node.get(REQUIRED).set(false);
            node.get(ATTRIBUTES, CODE, DESCRIPTION).set(bundle.getString("code"));
            node.get(ATTRIBUTES, CODE, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, CODE, REQUIRED).set(true);
            node.get(ATTRIBUTES, FLAG, DESCRIPTION).set(bundle.getString("flag"));
            node.get(ATTRIBUTES, FLAG, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, FLAG, REQUIRED).set(true);
            node.get(MODULE_OPTIONS).set(getModuleOptions(locale));

            return node;
        }

        static ModelNode getAclAdd(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(TYPE).set(ModelType.LIST);
            node.get(DESCRIPTION).set(bundle.getString("acl"));
            node.get(REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, CODE, DESCRIPTION).set(bundle.getString("code"));
            node.get(REQUEST_PROPERTIES, CODE, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, CODE, REQUIRED).set(true);
            node.get(REQUEST_PROPERTIES, FLAG, DESCRIPTION).set(bundle.getString("flag"));
            node.get(REQUEST_PROPERTIES, FLAG, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, FLAG, REQUIRED).set(true);
            node.get(MODULE_OPTIONS).set(getModuleOptionsAdd(locale));

            return node;
        }

        static ModelNode getAudit(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(TYPE).set(ModelType.LIST);
            node.get(DESCRIPTION).set(bundle.getString("audit"));
            node.get(REQUIRED).set(false);
            node.get(ATTRIBUTES, CODE, DESCRIPTION).set(bundle.getString("code"));
            node.get(ATTRIBUTES, CODE, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, CODE, REQUIRED).set(true);
            node.get(MODULE_OPTIONS).set(getModuleOptions(locale));

            return node;
        }

        static ModelNode getAuditAdd(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(TYPE).set(ModelType.LIST);
            node.get(DESCRIPTION).set(bundle.getString("audit"));
            node.get(REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, CODE, DESCRIPTION).set(bundle.getString("code"));
            node.get(REQUEST_PROPERTIES, CODE, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, CODE, REQUIRED).set(true);
            node.get(MODULE_OPTIONS).set(getModuleOptionsAdd(locale));

            return node;
        }

        static ModelNode getIdentityTrust(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(TYPE).set(ModelType.LIST);
            node.get(DESCRIPTION).set(bundle.getString("identity-trust"));
            node.get(REQUIRED).set(false);
            node.get(ATTRIBUTES, CODE, DESCRIPTION).set(bundle.getString("code"));
            node.get(ATTRIBUTES, CODE, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, CODE, REQUIRED).set(true);
            node.get(ATTRIBUTES, FLAG, DESCRIPTION).set(bundle.getString("flag"));
            node.get(ATTRIBUTES, FLAG, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, FLAG, REQUIRED).set(true);
            node.get(MODULE_OPTIONS).set(getModuleOptions(locale));

            return node;
        }

        static ModelNode getIdentityTrustAdd(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(TYPE).set(ModelType.LIST);
            node.get(DESCRIPTION).set(bundle.getString("identity-trust"));
            node.get(REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, CODE, DESCRIPTION).set(bundle.getString("code"));
            node.get(REQUEST_PROPERTIES, CODE, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, CODE, REQUIRED).set(true);
            node.get(REQUEST_PROPERTIES, FLAG, DESCRIPTION).set(bundle.getString("flag"));
            node.get(REQUEST_PROPERTIES, FLAG, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, FLAG, REQUIRED).set(true);
            node.get(MODULE_OPTIONS).set(getModuleOptionsAdd(locale));

            return node;
        }

        static ModelNode getMapping(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(TYPE).set(ModelType.LIST);
            node.get(DESCRIPTION).set(bundle.getString("mapping"));
            node.get(REQUIRED).set(false);
            node.get(ATTRIBUTES, CODE, DESCRIPTION).set(bundle.getString("code"));
            node.get(ATTRIBUTES, CODE, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, CODE, REQUIRED).set(true);
            node.get(ATTRIBUTES, TYPE, DESCRIPTION).set(bundle.getString("type"));
            node.get(ATTRIBUTES, TYPE, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, TYPE, REQUIRED).set(false);
            node.get(MODULE_OPTIONS).set(getModuleOptions(locale));

            return node;
        }

        static ModelNode getMappingAdd(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(TYPE).set(ModelType.LIST);
            node.get(DESCRIPTION).set(bundle.getString("mapping"));
            node.get(REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, CODE, DESCRIPTION).set(bundle.getString("code"));
            node.get(REQUEST_PROPERTIES, CODE, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, CODE, REQUIRED).set(true);
            node.get(REQUEST_PROPERTIES, TYPE, DESCRIPTION).set(bundle.getString("type"));
            node.get(REQUEST_PROPERTIES, TYPE, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, TYPE, REQUIRED).set(false);
            node.get(MODULE_OPTIONS).set(getModuleOptionsAdd(locale));

            return node;
        }

        static ModelNode getJSSE(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(TYPE).set(ModelType.OBJECT);
            node.get(DESCRIPTION).set(bundle.getString("jsse"));
            node.get(REQUIRED).set(false);
            node.get(ATTRIBUTES, KEYSTORE_PASSWORD, DESCRIPTION).set(bundle.getString("keystore-password"));
            node.get(ATTRIBUTES, KEYSTORE_PASSWORD, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, KEYSTORE_PASSWORD, REQUIRED).set(false);
            node.get(ATTRIBUTES, KEYSTORE_TYPE, DESCRIPTION).set(bundle.getString("keystore-type"));
            node.get(ATTRIBUTES, KEYSTORE_TYPE, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, KEYSTORE_TYPE, REQUIRED).set(false);
            node.get(ATTRIBUTES, KEYSTORE_URL, DESCRIPTION).set(bundle.getString("keystore-url"));
            node.get(ATTRIBUTES, KEYSTORE_URL, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, KEYSTORE_URL, REQUIRED).set(false);
            node.get(ATTRIBUTES, KEYSTORE_PROVIDER, DESCRIPTION).set(bundle.getString("keystore-provider"));
            node.get(ATTRIBUTES, KEYSTORE_PROVIDER, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, KEYSTORE_PROVIDER, REQUIRED).set(false);
            node.get(ATTRIBUTES, KEYSTORE_PROVIDER_ARGUMENT, DESCRIPTION).set(bundle.getString("keystore-provider-argument"));
            node.get(ATTRIBUTES, KEYSTORE_PROVIDER_ARGUMENT, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, KEYSTORE_PROVIDER_ARGUMENT, REQUIRED).set(false);
            node.get(ATTRIBUTES, KEY_MANAGER_FACTORY_PROVIDER, DESCRIPTION).set(
                    bundle.getString("key-manager-factory-provider"));
            node.get(ATTRIBUTES, KEY_MANAGER_FACTORY_PROVIDER, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, KEY_MANAGER_FACTORY_PROVIDER, REQUIRED).set(false);
            node.get(ATTRIBUTES, KEY_MANAGER_FACTORY_ALGORITHM, DESCRIPTION).set(
                    bundle.getString("key-manager-factory-algorithm"));
            node.get(ATTRIBUTES, KEY_MANAGER_FACTORY_ALGORITHM, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, KEY_MANAGER_FACTORY_ALGORITHM, REQUIRED).set(false);
            node.get(ATTRIBUTES, TRUSTSTORE_PASSWORD, DESCRIPTION).set(bundle.getString("truststore-password"));
            node.get(ATTRIBUTES, TRUSTSTORE_PASSWORD, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, TRUSTSTORE_PASSWORD, REQUIRED).set(false);
            node.get(ATTRIBUTES, TRUSTSTORE_TYPE, DESCRIPTION).set(bundle.getString("truststore-type"));
            node.get(ATTRIBUTES, TRUSTSTORE_TYPE, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, TRUSTSTORE_TYPE, REQUIRED).set(false);
            node.get(ATTRIBUTES, TRUSTSTORE_URL, DESCRIPTION).set(bundle.getString("truststore-url"));
            node.get(ATTRIBUTES, TRUSTSTORE_URL, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, TRUSTSTORE_URL, REQUIRED).set(false);
            node.get(ATTRIBUTES, TRUSTSTORE_PROVIDER, DESCRIPTION).set(bundle.getString("truststore-provider"));
            node.get(ATTRIBUTES, TRUSTSTORE_PROVIDER, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, TRUSTSTORE_PROVIDER, REQUIRED).set(false);
            node.get(ATTRIBUTES, TRUSTSTORE_PROVIDER_ARGUMENT, DESCRIPTION).set(
                    bundle.getString("truststore-provider-argument"));
            node.get(ATTRIBUTES, TRUSTSTORE_PROVIDER_ARGUMENT, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, TRUSTSTORE_PROVIDER_ARGUMENT, REQUIRED).set(false);
            node.get(ATTRIBUTES, TRUST_MANAGER_FACTORY_PROVIDER, DESCRIPTION).set(
                    bundle.getString("trust-manager-factory-provider"));
            node.get(ATTRIBUTES, TRUST_MANAGER_FACTORY_PROVIDER, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, TRUST_MANAGER_FACTORY_PROVIDER, REQUIRED).set(false);
            node.get(ATTRIBUTES, TRUST_MANAGER_FACTORY_ALGORITHM, DESCRIPTION).set(
                    bundle.getString("trust-manager-factory-algorithm"));
            node.get(ATTRIBUTES, TRUST_MANAGER_FACTORY_ALGORITHM, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, TRUST_MANAGER_FACTORY_ALGORITHM, REQUIRED).set(false);
            node.get(ATTRIBUTES, CLIENT_ALIAS, DESCRIPTION).set(bundle.getString("client-alias"));
            node.get(ATTRIBUTES, CLIENT_ALIAS, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, CLIENT_ALIAS, REQUIRED).set(false);
            node.get(ATTRIBUTES, SERVER_ALIAS, DESCRIPTION).set(bundle.getString("server-alias"));
            node.get(ATTRIBUTES, SERVER_ALIAS, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, SERVER_ALIAS, REQUIRED).set(false);
            node.get(ATTRIBUTES, CLIENT_AUTH, DESCRIPTION).set(bundle.getString("client-auth"));
            node.get(ATTRIBUTES, CLIENT_AUTH, TYPE).set(ModelType.BOOLEAN);
            node.get(ATTRIBUTES, CLIENT_AUTH, REQUIRED).set(false);
            node.get(ATTRIBUTES, SERVICE_AUTH_TOKEN, DESCRIPTION).set(bundle.getString("service-auth-token"));
            node.get(ATTRIBUTES, SERVICE_AUTH_TOKEN, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, SERVICE_AUTH_TOKEN, REQUIRED).set(false);
            node.get(ATTRIBUTES, CIPHER_SUITES, DESCRIPTION).set(bundle.getString("cipher-suites"));
            node.get(ATTRIBUTES, CIPHER_SUITES, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, CIPHER_SUITES, REQUIRED).set(false);
            node.get(ATTRIBUTES, PROTOCOLS, DESCRIPTION).set(bundle.getString("protocols"));
            node.get(ATTRIBUTES, PROTOCOLS, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, PROTOCOLS, REQUIRED).set(false);
            node.get(ATTRIBUTES, ADDITIONAL_PROPERTIES, DESCRIPTION).set(bundle.getString("additional-properties"));
            node.get(ATTRIBUTES, ADDITIONAL_PROPERTIES, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, ADDITIONAL_PROPERTIES, REQUIRED).set(false);

            return node;
        }

        static ModelNode getJSSEAdd(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(TYPE).set(ModelType.OBJECT);
            node.get(DESCRIPTION).set(bundle.getString("jsse"));
            node.get(REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, KEYSTORE_PASSWORD, DESCRIPTION).set(bundle.getString("keystore-password"));
            node.get(REQUEST_PROPERTIES, KEYSTORE_PASSWORD, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, KEYSTORE_PASSWORD, REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, KEYSTORE_TYPE, DESCRIPTION).set(bundle.getString("keystore-type"));
            node.get(REQUEST_PROPERTIES, KEYSTORE_TYPE, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, KEYSTORE_TYPE, REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, KEYSTORE_URL, DESCRIPTION).set(bundle.getString("keystore-url"));
            node.get(REQUEST_PROPERTIES, KEYSTORE_URL, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, KEYSTORE_URL, REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, KEYSTORE_PROVIDER, DESCRIPTION).set(bundle.getString("keystore-provider"));
            node.get(REQUEST_PROPERTIES, KEYSTORE_PROVIDER, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, KEYSTORE_PROVIDER, REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, KEYSTORE_PROVIDER_ARGUMENT, DESCRIPTION).set(
                    bundle.getString("keystore-provider-argument"));
            node.get(REQUEST_PROPERTIES, KEYSTORE_PROVIDER_ARGUMENT, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, KEYSTORE_PROVIDER_ARGUMENT, REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, KEY_MANAGER_FACTORY_PROVIDER, DESCRIPTION).set(
                    bundle.getString("key-manager-factory-provider"));
            node.get(REQUEST_PROPERTIES, KEY_MANAGER_FACTORY_PROVIDER, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, KEY_MANAGER_FACTORY_PROVIDER, REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, KEY_MANAGER_FACTORY_ALGORITHM, DESCRIPTION).set(
                    bundle.getString("key-manager-factory-algorithm"));
            node.get(REQUEST_PROPERTIES, KEY_MANAGER_FACTORY_ALGORITHM, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, KEY_MANAGER_FACTORY_ALGORITHM, REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, TRUSTSTORE_PASSWORD, DESCRIPTION).set(bundle.getString("truststore-password"));
            node.get(REQUEST_PROPERTIES, TRUSTSTORE_PASSWORD, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, TRUSTSTORE_PASSWORD, REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, TRUSTSTORE_TYPE, DESCRIPTION).set(bundle.getString("truststore-type"));
            node.get(REQUEST_PROPERTIES, TRUSTSTORE_TYPE, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, TRUSTSTORE_TYPE, REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, TRUSTSTORE_URL, DESCRIPTION).set(bundle.getString("truststore-url"));
            node.get(REQUEST_PROPERTIES, TRUSTSTORE_URL, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, TRUSTSTORE_URL, REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, TRUSTSTORE_PROVIDER, DESCRIPTION).set(bundle.getString("truststore-provider"));
            node.get(REQUEST_PROPERTIES, TRUSTSTORE_PROVIDER, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, TRUSTSTORE_PROVIDER, REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, TRUSTSTORE_PROVIDER_ARGUMENT, DESCRIPTION).set(
                    bundle.getString("truststore-provider-argument"));
            node.get(REQUEST_PROPERTIES, TRUSTSTORE_PROVIDER_ARGUMENT, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, TRUSTSTORE_PROVIDER_ARGUMENT, REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, TRUST_MANAGER_FACTORY_PROVIDER, DESCRIPTION).set(
                    bundle.getString("trust-manager-factory-provider"));
            node.get(REQUEST_PROPERTIES, TRUST_MANAGER_FACTORY_PROVIDER, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, TRUST_MANAGER_FACTORY_PROVIDER, REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, TRUST_MANAGER_FACTORY_ALGORITHM, DESCRIPTION).set(
                    bundle.getString("trust-manager-factory-algorithm"));
            node.get(REQUEST_PROPERTIES, TRUST_MANAGER_FACTORY_ALGORITHM, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, TRUST_MANAGER_FACTORY_ALGORITHM, REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, CLIENT_ALIAS, DESCRIPTION).set(bundle.getString("client-alias"));
            node.get(REQUEST_PROPERTIES, CLIENT_ALIAS, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, CLIENT_ALIAS, REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, SERVER_ALIAS, DESCRIPTION).set(bundle.getString("server-alias"));
            node.get(REQUEST_PROPERTIES, SERVER_ALIAS, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, SERVER_ALIAS, REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, CLIENT_AUTH, DESCRIPTION).set(bundle.getString("client-auth"));
            node.get(REQUEST_PROPERTIES, CLIENT_AUTH, TYPE).set(ModelType.BOOLEAN);
            node.get(REQUEST_PROPERTIES, CLIENT_AUTH, REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, SERVICE_AUTH_TOKEN, DESCRIPTION).set(bundle.getString("service-auth-token"));
            node.get(REQUEST_PROPERTIES, SERVICE_AUTH_TOKEN, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, SERVICE_AUTH_TOKEN, REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, CIPHER_SUITES, DESCRIPTION).set(bundle.getString("cipher-suites"));
            node.get(REQUEST_PROPERTIES, CIPHER_SUITES, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, CIPHER_SUITES, REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, PROTOCOLS, DESCRIPTION).set(bundle.getString("protocols"));
            node.get(REQUEST_PROPERTIES, PROTOCOLS, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, PROTOCOLS, REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, ADDITIONAL_PROPERTIES, DESCRIPTION).set(bundle.getString("additional-properties"));
            node.get(REQUEST_PROPERTIES, ADDITIONAL_PROPERTIES, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, ADDITIONAL_PROPERTIES, REQUIRED).set(false);

            return node;
        }

        static ModelNode getListCachedPrincipals(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set(Constants.LIST_CACHED_PRINCIPALS);
            op.get(DESCRIPTION).set(bundle.getString("list-cached-principals"));

            op.get(REPLY_PROPERTIES, DESCRIPTION).set("list-cached-principals.return");
            op.get(REPLY_PROPERTIES, TYPE).set(ModelType.LIST);

            return op;
        }

        static ModelNode getFlushCache(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set(Constants.FLUSH_CACHE);
            op.get(DESCRIPTION).set(bundle.getString("flush-cache"));

            op.get(REQUEST_PROPERTIES, Constants.PRINCIPAL_ARGUMENT, DESCRIPTION).set(bundle.getString("principal"));
            op.get(REQUEST_PROPERTIES, Constants.PRINCIPAL_ARGUMENT, TYPE).set(ModelType.STRING);
            op.get(REQUEST_PROPERTIES, Constants.PRINCIPAL_ARGUMENT, REQUIRED).set(false);


            return op;
        }

    }

}
