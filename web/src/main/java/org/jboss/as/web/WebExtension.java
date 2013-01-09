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

package org.jboss.as.web;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.AliasEntry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.AliasOperationTransformer;
import org.jboss.as.controller.transform.AliasOperationTransformer.AddressTransformer;
import org.jboss.as.controller.transform.OperationRejectionPolicy;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.RejectExpressionValuesChainedTransformer;
import org.jboss.as.controller.transform.RejectExpressionValuesTransformer;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.as.controller.transform.chained.ChainedOperationTransformer;
import org.jboss.as.controller.transform.chained.ChainedResourceTransformer;
import org.jboss.dmr.ModelNode;

/**
 * The web extension.
 *
 * @author Emanuel Muckenhuber
 * @author Tomaz Cerar
 */
public class WebExtension implements Extension {
    public static final String SUBSYSTEM_NAME = "web";
    public static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    protected static final PathElement CONNECTOR_PATH = PathElement.pathElement(Constants.CONNECTOR);

    protected static final PathElement SSL_PATH = PathElement.pathElement(Constants.CONFIGURATION, Constants.SSL);
    protected static final PathElement SSL_ALIAS = PathElement.pathElement(Constants.SSL, Constants.CONFIGURATION);

    protected static final PathElement HOST_PATH = PathElement.pathElement(Constants.VIRTUAL_SERVER);

    protected static final PathElement JSP_CONFIGURATION_PATH = PathElement.pathElement(Constants.CONFIGURATION, Constants.JSP_CONFIGURATION);
    protected static final PathElement STATIC_RESOURCES_PATH = PathElement.pathElement(Constants.CONFIGURATION, Constants.STATIC_RESOURCES);
    protected static final PathElement CONTAINER_PATH = PathElement.pathElement(Constants.CONFIGURATION, Constants.CONTAINER);

    protected static final PathElement ACCESS_LOG_PATH = PathElement.pathElement(Constants.CONFIGURATION, Constants.ACCESS_LOG);
    protected static final PathElement ACCESS_LOG_ALIAS = PathElement.pathElement(Constants.ACCESS_LOG, Constants.CONFIGURATION);

    protected static final PathElement REWRITE_PATH = PathElement.pathElement(Constants.REWRITE);

    protected static final PathElement SSO_PATH = PathElement.pathElement(Constants.CONFIGURATION, Constants.SSO);
    protected static final PathElement SSO_ALIAS = PathElement.pathElement(Constants.SSO, Constants.CONFIGURATION);

    protected static final PathElement DIRECTORY_PATH = PathElement.pathElement(Constants.SETTING, Constants.DIRECTORY);
    protected static final PathElement DIRECTORY_ALIAS = PathElement.pathElement(Constants.DIRECTORY, Constants.CONFIGURATION);

    protected static final PathElement REWRITECOND_PATH = PathElement.pathElement(Constants.CONDITION);

    protected static final PathElement VALVE_PATH = PathElement.pathElement(Constants.VALVE);

    protected static final PathElement PARAM = PathElement.pathElement(Constants.PARAM);

