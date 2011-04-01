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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.security.CommonAttributes.AUDIT_MANAGER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.AUTHENTICATION_MANAGER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.AUTHORIZATION_MANAGER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.DEEP_COPY_SUBJECT_MODE;
import static org.jboss.as.security.CommonAttributes.DEFAULT_CALLBACK_HANDLER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.IDENTITY_TRUST_MANAGER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.MAPPING_MANAGER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.MODULE_OPTIONS;
import static org.jboss.as.security.CommonAttributes.SUBJECT_FACTORY_CLASS_NAME;

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

        public ModelNode getModelDescription(Locale locale) {
            return Descriptions.getSubsystem(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_ADD = new DescriptionProvider() {

        public ModelNode getModelDescription(Locale locale) {
            return Descriptions.getSubsystemAdd(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_DESCRIBE = new DescriptionProvider() {

        public ModelNode getModelDescription(Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }
    };

    static final DescriptionProvider SECURITY_DOMAIN = new DescriptionProvider() {

        public ModelNode getModelDescription(Locale locale) {
            return Descriptions.getSecurityDomain(locale);
        }
    };

    static final DescriptionProvider SECURITY_DOMAIN_ADD = new DescriptionProvider() {

        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode subsystem = new ModelNode();

            return subsystem;
        }
    };

    static final DescriptionProvider SECURITY_DOMAIN_REMOVE = new DescriptionProvider() {

        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode subsystem = new ModelNode();

            return subsystem;
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
            subsystem.get(NAMESPACE).set(Namespace.SECURITY_1_0.getUriString());

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

            subsystem.get(CHILDREN, CommonAttributes.SECURITY_DOMAIN, DESCRIPTION).set(
                    bundle.getString("security-domain-children"));
            subsystem.get(CHILDREN, CommonAttributes.SECURITY_DOMAIN, REQUIRED).set(false);
            subsystem.get(CHILDREN, CommonAttributes.SECURITY_DOMAIN, MIN_OCCURS).set(0);
            subsystem.get(CHILDREN, CommonAttributes.SECURITY_DOMAIN, MAX_OCCURS).set(Integer.MAX_VALUE);
            subsystem.get(CHILDREN, CommonAttributes.SECURITY_DOMAIN, MODEL_DESCRIPTION).setEmptyObject();

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

            op.get(REPLY_PROPERTIES).setEmptyObject();

            return op;
        }

        static ModelNode getSecurityDomain(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode op = new ModelNode();
            op.get(DESCRIPTION).set(bundle.getString("security-domain"));
            op.get(HEAD_COMMENT_ALLOWED).set(true);
            op.get(TAIL_COMMENT_ALLOWED).set(true);

            op.get(ATTRIBUTES, Attribute.EXTENDS.getLocalName(), DESCRIPTION).set(bundle.getString("extends"));
            op.get(ATTRIBUTES, Attribute.EXTENDS.getLocalName(), TYPE).set(ModelType.STRING);
            op.get(ATTRIBUTES, Attribute.EXTENDS.getLocalName(), REQUIRED).set(false);

            op.get(CHILDREN, Element.AUTHENTICATION.getLocalName(), DESCRIPTION).set(bundle.getString("authentication"));
            op.get(CHILDREN, Element.AUTHENTICATION.getLocalName(), TYPE).set(ModelType.LIST);
            op.get(CHILDREN, Element.AUTHENTICATION.getLocalName(), REQUIRED).set(false);
            op.get(CHILDREN, Element.AUTHENTICATION.getLocalName(), Attribute.CODE.getLocalName(), DESCRIPTION).set(
                    bundle.getString("code"));
            op.get(CHILDREN, Element.AUTHENTICATION.getLocalName(), Attribute.CODE.getLocalName(), TYPE).set(ModelType.STRING);
            op.get(CHILDREN, Element.AUTHENTICATION.getLocalName(), Attribute.CODE.getLocalName(), REQUIRED).set(true);
            op.get(CHILDREN, Element.AUTHENTICATION.getLocalName(), Attribute.FLAG.getLocalName(), DESCRIPTION).set(
                    bundle.getString("flag"));
            op.get(CHILDREN, Element.AUTHENTICATION.getLocalName(), Attribute.FLAG.getLocalName(), TYPE).set(ModelType.STRING);
            op.get(CHILDREN, Element.AUTHENTICATION.getLocalName(), Attribute.FLAG.getLocalName(), REQUIRED).set(true);
            op.get(CHILDREN, Element.AUTHENTICATION.getLocalName(), MODULE_OPTIONS, DESCRIPTION).set(
                    bundle.getString("module-options"));
            op.get(CHILDREN, Element.AUTHENTICATION.getLocalName(), MODULE_OPTIONS, TYPE).set(ModelType.LIST);
            op.get(CHILDREN, Element.AUTHENTICATION.getLocalName(), MODULE_OPTIONS, REQUIRED).set(false);
            op.get(CHILDREN, Element.AUTHENTICATION.getLocalName(), MODULE_OPTIONS, Attribute.NAME.getLocalName(), DESCRIPTION)
                    .set(bundle.getString("module-options.name"));
            op.get(CHILDREN, Element.AUTHENTICATION.getLocalName(), MODULE_OPTIONS, Attribute.NAME.getLocalName(), TYPE).set(
                    ModelType.STRING);
            op.get(CHILDREN, Element.AUTHENTICATION.getLocalName(), MODULE_OPTIONS, Attribute.NAME.getLocalName(), REQUIRED)
                    .set(true);
            op.get(CHILDREN, Element.AUTHENTICATION.getLocalName(), MODULE_OPTIONS, Attribute.VALUE.getLocalName(), DESCRIPTION)
                    .set(bundle.getString("module-options.value"));
            op.get(CHILDREN, Element.AUTHENTICATION.getLocalName(), MODULE_OPTIONS, Attribute.VALUE.getLocalName(), TYPE).set(
                    ModelType.STRING);
            op.get(CHILDREN, Element.AUTHENTICATION.getLocalName(), MODULE_OPTIONS, Attribute.VALUE.getLocalName(), REQUIRED)
                    .set(true);

            op.get(CHILDREN, Element.AUTHORIZATION.getLocalName(), DESCRIPTION).set(bundle.getString("authorization"));
            op.get(CHILDREN, Element.AUTHORIZATION.getLocalName(), TYPE).set(ModelType.LIST);
            op.get(CHILDREN, Element.AUTHORIZATION.getLocalName(), REQUIRED).set(false);
            op.get(CHILDREN, Element.AUTHORIZATION.getLocalName(), Attribute.CODE.getLocalName(), DESCRIPTION).set(
                    bundle.getString("code"));
            op.get(CHILDREN, Element.AUTHORIZATION.getLocalName(), Attribute.CODE.getLocalName(), TYPE).set(ModelType.STRING);
            op.get(CHILDREN, Element.AUTHORIZATION.getLocalName(), Attribute.CODE.getLocalName(), REQUIRED).set(true);
            op.get(CHILDREN, Element.AUTHORIZATION.getLocalName(), Attribute.FLAG.getLocalName(), DESCRIPTION).set(
                    bundle.getString("flag"));
            op.get(CHILDREN, Element.AUTHORIZATION.getLocalName(), Attribute.FLAG.getLocalName(), TYPE).set(ModelType.STRING);
            op.get(CHILDREN, Element.AUTHORIZATION.getLocalName(), Attribute.FLAG.getLocalName(), REQUIRED).set(true);
            op.get(CHILDREN, Element.AUTHORIZATION.getLocalName(), MODULE_OPTIONS, DESCRIPTION).set(
                    bundle.getString("module-options"));
            op.get(CHILDREN, Element.AUTHORIZATION.getLocalName(), MODULE_OPTIONS, TYPE).set(ModelType.LIST);
            op.get(CHILDREN, Element.AUTHORIZATION.getLocalName(), MODULE_OPTIONS, REQUIRED).set(false);
            op.get(CHILDREN, Element.AUTHORIZATION.getLocalName(), MODULE_OPTIONS, Attribute.NAME.getLocalName(), DESCRIPTION)
                    .set(bundle.getString("module-options.name"));
            op.get(CHILDREN, Element.AUTHORIZATION.getLocalName(), MODULE_OPTIONS, Attribute.NAME.getLocalName(), TYPE).set(
                    ModelType.STRING);
            op.get(CHILDREN, Element.AUTHORIZATION.getLocalName(), MODULE_OPTIONS, Attribute.NAME.getLocalName(), REQUIRED)
                    .set(true);
            op.get(CHILDREN, Element.AUTHORIZATION.getLocalName(), MODULE_OPTIONS, Attribute.VALUE.getLocalName(), DESCRIPTION)
                    .set(bundle.getString("module-options.value"));
            op.get(CHILDREN, Element.AUTHORIZATION.getLocalName(), MODULE_OPTIONS, Attribute.VALUE.getLocalName(), TYPE).set(
                    ModelType.STRING);
            op.get(CHILDREN, Element.AUTHORIZATION.getLocalName(), MODULE_OPTIONS, Attribute.VALUE.getLocalName(), REQUIRED)
                    .set(true);

            op.get(CHILDREN, Element.ACL.getLocalName(), DESCRIPTION).set(bundle.getString("acl"));
            op.get(CHILDREN, Element.ACL.getLocalName(), TYPE).set(ModelType.LIST);
            op.get(CHILDREN, Element.ACL.getLocalName(), REQUIRED).set(false);
            op.get(CHILDREN, Element.ACL.getLocalName(), Attribute.CODE.getLocalName(), DESCRIPTION).set(
                    bundle.getString("code"));
            op.get(CHILDREN, Element.ACL.getLocalName(), Attribute.CODE.getLocalName(), TYPE).set(ModelType.STRING);
            op.get(CHILDREN, Element.ACL.getLocalName(), Attribute.CODE.getLocalName(), REQUIRED).set(true);
            op.get(CHILDREN, Element.ACL.getLocalName(), Attribute.FLAG.getLocalName(), DESCRIPTION).set(
                    bundle.getString("flag"));
            op.get(CHILDREN, Element.ACL.getLocalName(), Attribute.FLAG.getLocalName(), TYPE).set(ModelType.STRING);
            op.get(CHILDREN, Element.ACL.getLocalName(), Attribute.FLAG.getLocalName(), REQUIRED).set(true);
            op.get(CHILDREN, Element.ACL.getLocalName(), MODULE_OPTIONS, DESCRIPTION).set(bundle.getString("module-options"));
            op.get(CHILDREN, Element.ACL.getLocalName(), MODULE_OPTIONS, TYPE).set(ModelType.LIST);
            op.get(CHILDREN, Element.ACL.getLocalName(), MODULE_OPTIONS, REQUIRED).set(false);
            op.get(CHILDREN, Element.ACL.getLocalName(), MODULE_OPTIONS, Attribute.NAME.getLocalName(), DESCRIPTION).set(
                    bundle.getString("module-options.name"));
            op.get(CHILDREN, Element.ACL.getLocalName(), MODULE_OPTIONS, Attribute.NAME.getLocalName(), TYPE).set(
                    ModelType.STRING);
            op.get(CHILDREN, Element.ACL.getLocalName(), MODULE_OPTIONS, Attribute.NAME.getLocalName(), REQUIRED).set(true);
            op.get(CHILDREN, Element.ACL.getLocalName(), MODULE_OPTIONS, Attribute.VALUE.getLocalName(), DESCRIPTION).set(
                    bundle.getString("module-options.value"));
            op.get(CHILDREN, Element.ACL.getLocalName(), MODULE_OPTIONS, Attribute.VALUE.getLocalName(), TYPE).set(
                    ModelType.STRING);
            op.get(CHILDREN, Element.ACL.getLocalName(), MODULE_OPTIONS, Attribute.VALUE.getLocalName(), REQUIRED).set(true);

            op.get(CHILDREN, Element.MAPPING.getLocalName(), DESCRIPTION).set(bundle.getString("mapping"));
            op.get(CHILDREN, Element.MAPPING.getLocalName(), TYPE).set(ModelType.LIST);
            op.get(CHILDREN, Element.MAPPING.getLocalName(), REQUIRED).set(false);
            op.get(CHILDREN, Element.MAPPING.getLocalName(), Attribute.CODE.getLocalName(), DESCRIPTION).set(
                    bundle.getString("code"));
            op.get(CHILDREN, Element.MAPPING.getLocalName(), Attribute.CODE.getLocalName(), TYPE).set(ModelType.STRING);
            op.get(CHILDREN, Element.MAPPING.getLocalName(), Attribute.CODE.getLocalName(), REQUIRED).set(true);
            op.get(CHILDREN, Element.MAPPING.getLocalName(), Attribute.TYPE.getLocalName(), DESCRIPTION).set(
                    bundle.getString("type"));
            op.get(CHILDREN, Element.MAPPING.getLocalName(), Attribute.TYPE.getLocalName(), TYPE).set(ModelType.STRING);
            op.get(CHILDREN, Element.MAPPING.getLocalName(), Attribute.TYPE.getLocalName(), REQUIRED).set(false);
            op.get(CHILDREN, Element.MAPPING.getLocalName(), MODULE_OPTIONS, DESCRIPTION).set(
                    bundle.getString("module-options"));
            op.get(CHILDREN, Element.MAPPING.getLocalName(), MODULE_OPTIONS, TYPE).set(ModelType.LIST);
            op.get(CHILDREN, Element.MAPPING.getLocalName(), MODULE_OPTIONS, REQUIRED).set(false);
            op.get(CHILDREN, Element.MAPPING.getLocalName(), MODULE_OPTIONS, Attribute.NAME.getLocalName(), DESCRIPTION).set(
                    bundle.getString("module-options.name"));
            op.get(CHILDREN, Element.MAPPING.getLocalName(), MODULE_OPTIONS, Attribute.NAME.getLocalName(), TYPE).set(
                    ModelType.STRING);
            op.get(CHILDREN, Element.MAPPING.getLocalName(), MODULE_OPTIONS, Attribute.NAME.getLocalName(), REQUIRED).set(true);
            op.get(CHILDREN, Element.MAPPING.getLocalName(), MODULE_OPTIONS, Attribute.VALUE.getLocalName(), DESCRIPTION).set(
                    bundle.getString("module-options.value"));
            op.get(CHILDREN, Element.MAPPING.getLocalName(), MODULE_OPTIONS, Attribute.VALUE.getLocalName(), TYPE).set(
                    ModelType.STRING);
            op.get(CHILDREN, Element.MAPPING.getLocalName(), MODULE_OPTIONS, Attribute.VALUE.getLocalName(), REQUIRED)
                    .set(true);

            op.get(CHILDREN, Element.AUDIT.getLocalName(), DESCRIPTION).set(bundle.getString("audit"));
            op.get(CHILDREN, Element.AUDIT.getLocalName(), TYPE).set(ModelType.LIST);
            op.get(CHILDREN, Element.AUDIT.getLocalName(), REQUIRED).set(false);
            op.get(CHILDREN, Element.AUDIT.getLocalName(), Attribute.CODE.getLocalName(), DESCRIPTION).set(
                    bundle.getString("code"));
            op.get(CHILDREN, Element.AUDIT.getLocalName(), Attribute.CODE.getLocalName(), TYPE).set(ModelType.STRING);
            op.get(CHILDREN, Element.AUDIT.getLocalName(), Attribute.CODE.getLocalName(), REQUIRED).set(true);
            op.get(CHILDREN, Element.AUDIT.getLocalName(), MODULE_OPTIONS, DESCRIPTION).set(bundle.getString("module-options"));
            op.get(CHILDREN, Element.AUDIT.getLocalName(), MODULE_OPTIONS, TYPE).set(ModelType.LIST);
            op.get(CHILDREN, Element.AUDIT.getLocalName(), MODULE_OPTIONS, REQUIRED).set(false);
            op.get(CHILDREN, Element.AUDIT.getLocalName(), MODULE_OPTIONS, Attribute.NAME.getLocalName(), DESCRIPTION).set(
                    bundle.getString("module-options.name"));
            op.get(CHILDREN, Element.AUDIT.getLocalName(), MODULE_OPTIONS, Attribute.NAME.getLocalName(), TYPE).set(
                    ModelType.STRING);
            op.get(CHILDREN, Element.AUDIT.getLocalName(), MODULE_OPTIONS, Attribute.NAME.getLocalName(), REQUIRED).set(true);
            op.get(CHILDREN, Element.AUDIT.getLocalName(), MODULE_OPTIONS, Attribute.VALUE.getLocalName(), DESCRIPTION).set(
                    bundle.getString("module-options.value"));
            op.get(CHILDREN, Element.AUDIT.getLocalName(), MODULE_OPTIONS, Attribute.VALUE.getLocalName(), TYPE).set(
                    ModelType.STRING);
            op.get(CHILDREN, Element.AUDIT.getLocalName(), MODULE_OPTIONS, Attribute.VALUE.getLocalName(), REQUIRED).set(true);

            op.get(CHILDREN, Element.IDENTITY_TRUST.getLocalName(), DESCRIPTION).set(bundle.getString("identity-trust"));
            op.get(CHILDREN, Element.IDENTITY_TRUST.getLocalName(), TYPE).set(ModelType.LIST);
            op.get(CHILDREN, Element.IDENTITY_TRUST.getLocalName(), REQUIRED).set(false);
            op.get(CHILDREN, Element.IDENTITY_TRUST.getLocalName(), Attribute.CODE.getLocalName(), DESCRIPTION).set(
                    bundle.getString("code"));
            op.get(CHILDREN, Element.IDENTITY_TRUST.getLocalName(), Attribute.CODE.getLocalName(), TYPE).set(ModelType.STRING);
            op.get(CHILDREN, Element.IDENTITY_TRUST.getLocalName(), Attribute.CODE.getLocalName(), REQUIRED).set(true);
            op.get(CHILDREN, Element.IDENTITY_TRUST.getLocalName(), Attribute.FLAG.getLocalName(), DESCRIPTION).set(
                    bundle.getString("flag"));
            op.get(CHILDREN, Element.IDENTITY_TRUST.getLocalName(), Attribute.FLAG.getLocalName(), TYPE).set(ModelType.STRING);
            op.get(CHILDREN, Element.IDENTITY_TRUST.getLocalName(), Attribute.FLAG.getLocalName(), REQUIRED).set(true);
            op.get(CHILDREN, Element.IDENTITY_TRUST.getLocalName(), MODULE_OPTIONS, DESCRIPTION).set(
                    bundle.getString("module-options"));
            op.get(CHILDREN, Element.IDENTITY_TRUST.getLocalName(), MODULE_OPTIONS, TYPE).set(ModelType.LIST);
            op.get(CHILDREN, Element.IDENTITY_TRUST.getLocalName(), MODULE_OPTIONS, REQUIRED).set(false);
            op.get(CHILDREN, Element.IDENTITY_TRUST.getLocalName(), MODULE_OPTIONS, Attribute.NAME.getLocalName(), DESCRIPTION)
                    .set(bundle.getString("module-options.name"));
            op.get(CHILDREN, Element.IDENTITY_TRUST.getLocalName(), MODULE_OPTIONS, Attribute.NAME.getLocalName(), TYPE).set(
                    ModelType.STRING);
            op.get(CHILDREN, Element.IDENTITY_TRUST.getLocalName(), MODULE_OPTIONS, Attribute.NAME.getLocalName(), REQUIRED)
                    .set(true);
            op.get(CHILDREN, Element.IDENTITY_TRUST.getLocalName(), MODULE_OPTIONS, Attribute.VALUE.getLocalName(), DESCRIPTION)
                    .set(bundle.getString("module-options.value"));
            op.get(CHILDREN, Element.IDENTITY_TRUST.getLocalName(), MODULE_OPTIONS, Attribute.VALUE.getLocalName(), TYPE).set(
                    ModelType.STRING);
            op.get(CHILDREN, Element.IDENTITY_TRUST.getLocalName(), MODULE_OPTIONS, Attribute.VALUE.getLocalName(), REQUIRED)
                    .set(true);

            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), DESCRIPTION).set(
                    bundle.getString("authentication-jaspi"));
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), TYPE).set(ModelType.LIST);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), REQUIRED).set(false);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.LOGIN_MODULE_STACK.getLocalName(),
                    DESCRIPTION).set(bundle.getString("authentication-jaspi.login-module-stack"));
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.LOGIN_MODULE_STACK.getLocalName(), TYPE).set(
                    ModelType.LIST);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.LOGIN_MODULE_STACK.getLocalName(), REQUIRED)
                    .set(false);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.LOGIN_MODULE_STACK.getLocalName(),
                    Attribute.NAME.getLocalName(), DESCRIPTION).set(
                    bundle.getString("authentication-jaspi.login-module-stack.name"));
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.LOGIN_MODULE_STACK.getLocalName(),
                    Attribute.NAME.getLocalName(), TYPE).set(ModelType.STRING);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.LOGIN_MODULE_STACK.getLocalName(),
                    Attribute.NAME.getLocalName(), REQUIRED).set(true);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.LOGIN_MODULE_STACK.getLocalName(),
                    Attribute.CODE.getLocalName(), DESCRIPTION).set(bundle.getString("code"));
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.LOGIN_MODULE_STACK.getLocalName(),
                    Attribute.CODE.getLocalName(), TYPE).set(ModelType.STRING);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.LOGIN_MODULE_STACK.getLocalName(),
                    Attribute.CODE.getLocalName(), REQUIRED).set(true);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.LOGIN_MODULE_STACK.getLocalName(),
                    Attribute.FLAG.getLocalName(), DESCRIPTION).set(bundle.getString("flag"));
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.LOGIN_MODULE_STACK.getLocalName(),
                    Attribute.FLAG.getLocalName(), TYPE).set(ModelType.STRING);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.LOGIN_MODULE_STACK.getLocalName(),
                    Attribute.FLAG.getLocalName(), REQUIRED).set(true);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.LOGIN_MODULE_STACK.getLocalName(),
                    MODULE_OPTIONS, DESCRIPTION).set(bundle.getString("module-options"));
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.LOGIN_MODULE_STACK.getLocalName(),
                    MODULE_OPTIONS, TYPE).set(ModelType.LIST);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.LOGIN_MODULE_STACK.getLocalName(),
                    MODULE_OPTIONS, REQUIRED).set(false);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.LOGIN_MODULE_STACK.getLocalName(),
                    MODULE_OPTIONS, Attribute.NAME.getLocalName(), DESCRIPTION).set(bundle.getString("module-options.name"));
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.LOGIN_MODULE_STACK.getLocalName(),
                    MODULE_OPTIONS, Attribute.NAME.getLocalName(), TYPE).set(ModelType.STRING);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.LOGIN_MODULE_STACK.getLocalName(),
                    MODULE_OPTIONS, Attribute.NAME.getLocalName(), REQUIRED).set(true);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.LOGIN_MODULE_STACK.getLocalName(),
                    MODULE_OPTIONS, Attribute.VALUE.getLocalName(), DESCRIPTION).set(bundle.getString("module-options.value"));
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.LOGIN_MODULE_STACK.getLocalName(),
                    MODULE_OPTIONS, Attribute.VALUE.getLocalName(), TYPE).set(ModelType.STRING);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.LOGIN_MODULE_STACK.getLocalName(),
                    MODULE_OPTIONS, Attribute.VALUE.getLocalName(), REQUIRED).set(true);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.AUTH_MODULE.getLocalName(), DESCRIPTION).set(
                    bundle.getString("authentication-jaspi.auth-module"));
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.AUTH_MODULE.getLocalName(), TYPE).set(
                    ModelType.LIST);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.AUTH_MODULE.getLocalName(), REQUIRED).set(
                    false);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.AUTH_MODULE.getLocalName(),
                    Attribute.CODE.getLocalName(), DESCRIPTION).set(bundle.getString("code"));
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.AUTH_MODULE.getLocalName(),
                    Attribute.CODE.getLocalName(), TYPE).set(ModelType.STRING);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.AUTH_MODULE.getLocalName(),
                    Attribute.CODE.getLocalName(), REQUIRED).set(true);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.AUTH_MODULE.getLocalName(),
                    Attribute.LOGIN_MODULE_STACK_REF.getLocalName(), DESCRIPTION).set(
                    bundle.getString("login-module-stack-ref"));
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.AUTH_MODULE.getLocalName(),
                    Attribute.LOGIN_MODULE_STACK_REF.getLocalName(), TYPE).set(ModelType.STRING);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.AUTH_MODULE.getLocalName(),
                    Attribute.LOGIN_MODULE_STACK_REF.getLocalName(), REQUIRED).set(false);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.AUTH_MODULE.getLocalName(), MODULE_OPTIONS,
                    DESCRIPTION).set(bundle.getString("module-options"));
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.AUTH_MODULE.getLocalName(), MODULE_OPTIONS,
                    TYPE).set(ModelType.LIST);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.AUTH_MODULE.getLocalName(), MODULE_OPTIONS,
                    REQUIRED).set(false);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.AUTH_MODULE.getLocalName(), MODULE_OPTIONS,
                    Attribute.NAME.getLocalName(), DESCRIPTION).set(bundle.getString("module-options.name"));
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.AUTH_MODULE.getLocalName(), MODULE_OPTIONS,
                    Attribute.NAME.getLocalName(), TYPE).set(ModelType.STRING);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.AUTH_MODULE.getLocalName(), MODULE_OPTIONS,
                    Attribute.NAME.getLocalName(), REQUIRED).set(true);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.AUTH_MODULE.getLocalName(), MODULE_OPTIONS,
                    Attribute.VALUE.getLocalName(), DESCRIPTION).set(bundle.getString("module-options.value"));
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.AUTH_MODULE.getLocalName(), MODULE_OPTIONS,
                    Attribute.VALUE.getLocalName(), TYPE).set(ModelType.STRING);
            op.get(CHILDREN, Element.AUTHENTICATION_JASPI.getLocalName(), Element.AUTH_MODULE.getLocalName(), MODULE_OPTIONS,
                    Attribute.VALUE.getLocalName(), REQUIRED).set(true);

            return op;
        }
    }

}
