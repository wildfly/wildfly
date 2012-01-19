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
package org.jboss.as.webservices.dmr;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_CLASS;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_CONFIG;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_CONTEXT;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_NAME;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_TYPE;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_WSDL;
import static org.jboss.as.webservices.dmr.Constants.FEATURE;
import static org.jboss.as.webservices.dmr.Constants.HANDLER_CHAIN;
import static org.jboss.as.webservices.dmr.Constants.HANDLER_CLASS;
import static org.jboss.as.webservices.dmr.Constants.MODIFY_WSDL_ADDRESS;
import static org.jboss.as.webservices.dmr.Constants.POST_HANDLER_CHAIN;
import static org.jboss.as.webservices.dmr.Constants.PRE_HANDLER_CHAIN;
import static org.jboss.as.webservices.dmr.Constants.PROTOCOL_BINDINGS;
import static org.jboss.as.webservices.dmr.Constants.PORT_NAME_PATTERN;
import static org.jboss.as.webservices.dmr.Constants.SERVICE_NAME_PATTERN;
import static org.jboss.as.webservices.dmr.Constants.PROPERTY;
import static org.jboss.as.webservices.dmr.Constants.WSDL_HOST;
import static org.jboss.as.webservices.dmr.Constants.WSDL_PORT;
import static org.jboss.as.webservices.dmr.Constants.WSDL_SECURE_PORT;
import static org.jboss.as.webservices.dmr.WSEndpointMetrics.AVERAGE_PROCESSING_TIME;
import static org.jboss.as.webservices.dmr.WSEndpointMetrics.FAULT_COUNT;
import static org.jboss.as.webservices.dmr.WSEndpointMetrics.MAX_PROCESSING_TIME;
import static org.jboss.as.webservices.dmr.WSEndpointMetrics.MIN_PROCESSING_TIME;
import static org.jboss.as.webservices.dmr.WSEndpointMetrics.REQUEST_COUNT;
import static org.jboss.as.webservices.dmr.WSEndpointMetrics.RESPONSE_COUNT;
import static org.jboss.as.webservices.dmr.WSEndpointMetrics.TOTAL_PROCESSING_TIME;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

