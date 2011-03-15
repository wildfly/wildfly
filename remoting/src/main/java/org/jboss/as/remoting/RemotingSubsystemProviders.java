/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.remoting;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.remoting.CommonAttributes.AUTHENTICATION_PROVIDER;
import static org.jboss.as.remoting.CommonAttributes.CONNECTOR;
import static org.jboss.as.remoting.CommonAttributes.FORWARD_SECRECY;
import static org.jboss.as.remoting.CommonAttributes.INCLUDE_MECHANISMS;
import static org.jboss.as.remoting.CommonAttributes.NO_ACTIVE;
import static org.jboss.as.remoting.CommonAttributes.NO_ANONYMOUS;
import static org.jboss.as.remoting.CommonAttributes.NO_DICTIONARY;
import static org.jboss.as.remoting.CommonAttributes.NO_PLAINTEXT;
import static org.jboss.as.remoting.CommonAttributes.PASS_CREDENTIALS;
import static org.jboss.as.remoting.CommonAttributes.POLICY;
import static org.jboss.as.remoting.CommonAttributes.PROPERTIES;
import static org.jboss.as.remoting.CommonAttributes.QOP;
import static org.jboss.as.remoting.CommonAttributes.REUSE_SESSION;
import static org.jboss.as.remoting.CommonAttributes.SASL;
import static org.jboss.as.remoting.CommonAttributes.SERVER_AUTH;
import static org.jboss.as.remoting.CommonAttributes.SOCKET_BINDING;
import static org.jboss.as.remoting.CommonAttributes.THREAD_POOL;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * The remoting subsystem description providers.
 *
 * @author Emanuel Muckenhuber
 */
class RemotingSubsystemProviders {

    static final String RESOURCE_NAME = RemotingSubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode subsystem = new ModelNode();
            subsystem.get(DESCRIPTION).set(bundle.getString("remoting"));
            subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
            subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
            subsystem.get(NAMESPACE).set(Namespace.REMOTING_1_0.getUriString());

            subsystem.get(ATTRIBUTES, THREAD_POOL, TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, THREAD_POOL, DESCRIPTION).set(bundle.getString("remoting.thread-pool"));
            subsystem.get(ATTRIBUTES, THREAD_POOL, REQUIRED).set(true);

            subsystem.get(CHILDREN, CONNECTOR).set(getConnectorDescription(bundle));