    private static final String RESOURCE_NAME = WebExtension.class.getPackage().getName() + ".LocalDescriptions";

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        String prefix = SUBSYSTEM_NAME + (keyPrefix == null ? "" : "." + keyPrefix);
        return new StandardResourceDescriptionResolver(prefix, RESOURCE_NAME, WebExtension.class.getClassLoader(), true, false);
    }

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 2;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ExtensionContext context) {

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(WebDefinition.INSTANCE);
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
        subsystem.registerXMLElementWriter(WebSubsystemParser.getInstance());

        // connectors
        final ManagementResourceRegistration connectors = registration.registerSubModel(WebConnectorDefinition.INSTANCE);

        final ManagementResourceRegistration ssl = connectors.registerSubModel(WebSSLDefinition.INSTANCE);
        connectors.registerAlias(SSL_ALIAS, new StandardWebExtensionAliasEntry(ssl));

        //hosts
        final ManagementResourceRegistration hosts = registration.registerSubModel(WebVirtualHostDefinition.INSTANCE);

        // access-log.
        final ManagementResourceRegistration accesslog = hosts.registerSubModel(WebAccessLogDefinition.INSTANCE);
        hosts.registerAlias(ACCESS_LOG_ALIAS, new StandardWebExtensionAliasEntry(accesslog));

        // access-log.
        // the directory needs one level more
        final ManagementResourceRegistration accessLogDir = accesslog.registerSubModel(WebAccessLogDirectoryDefinition.INSTANCE);
        accesslog.registerAlias(DIRECTORY_ALIAS, new StandardWebExtensionAliasEntry(accessLogDir));

        // sso valve.
        final ManagementResourceRegistration sso = hosts.registerSubModel(WebSSODefinition.INSTANCE);
        hosts.registerAlias(SSO_ALIAS, new StandardWebExtensionAliasEntry(sso));

        // rewrite valve.
        final ManagementResourceRegistration rewrite = hosts.registerSubModel(WebReWriteDefinition.INSTANCE);

        // the condition needs one level more
        rewrite.registerSubModel(WebReWriteConditionDefinition.INSTANCE);

        // configuration=jsp
        registration.registerSubModel(WebJSPDefinition.INSTANCE);

        // configuration=resources
        registration.registerSubModel(WebStaticResources.INSTANCE);

        // configuration=container
        registration.registerSubModel(WebContainerDefinition.INSTANCE);


        //deployment
        final ManagementResourceRegistration deployments = subsystem.registerDeploymentModel(WebDeploymentDefinition.INSTANCE);
        deployments.registerSubModel(WebDeploymentServletDefinition.INSTANCE);

        // Global valve.
        registration.registerSubModel(WebValveDefinition.INSTANCE);
        registerTransformers_1_1_0(subsystem);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (Namespace ns : Namespace.values()) {
            if (ns.getUriString() != null) {
                context.setSubsystemXmlMapping(SUBSYSTEM_NAME, ns.getUriString(), WebSubsystemParser.getInstance());
            }
        }
        context.setProfileParsingCompletionHandler(new DefaultJsfProfileCompletionHandler());
    }

    private void registerTransformers_1_1_0(SubsystemRegistration registration) {

        final int defaultRedirectPort = 443;

        final TransformersSubRegistration transformers = registration.registerModelTransformers(ModelVersion.create(1, 1, 0), ResourceTransformer.DEFAULT);
        transformers.registerSubResource(VALVE_PATH, true);
        // configuration
        rejectExpressions(transformers, JSP_CONFIGURATION_PATH, WebJSPDefinition.JSP_ATTRIBUTES);
        rejectExpressions(transformers, STATIC_RESOURCES_PATH, WebStaticResources.STATIC_ATTRIBUTES);

        // Connector
        final RejectExpressionValuesChainedTransformer reject = new RejectExpressionValuesChainedTransformer(WebConnectorDefinition.CONNECTOR_ATTRIBUTES);
        final TransformersSubRegistration connectors = transformers.registerSubResource(CONNECTOR_PATH, new ChainedResourceTransformer(reject));
        connectors.registerOperationTransformer(ADD, new ChainedOperationTransformer(new OperationTransformer() {
            @Override
            public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation)
                    throws OperationFailedException {

                final ModelNode transformedOperation;
                if (!operation.hasDefined(WebConnectorDefinition.REDIRECT_PORT.getName())) {
                    // AS7-5871 send the correct default value
                    transformedOperation = operation.clone();
                    transformedOperation.get(WebConnectorDefinition.REDIRECT_PORT.getName()).set(defaultRedirectPort);
                } else {
                    transformedOperation = operation;
                }

                // Reject if it does not get ignored on the slave
                final boolean hasDefinedVirtualServer = operation.hasDefined(Constants.VIRTUAL_SERVER);
                return new TransformedOperation(transformedOperation, new OperationRejectionPolicy() {

                    @Override
                    public boolean rejectOperation(ModelNode preparedResult) {
                        return hasDefinedVirtualServer;
                    }

                    @Override
                    public String getFailureDescription() {
                        return WebMessages.MESSAGES.transformationVersion_1_1_0_JBPAPP_9314();
                    }

                }, OperationResultTransformer.ORIGINAL_RESULT);
            }
        }, reject));
        connectors.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, new ChainedOperationTransformer(new OperationTransformer() {

            @Override
            public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation)
                    throws OperationFailedException {

                final String attributeName = operation.get(NAME).asString();
                final ModelNode transformedOperation;
                if (WebConnectorDefinition.REDIRECT_PORT.getName().equals(attributeName) && !operation.hasDefined(VALUE)) {
                    // AS7-5871 send the correct default value
                    transformedOperation = operation.clone();
                    transformedOperation.get(VALUE).set(defaultRedirectPort);
                } else {
                    transformedOperation = operation;
                }
                // Reject if it does not get ignored on the slave
                final boolean isVirtualServer = operation.get(NAME).asString().equals(Constants.VIRTUAL_SERVER);
                return new TransformedOperation(transformedOperation, new OperationRejectionPolicy() {

                    @Override
                    public boolean rejectOperation(ModelNode preparedResult) {
                        return isVirtualServer;
                    }

                    @Override
                    public String getFailureDescription() {
                        return WebMessages.MESSAGES.transformationVersion_1_1_0_JBPAPP_9314();
                    }

                }, OperationResultTransformer.ORIGINAL_RESULT);
            }
        }, reject.getWriteAttributeTransformer()));

        connectors.registerOperationTransformer(UNDEFINE_ATTRIBUTE_OPERATION, new OperationTransformer() {

            @Override
            public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation)
                    throws OperationFailedException {

                final String attributeName = operation.get(NAME).asString();
                final ModelNode transformedOperation;
                if (WebConnectorDefinition.REDIRECT_PORT.getName().equals(attributeName)) {
                    // AS7-5871 send the correct default value
                    transformedOperation = operation.clone();
                    transformedOperation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
                    transformedOperation.get(VALUE).set(defaultRedirectPort);
                } else {
                    transformedOperation = operation;
                }
                return new TransformedOperation(transformedOperation, OperationResultTransformer.ORIGINAL_RESULT);
            }
        });

        // virtual-host
        final TransformersSubRegistration virtualHost = rejectExpressions(transformers, HOST_PATH, WebVirtualHostDefinition.DEFAULT_WEB_MODULE);
        rejectExpressions(virtualHost, SSL_PATH, WebSSLDefinition.SSL_ATTRIBUTES);

        final TransformersSubRegistration rewritePath = rejectExpressions(virtualHost, REWRITE_PATH, WebReWriteDefinition.FLAGS, WebReWriteDefinition.PATTERN, WebReWriteDefinition.SUBSTITUTION);
        rejectExpressions(rewritePath, REWRITECOND_PATH, WebReWriteConditionDefinition.FLAGS, WebReWriteConditionDefinition.PATTERN);


        // Aliases
        connectors.registerSubResource(SSL_PATH, AliasOperationTransformer.replaceLastElement(SSL_ALIAS));
        TransformersSubRegistration virtualServer = transformers.registerSubResource(HOST_PATH);
        //todo this is broken atm
        //TransformersSubRegistration sso = rejectExpressions(virtualHost, SSO_PATH, AliasOperationTransformer.replaceLastElement(SSO_ALIAS), WebSSODefinition.SSO_ATTRIBUTES);
        //TransformersSubRegistration accessLog = rejectExpressions(virtualHost, ACCESS_LOG_PATH, AliasOperationTransformer.replaceLastElement(ACCESS_LOG_ALIAS), WebAccessLogDefinition.ACCESS_LOG_ATTRIBUTES);
        TransformersSubRegistration sso = virtualServer.registerSubResource(SSO_PATH, AliasOperationTransformer.replaceLastElement(SSO_ALIAS));
        TransformersSubRegistration accessLog = virtualServer.registerSubResource(ACCESS_LOG_PATH, AliasOperationTransformer.replaceLastElement(ACCESS_LOG_ALIAS));

        accessLog.registerSubResource(DIRECTORY_PATH, AliasOperationTransformer.create(new AddressTransformer() {
            @Override
            public PathAddress transformAddress(PathAddress address) {
                PathAddress copy = PathAddress.EMPTY_ADDRESS;
                for (PathElement element : address) {
                    if (element.getKey().equals(Constants.CONFIGURATION)) {
                        copy = copy.append(ACCESS_LOG_ALIAS);
                    } else if (element.getKey().equals(Constants.SETTING)) {
                        copy = copy.append(DIRECTORY_ALIAS);
                    } else {
                        copy = copy.append(element);
                    }
                }
                return copy;
            }
        }));
    }

    private static class StandardWebExtensionAliasEntry extends AliasEntry {
        public StandardWebExtensionAliasEntry(ManagementResourceRegistration target) {
            super(target);
        }

        @Override
        public PathAddress convertToTargetAddress(PathAddress addr) {
            final PathAddress targetAddress = getTargetAddress();
            List<PathElement> list = new ArrayList<PathElement>();
            int i = 0;
            for (PathElement element : addr) {
                String key = element.getKey();
                try {
                    if (i < targetAddress.size() && (key.equals(Constants.SSL) || key.equals(Constants.SSO) || key.equals(Constants.ACCESS_LOG) || key.equals(Constants.DIRECTORY))) {
                        list.add(targetAddress.getElement(i));
                    } else {
                        list.add(element);
                    }
                    i++;
                } catch (Exception e) {
                    throw new RuntimeException("Bad " + addr + " " + targetAddress);
                }
            }
            return PathAddress.pathAddress(list);
        }
    }

    private static TransformersSubRegistration rejectExpressions(final TransformersSubRegistration parent, final PathElement path, final AttributeDefinition... definitions) {
        return rejectExpressions(parent, path, getKeys(definitions));
    }

    private static TransformersSubRegistration rejectExpressions(final TransformersSubRegistration parent, final PathElement path, final Set<String> expressionKeys) {
        final RejectExpressionValuesTransformer operationTransformer = new RejectExpressionValuesTransformer(expressionKeys);
        final TransformersSubRegistration registration = parent.registerSubResource(path, operationTransformer.getResourceTransformer());
        registration.registerOperationTransformer(ADD, operationTransformer);
        registration.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, operationTransformer.getWriteAttributeTransformer());
        return registration;
    }

    private static Set<String> getKeys(final AttributeDefinition... definitions) {
        final Set<String> keys = new HashSet<String>();
        for (final AttributeDefinition definition : definitions) {
            if (definition.isAllowExpression()) {
                keys.add(definition.getName());
            }
        }
        return keys;
    }
}