// TODO: review REQUIRED - should be present only on operations?
// TODO: review NILLABLE
/**
 * Deployment model providers.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class WSSubsystemProviders {

    static final String RESOURCE_NAME = WSSubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    private WSSubsystemProviders() {
        super();
    }

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {
        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getSubsystem(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_ADD = new DescriptionProvider() {
        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getSubsystemAdd(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_DESCRIBE = new DescriptionProvider() {
        public ModelNode getModelDescription(final Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return Descriptions.getSubsystemRemoveDescription(locale);
        }
    };

    static final DescriptionProvider DEPLOYMENT_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getDeploymentDescription(locale);
        }
    };

    static final DescriptionProvider ENDPOINT_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getEndpointDescription(locale);
        }
    };

    static final DescriptionProvider ENDPOINT_ADD_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getEndpointAddDescription(locale);
        }
    };

    static final DescriptionProvider ENDPOINT_REMOVE_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getEndpointRemoveDescription(locale);
        }
    };

    static final DescriptionProvider ENDPOINT_CONFIG_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getEndpointConfigDescription(locale);
        }
    };

    static final DescriptionProvider ENDPOINT_CONFIG_FEATURE_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getEndpointConfigFeatureDescription(locale);
        }
    };

    static final DescriptionProvider ENDPOINT_CONFIG_PROPERTY_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getEndpointConfigPropertyDescription(locale);
        }
    };

    static final DescriptionProvider ENDPOINT_CONFIG_PRE_HANDLER_CHAIN_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getEndpointConfigHandlerChainDescription(locale, "endpoint.config.pre.handler.chain");
        }
    };

    static final DescriptionProvider ENDPOINT_CONFIG_HANDLER_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getEndpointConfigHandlerDescription(locale);
        }
    };

    static final DescriptionProvider ENDPOINT_CONFIG_POST_HANDLER_CHAIN_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getEndpointConfigHandlerChainDescription(locale, "endpoint.config.post.handler.chain");
        }
    };

    static final DescriptionProvider ENDPOINT_CONFIG_ADD_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getEndpointConfigAddDescription(locale);
        }
    };

    static final DescriptionProvider ENDPOINT_CONFIG_FEATURE_ADD_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getEndpointConfigFeatureAddDescription(locale);
        }
    };

    static final DescriptionProvider ENDPOINT_CONFIG_PROPERTY_ADD_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getEndpointConfigPropertyAddDescription(locale);
        }
    };

    static final DescriptionProvider ENDPOINT_CONFIG_PRE_HANDLER_CHAIN_ADD_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getEndpointConfigHandlerChainAddDescription(locale, "endpoint.config.pre.handler.chain");
        }
    };

    static final DescriptionProvider ENDPOINT_CONFIG_HANDLER_ADD_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getEndpointConfigHandlerAddDescription(locale);
        }
    };

    static final DescriptionProvider ENDPOINT_CONFIG_POST_HANDLER_CHAIN_ADD_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getEndpointConfigHandlerChainAddDescription(locale, "endpoint.config.post.handler.chain");
        }
    };

    static final DescriptionProvider ENDPOINT_CONFIG_REMOVE_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getEndpointConfigRemoveDescription(locale);
        }
    };

    static final DescriptionProvider ENDPOINT_CONFIG_FEATURE_REMOVE_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getEndpointConfigFeatureRemoveDescription(locale);
        }
    };

    static final DescriptionProvider ENDPOINT_CONFIG_PROPERTY_REMOVE_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getEndpointConfigPropertyRemoveDescription(locale);
        }
    };

    private static ResourceBundle getResourceBundle(final Locale locale) {
        return ResourceBundle.getBundle(RESOURCE_NAME, locale == null ? Locale.getDefault() : locale);
    }

    private static class Descriptions {
        static ModelNode getSubsystem(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode subsystem = new ModelNode();

            subsystem.get(DESCRIPTION).set(bundle.getString("ws"));
            subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
            subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
            subsystem.get(NAMESPACE).set(Namespace.WEBSERVICES_1_0.getUriString());

            subsystem.get(ATTRIBUTES, MODIFY_WSDL_ADDRESS, DESCRIPTION).set(bundle.getString("modify.wsdl.address"));
            subsystem.get(ATTRIBUTES, MODIFY_WSDL_ADDRESS, TYPE).set(ModelType.BOOLEAN);
            subsystem.get(ATTRIBUTES, MODIFY_WSDL_ADDRESS, REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, MODIFY_WSDL_ADDRESS, NILLABLE).set(true);

            subsystem.get(ATTRIBUTES, WSDL_HOST, DESCRIPTION).set(bundle.getString("wsdl.host"));
            subsystem.get(ATTRIBUTES, WSDL_HOST, TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, WSDL_HOST, REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, WSDL_HOST, NILLABLE).set(true);

            subsystem.get(ATTRIBUTES, WSDL_PORT, DESCRIPTION).set(bundle.getString("wsdl.port"));
            subsystem.get(ATTRIBUTES, WSDL_PORT, TYPE).set(ModelType.INT);
            subsystem.get(ATTRIBUTES, WSDL_PORT, REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, WSDL_PORT, NILLABLE).set(true);

            subsystem.get(ATTRIBUTES, WSDL_SECURE_PORT, DESCRIPTION).set(bundle.getString("wsdl.secure.port"));
            subsystem.get(ATTRIBUTES, WSDL_SECURE_PORT, TYPE).set(ModelType.INT);
            subsystem.get(ATTRIBUTES, WSDL_SECURE_PORT, REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, WSDL_SECURE_PORT, NILLABLE).set(true);

            subsystem.get(CHILDREN, ENDPOINT_CONFIG, DESCRIPTION).set(bundle.getString("endpoint.config"));
            subsystem.get(CHILDREN, ENDPOINT_CONFIG, REQUIRED).set(false);

            subsystem.get(CHILDREN, ENDPOINT, DESCRIPTION).set(bundle.getString("endpoint"));
            subsystem.get(CHILDREN, ENDPOINT, REQUIRED).set(false);

            return subsystem;
        }

        static ModelNode getEndpointAddDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();

            operation.get(OPERATION_NAME).set(ADD);
            operation.get(DESCRIPTION).set(bundle.getString("endpoint.add"));

            operation.get(REQUEST_PROPERTIES, ENDPOINT_NAME, DESCRIPTION).set(bundle.getString("endpoint.name"));
            operation.get(REQUEST_PROPERTIES, ENDPOINT_NAME, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, ENDPOINT_NAME, REQUIRED).set(true);

            operation.get(REQUEST_PROPERTIES, ENDPOINT_CONTEXT, DESCRIPTION).set(bundle.getString("endpoint.context"));
            operation.get(REQUEST_PROPERTIES, ENDPOINT_CONTEXT, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, ENDPOINT_CONTEXT, REQUIRED).set(true);

            operation.get(REQUEST_PROPERTIES, ENDPOINT_CLASS, DESCRIPTION).set(bundle.getString("endpoint.class"));
            operation.get(REQUEST_PROPERTIES, ENDPOINT_CLASS, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, ENDPOINT_CLASS, REQUIRED).set(false);

            operation.get(REQUEST_PROPERTIES, ENDPOINT_TYPE, DESCRIPTION).set(bundle.getString("endpoint.type"));
            operation.get(REQUEST_PROPERTIES, ENDPOINT_TYPE, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, ENDPOINT_TYPE, REQUIRED).set(false);

            operation.get(REQUEST_PROPERTIES, ENDPOINT_WSDL, DESCRIPTION).set(bundle.getString("endpoint.wsdl"));
            operation.get(REQUEST_PROPERTIES, ENDPOINT_WSDL, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, ENDPOINT_WSDL, REQUIRED).set(false);

            operation.get(REPLY_PROPERTIES).setEmptyObject();

            return operation;
        }

        static ModelNode getEndpointRemoveDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();

            operation.get(OPERATION_NAME).set(REMOVE);
            operation.get(DESCRIPTION).set(bundle.getString("endpoint.remove"));

            operation.get(REQUEST_PROPERTIES, NAME, DESCRIPTION).set(bundle.getString("endpoint.name"));
            operation.get(REQUEST_PROPERTIES, NAME, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, NAME, REQUIRED).set(true);

            operation.get(REPLY_PROPERTIES).setEmptyObject();

            return operation;
        }

        static ModelNode getDeploymentDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();

            node.get(DESCRIPTION).set(bundle.getString("deployment"));
            node.get(ATTRIBUTES).setEmptyObject();
            node.get(OPERATIONS); // placeholder

            node.get(CHILDREN, ENDPOINT, DESCRIPTION).set(bundle.getString("endpoint"));
            node.get(CHILDREN, ENDPOINT, MIN_OCCURS).set(0);
            node.get(CHILDREN, ENDPOINT, MODEL_DESCRIPTION);

            return node;
        }

        static ModelNode getEndpointDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();

            node.get(DESCRIPTION).set(bundle.getString("endpoint"));
            node.get(HEAD_COMMENT_ALLOWED).set(true);
            node.get(TAIL_COMMENT_ALLOWED).set(true);

            node.get(ATTRIBUTES, ENDPOINT_NAME, DESCRIPTION).set(bundle.getString("endpoint.name"));
            node.get(ATTRIBUTES, ENDPOINT_NAME, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, ENDPOINT_NAME, REQUIRED).set(true);

            node.get(ATTRIBUTES, ENDPOINT_CONTEXT, DESCRIPTION).set(bundle.getString("endpoint.context"));
            node.get(ATTRIBUTES, ENDPOINT_CONTEXT, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, ENDPOINT_CONTEXT, REQUIRED).set(true);

            node.get(ATTRIBUTES, ENDPOINT_CLASS, DESCRIPTION).set(bundle.getString("endpoint.class"));
            node.get(ATTRIBUTES, ENDPOINT_CLASS, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, ENDPOINT_CLASS, REQUIRED).set(false);

            node.get(ATTRIBUTES, ENDPOINT_TYPE, DESCRIPTION).set(bundle.getString("endpoint.type"));
            node.get(ATTRIBUTES, ENDPOINT_TYPE, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, ENDPOINT_TYPE, REQUIRED).set(false);

            node.get(ATTRIBUTES, ENDPOINT_WSDL, DESCRIPTION).set(bundle.getString("endpoint.wsdl"));
            node.get(ATTRIBUTES, ENDPOINT_WSDL, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, ENDPOINT_WSDL, REQUIRED).set(false);

            node.get(ATTRIBUTES, AVERAGE_PROCESSING_TIME, DESCRIPTION).set(bundle.getString("average.processing.time"));
            node.get(ATTRIBUTES, AVERAGE_PROCESSING_TIME, TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, AVERAGE_PROCESSING_TIME, REQUIRED).set(false);

            node.get(ATTRIBUTES, MIN_PROCESSING_TIME, DESCRIPTION).set(bundle.getString("min.processing.time"));
            node.get(ATTRIBUTES, MIN_PROCESSING_TIME, TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, MIN_PROCESSING_TIME, REQUIRED).set(false);

            node.get(ATTRIBUTES, MAX_PROCESSING_TIME, DESCRIPTION).set(bundle.getString("max.processing.time"));
            node.get(ATTRIBUTES, MAX_PROCESSING_TIME, TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, MAX_PROCESSING_TIME, REQUIRED).set(false);

            node.get(ATTRIBUTES, TOTAL_PROCESSING_TIME, DESCRIPTION).set(bundle.getString("total.processing.time"));
            node.get(ATTRIBUTES, TOTAL_PROCESSING_TIME, TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, TOTAL_PROCESSING_TIME, REQUIRED).set(false);

            node.get(ATTRIBUTES, REQUEST_COUNT, DESCRIPTION).set(bundle.getString("request.count"));
            node.get(ATTRIBUTES, REQUEST_COUNT, TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, REQUEST_COUNT, REQUIRED).set(false);

            node.get(ATTRIBUTES, RESPONSE_COUNT, DESCRIPTION).set(bundle.getString("response.count"));
            node.get(ATTRIBUTES, RESPONSE_COUNT, TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, RESPONSE_COUNT, REQUIRED).set(false);

            node.get(ATTRIBUTES, FAULT_COUNT, DESCRIPTION).set(bundle.getString("fault.count"));
            node.get(ATTRIBUTES, FAULT_COUNT, TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, FAULT_COUNT, REQUIRED).set(false);

            return node;
        }

        static ModelNode getEndpointConfigDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();

            node.get(DESCRIPTION).set(bundle.getString("endpoint.config"));
            node.get(HEAD_COMMENT_ALLOWED).set(true);
            node.get(TAIL_COMMENT_ALLOWED).set(true);

            node.get(CHILDREN, PRE_HANDLER_CHAIN, DESCRIPTION).set(bundle.getString("endpoint.config.pre.handler.chain"));
            node.get(CHILDREN, PRE_HANDLER_CHAIN, REQUIRED).set(false);
            node.get(CHILDREN, PRE_HANDLER_CHAIN, NILLABLE).set(true);

            node.get(CHILDREN, POST_HANDLER_CHAIN, DESCRIPTION).set(bundle.getString("endpoint.config.post.handler.chain"));
            node.get(CHILDREN, POST_HANDLER_CHAIN, REQUIRED).set(false);
            node.get(CHILDREN, POST_HANDLER_CHAIN, NILLABLE).set(true);

            node.get(CHILDREN, PROPERTY, DESCRIPTION).set(bundle.getString("endpoint.config.property"));
            node.get(CHILDREN, PROPERTY, REQUIRED).set(false);
            node.get(CHILDREN, POST_HANDLER_CHAIN, NILLABLE).set(true);

            node.get(CHILDREN, FEATURE, DESCRIPTION).set(bundle.getString("endpoint.config.feature"));
            node.get(CHILDREN, FEATURE, REQUIRED).set(false);
            node.get(CHILDREN, POST_HANDLER_CHAIN, NILLABLE).set(true);

            // TODO: rewrite the following commented out code
            /*
            node.get(ATTRIBUTES, Constants.PRE_HANDLER_CHAINS, Constants.HANDLER_CHAIN, Constants.HANDLER, Constants.HANDLER_NAME, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, Constants.PRE_HANDLER_CHAINS, Constants.HANDLER_CHAIN, Constants.HANDLER, Constants.HANDLER_NAME, DESCRIPTION).set(bundle.getString("handler.name"));
            node.get(ATTRIBUTES, Constants.PRE_HANDLER_CHAINS, Constants.HANDLER_CHAIN, Constants.HANDLER, Constants.HANDLER_NAME, REQUIRED).set(true);

            node.get(ATTRIBUTES, Constants.PRE_HANDLER_CHAINS, Constants.HANDLER_CHAIN, Constants.HANDLER, Constants.HANDLER_CLASS, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, Constants.PRE_HANDLER_CHAINS, Constants.HANDLER_CHAIN, Constants.HANDLER, Constants.HANDLER_CLASS, DESCRIPTION).set(bundle.getString("handler.class"));
            node.get(ATTRIBUTES, Constants.PRE_HANDLER_CHAINS, Constants.HANDLER_CHAIN, Constants.HANDLER, Constants.HANDLER_CLASS, REQUIRED).set(true);

            node.get(ATTRIBUTES, Constants.POST_HANDLER_CHAINS, Constants.HANDLER_CHAIN, Constants.HANDLER, Constants.HANDLER_NAME, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, Constants.POST_HANDLER_CHAINS, Constants.HANDLER_CHAIN, Constants.HANDLER, Constants.HANDLER_NAME, DESCRIPTION).set(bundle.getString("handler.name"));
            node.get(ATTRIBUTES, Constants.POST_HANDLER_CHAINS, Constants.HANDLER_CHAIN, Constants.HANDLER, Constants.HANDLER_NAME, REQUIRED).set(true);

            node.get(ATTRIBUTES, Constants.POST_HANDLER_CHAINS, Constants.HANDLER_CHAIN, Constants.HANDLER, Constants.HANDLER_CLASS, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, Constants.POST_HANDLER_CHAINS, Constants.HANDLER_CHAIN, Constants.HANDLER, Constants.HANDLER_CLASS, DESCRIPTION).set(bundle.getString("handler.class"));
            node.get(ATTRIBUTES, Constants.POST_HANDLER_CHAINS, Constants.HANDLER_CHAIN, Constants.HANDLER, Constants.HANDLER_CLASS, REQUIRED).set(true);
            */

            return node;
        }

        static ModelNode getEndpointConfigHandlerChainDescription(final Locale locale, final String handlerChainName) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();

            node.get(DESCRIPTION).set(bundle.getString(handlerChainName));
            node.get(HEAD_COMMENT_ALLOWED).set(true);
            node.get(TAIL_COMMENT_ALLOWED).set(true);

            node.get(ATTRIBUTES, PROTOCOL_BINDINGS, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, PROTOCOL_BINDINGS, DESCRIPTION).set(bundle.getString("protocol.binding"));
            node.get(ATTRIBUTES, PROTOCOL_BINDINGS, REQUIRED).set(false);

            node.get(ATTRIBUTES, PORT_NAME_PATTERN, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, PORT_NAME_PATTERN, DESCRIPTION).set(bundle.getString("portname.pattern"));
            node.get(ATTRIBUTES, PORT_NAME_PATTERN, REQUIRED).set(false);

            node.get(ATTRIBUTES, SERVICE_NAME_PATTERN, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, SERVICE_NAME_PATTERN, DESCRIPTION).set(bundle.getString("servicename.pattern"));
            node.get(ATTRIBUTES, SERVICE_NAME_PATTERN, REQUIRED).set(false);

            node.get(CHILDREN, HANDLER_CHAIN, DESCRIPTION).set(bundle.getString(handlerChainName));
            node.get(CHILDREN, HANDLER_CHAIN, REQUIRED).set(false);

            return node;
        }

        static ModelNode getEndpointConfigHandlerDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();

            node.get(DESCRIPTION).set(bundle.getString("handler"));
            node.get(HEAD_COMMENT_ALLOWED).set(true);
            node.get(TAIL_COMMENT_ALLOWED).set(true);

            node.get(ATTRIBUTES, HANDLER_CLASS, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, HANDLER_CLASS, DESCRIPTION).set(bundle.getString("handler.class"));
            node.get(ATTRIBUTES, HANDLER_CLASS, REQUIRED).set(true);

            return node;
        }

        static ModelNode getEndpointConfigFeatureDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();

            node.get(DESCRIPTION).set(bundle.getString("endpoint.config.feature"));
            node.get(HEAD_COMMENT_ALLOWED).set(true);
            node.get(TAIL_COMMENT_ALLOWED).set(true);

            return node;
        }

        static ModelNode getEndpointConfigPropertyDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();

            node.get(DESCRIPTION).set(bundle.getString("endpoint.config.property"));
            node.get(HEAD_COMMENT_ALLOWED).set(true);
            node.get(TAIL_COMMENT_ALLOWED).set(true);

            node.get(ATTRIBUTES, VALUE, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, VALUE, DESCRIPTION).set(bundle.getString("endpoint.config.property.value"));
            node.get(ATTRIBUTES, VALUE, REQUIRED).set(false);

            return node;
        }

        static ModelNode getEndpointConfigAddDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set(ADD);
            node.get(DESCRIPTION).set(bundle.getString("endpoint.config.add"));
            node.get(REQUEST_PROPERTIES).setEmptyObject();
            node.get(REPLY_PROPERTIES).setEmptyObject();
            return node;
        }

        static ModelNode getEndpointConfigFeatureAddDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set(ADD);
            node.get(DESCRIPTION).set(bundle.getString("endpoint.config.feature.add"));
            node.get(REQUEST_PROPERTIES).setEmptyObject();
            node.get(REPLY_PROPERTIES).setEmptyObject();
            return node;
        }

        static ModelNode getEndpointConfigPropertyAddDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set(ADD);
            node.get(DESCRIPTION).set(bundle.getString("endpoint.config.property.add"));
            node.get(REQUEST_PROPERTIES, VALUE, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, VALUE, DESCRIPTION).set(bundle.getString("endpoint.config.property.value"));
            node.get(REQUEST_PROPERTIES, VALUE, REQUIRED).set(false);
            node.get(REPLY_PROPERTIES).setEmptyObject();
            return node;
        }

        static ModelNode getEndpointConfigHandlerChainAddDescription(final Locale locale, final String handlerChainName) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set(ADD);
            node.get(DESCRIPTION).set(bundle.getString(handlerChainName));

            node.get(REQUEST_PROPERTIES, PROTOCOL_BINDINGS, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, PROTOCOL_BINDINGS, DESCRIPTION).set(bundle.getString("protocol.binding"));
            node.get(REQUEST_PROPERTIES, PROTOCOL_BINDINGS, REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, PROTOCOL_BINDINGS, NILLABLE).set(true);

            node.get(REQUEST_PROPERTIES, PORT_NAME_PATTERN, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, PORT_NAME_PATTERN, DESCRIPTION).set(bundle.getString("portname.pattern"));
            node.get(REQUEST_PROPERTIES, PORT_NAME_PATTERN, REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, PORT_NAME_PATTERN, NILLABLE).set(true);

            node.get(REQUEST_PROPERTIES, SERVICE_NAME_PATTERN, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, SERVICE_NAME_PATTERN, DESCRIPTION).set(bundle.getString("servicename.pattern"));
            node.get(REQUEST_PROPERTIES, SERVICE_NAME_PATTERN, REQUIRED).set(false);
            node.get(REQUEST_PROPERTIES, SERVICE_NAME_PATTERN, NILLABLE).set(true);

            node.get(REPLY_PROPERTIES).setEmptyObject();

            return node;
        }

        static ModelNode getEndpointConfigHandlerAddDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set(ADD);
            node.get(DESCRIPTION).set(bundle.getString("handler.name"));

            node.get(REQUEST_PROPERTIES, HANDLER_CLASS, TYPE).set(ModelType.STRING);
            node.get(REQUEST_PROPERTIES, HANDLER_CLASS, DESCRIPTION).set(bundle.getString("handler.class"));
            node.get(REQUEST_PROPERTIES, HANDLER_CLASS, REQUIRED).set(true);
            node.get(REQUEST_PROPERTIES, HANDLER_CLASS, NILLABLE).set(false);

            node.get(REPLY_PROPERTIES).setEmptyObject();

            return node;
        }

        static ModelNode getEndpointConfigRemoveDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set(REMOVE);
            node.get(DESCRIPTION).set(bundle.getString("endpoint.config.remove"));
            node.get(REQUEST_PROPERTIES).setEmptyObject();
            node.get(REPLY_PROPERTIES).setEmptyObject();
            return node;
        }

        static ModelNode getEndpointConfigFeatureRemoveDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set(REMOVE);
            node.get(DESCRIPTION).set(bundle.getString("endpoint.config.feature.remove"));
            node.get(REQUEST_PROPERTIES).setEmptyObject();
            node.get(REPLY_PROPERTIES).setEmptyObject();
            return node;
        }

        static ModelNode getEndpointConfigPropertyRemoveDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set(REMOVE);
            node.get(DESCRIPTION).set(bundle.getString("endpoint.config.property.remove"));
            node.get(REQUEST_PROPERTIES).setEmptyObject();
            node.get(REPLY_PROPERTIES).setEmptyObject();
            return node;
        }

        static ModelNode getSubsystemAdd(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode op = new ModelNode();

            op.get(OPERATION_NAME).set(ADD);
            op.get(DESCRIPTION).set(bundle.getString("ws.add"));

            op.get(REQUEST_PROPERTIES, MODIFY_WSDL_ADDRESS, DESCRIPTION).set(bundle.getString("modify.wsdl.address"));
            op.get(REQUEST_PROPERTIES, MODIFY_WSDL_ADDRESS, TYPE).set(ModelType.BOOLEAN);
            op.get(REQUEST_PROPERTIES, MODIFY_WSDL_ADDRESS, REQUIRED).set(true);

            op.get(REQUEST_PROPERTIES, WSDL_HOST, DESCRIPTION).set(bundle.getString("wsdl.host"));
            op.get(REQUEST_PROPERTIES, WSDL_HOST, TYPE).set(ModelType.STRING);
            op.get(REQUEST_PROPERTIES, WSDL_HOST, REQUIRED).set(true);

            op.get(REQUEST_PROPERTIES, WSDL_PORT, DESCRIPTION).set(bundle.getString("wsdl.port"));
            op.get(REQUEST_PROPERTIES, WSDL_PORT, TYPE).set(ModelType.INT);
            op.get(REQUEST_PROPERTIES, WSDL_PORT, REQUIRED).set(false);

            op.get(REQUEST_PROPERTIES, WSDL_SECURE_PORT, DESCRIPTION).set(bundle.getString("wsdl.secure.port"));
            op.get(REQUEST_PROPERTIES, WSDL_SECURE_PORT, TYPE).set(ModelType.INT);
            op.get(REQUEST_PROPERTIES, WSDL_SECURE_PORT, REQUIRED).set(false);

            op.get(REPLY_PROPERTIES).setEmptyObject();

            return op;
        }

        static ModelNode getSubsystemRemoveDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set(REMOVE);
            op.get(DESCRIPTION).set(bundle.getString("ws.remove"));
            op.get(REPLY_PROPERTIES).setEmptyObject();
            op.get(REQUEST_PROPERTIES).setEmptyObject();
            return op;
        }
    }

}