            return subsystem;
        }
    };

    static final DescriptionProvider SUBSYSTEM_ADD = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set("add");
            operation.get(REQUEST_PROPERTIES, THREAD_POOL, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, THREAD_POOL, DESCRIPTION).set(bundle.getString("remoting.thread-pool"));
            operation.get(REQUEST_PROPERTIES, THREAD_POOL, REQUIRED).set(true);

            return operation;
        }
    };

    static final DescriptionProvider CONNECTOR_ADD = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set("add");

            operation.get(REQUEST_PROPERTIES, NAME, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, NAME, DESCRIPTION).set(bundle.getString("remoting.connector.name"));
            operation.get(REQUEST_PROPERTIES, NAME, REQUIRED).set(true);

            operation.get(REQUEST_PROPERTIES, SOCKET_BINDING, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, SOCKET_BINDING, DESCRIPTION).set(bundle.getString("remoting.connector.socket-binding"));
            operation.get(REQUEST_PROPERTIES, SOCKET_BINDING, REQUIRED).set(true);

            operation.get(REQUEST_PROPERTIES, AUTHENTICATION_PROVIDER, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, AUTHENTICATION_PROVIDER, DESCRIPTION).set(bundle.getString("remoting.connector.authentication-provider"));
            operation.get(REQUEST_PROPERTIES, AUTHENTICATION_PROVIDER, REQUIRED).set(false);

            operation.get(REQUEST_PROPERTIES, SASL).set(getSaslElement(bundle));

            return operation;
        }
    };

    static final DescriptionProvider CONNECTOR_REMOVE = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set("remove");

            operation.get(REQUEST_PROPERTIES, NAME, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, NAME, DESCRIPTION).set(bundle.getString("remoting.connector.name"));
            operation.get(REQUEST_PROPERTIES, NAME, REQUIRED).set(true);

            return operation;
        }
    };

    static final DescriptionProvider CONNECTOR_SPEC = new DescriptionProvider( ) {

        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            return getConnectorDescription(bundle);
        }
    };


    static ModelNode getConnectorDescription(final ResourceBundle bundle) {
        final ModelNode connector = new ModelNode();

        connector.get(TYPE).set(ModelType.OBJECT);
        connector.get(DESCRIPTION).set(bundle.getString("remoting.connector"));

        connector.get(ATTRIBUTES, NAME, TYPE).set(ModelType.STRING);
        connector.get(ATTRIBUTES, NAME, DESCRIPTION).set(bundle.getString("remoting.connector.name"));
        connector.get(ATTRIBUTES, NAME, REQUIRED).set(true);

        connector.get(ATTRIBUTES, SOCKET_BINDING, TYPE).set(ModelType.STRING);
        connector.get(ATTRIBUTES, SOCKET_BINDING, DESCRIPTION).set(bundle.getString("remoting.connector.socket-binding"));
        connector.get(ATTRIBUTES, SOCKET_BINDING, REQUIRED).set(true);

        connector.get(ATTRIBUTES, AUTHENTICATION_PROVIDER, TYPE).set(ModelType.STRING);
        connector.get(ATTRIBUTES, AUTHENTICATION_PROVIDER, DESCRIPTION).set(bundle.getString("remoting.connector.authentication-provider"));
        connector.get(ATTRIBUTES, AUTHENTICATION_PROVIDER, REQUIRED).set(false);

        connector.get(CHILDREN, SASL).set(getSaslElement(bundle));

        return connector;
    }


    static ModelNode getSaslElement(final ResourceBundle bundle) {
        final ModelNode sasl = new ModelNode();

        sasl.get(TYPE).set(ModelType.OBJECT);
        sasl.get(DESCRIPTION).set(bundle.getString("remoting.sasl"));
        sasl.get(REQUIRED).set(false);

        sasl.get(ATTRIBUTES, REUSE_SESSION, TYPE).set(ModelType.BOOLEAN);
        sasl.get(ATTRIBUTES, REUSE_SESSION, DESCRIPTION).set(bundle.getString("remoting.sasl.reuse-session"));
        sasl.get(ATTRIBUTES, REUSE_SESSION, REQUIRED).set(false);
        sasl.get(ATTRIBUTES, REUSE_SESSION, DEFAULT).set(false);

        sasl.get(ATTRIBUTES, SERVER_AUTH, TYPE).set(ModelType.BOOLEAN);
        sasl.get(ATTRIBUTES, SERVER_AUTH, DESCRIPTION).set(bundle.getString("remoting.sasl.server-auth"));
        sasl.get(ATTRIBUTES, SERVER_AUTH, REQUIRED).set(false);
        sasl.get(ATTRIBUTES, SERVER_AUTH, DEFAULT).set(false);

        sasl.get(CHILDREN, INCLUDE_MECHANISMS, TYPE).set(ModelType.LIST);
        sasl.get(CHILDREN, INCLUDE_MECHANISMS, VALUE_TYPE).set(ModelType.STRING);
        sasl.get(CHILDREN, INCLUDE_MECHANISMS, DESCRIPTION).set(bundle.getString("remoting.sasl.include-mechanisms"));
        sasl.get(CHILDREN, INCLUDE_MECHANISMS, REQUIRED).set(false);

        sasl.get(CHILDREN, QOP, TYPE).set(ModelType.LIST);
        sasl.get(CHILDREN, QOP, VALUE_TYPE).set(ModelType.STRING);
        sasl.get(CHILDREN, QOP, DESCRIPTION).set(bundle.getString("remoting.sasl.qop"));
        sasl.get(CHILDREN, QOP, REQUIRED).set(false);

        sasl.get(CHILDREN, POLICY).set(getPolicyElement(bundle));

        sasl.get(CHILDREN, PROPERTIES, TYPE).set(ModelType.LIST);
        sasl.get(CHILDREN, PROPERTIES, VALUE_TYPE).set(ModelType.PROPERTY);

        return sasl;
    }

    static ModelNode getPolicyElement(final ResourceBundle bundle) {
        final ModelNode policy = new ModelNode();

        policy.get(TYPE).set(ModelType.PROPERTY);
        policy.get(VALUE_TYPE).set(ModelType.BOOLEAN);
        policy.get(DESCRIPTION).set(bundle.getString("remoting.sasl.policy"));

        policy.get(ATTRIBUTES, FORWARD_SECRECY, TYPE).set(ModelType.BOOLEAN);
        policy.get(ATTRIBUTES, FORWARD_SECRECY, DESCRIPTION).set(bundle.getString("remoting.sasl.policy.forward-secrecy"));
        policy.get(ATTRIBUTES, NO_ACTIVE, TYPE).set(ModelType.BOOLEAN);
        policy.get(ATTRIBUTES, NO_ACTIVE, DESCRIPTION).set(bundle.getString("remoting.sasl.policy.no-active"));
        policy.get(ATTRIBUTES, NO_ANONYMOUS, TYPE).set(ModelType.BOOLEAN);
        policy.get(ATTRIBUTES, NO_ANONYMOUS, DESCRIPTION).set(bundle.getString("remoting.sasl.policy.no-anonymous"));
        policy.get(ATTRIBUTES, NO_DICTIONARY, TYPE).set(ModelType.BOOLEAN);
        policy.get(ATTRIBUTES, NO_DICTIONARY, DESCRIPTION).set(bundle.getString("remoting.sasl.policy.no-dictionary"));
        policy.get(ATTRIBUTES, NO_PLAINTEXT, TYPE).set(ModelType.BOOLEAN);
        policy.get(ATTRIBUTES, NO_PLAINTEXT, DESCRIPTION).set(bundle.getString("remoting.sasl.policy.no-plain-text"));
        policy.get(ATTRIBUTES, PASS_CREDENTIALS, TYPE).set(ModelType.BOOLEAN);
        policy.get(ATTRIBUTES, PASS_CREDENTIALS, DESCRIPTION).set(bundle.getString("remoting.sasl.policy.pass-credentials"));

        return policy;
    }


    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}
