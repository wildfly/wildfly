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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
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

        if (context.isRegisterTransformers()) {
            registerTransformers_1_1_0(subsystem);
        }
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
        final ResourceTransformationDescriptionBuilder subsystemRoot = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        // Discard valve
        subsystemRoot.discardChildResource(VALVE_PATH);

        // Reject expressions for configuration
        subsystemRoot.addChildResource(JSP_CONFIGURATION_PATH).getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, WebJSPDefinition.JSP_ATTRIBUTES);
        subsystemRoot.addChildResource(STATIC_RESOURCES_PATH).getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, WebStaticResources.STATIC_ATTRIBUTES);
        subsystemRoot.addChildResource(CONTAINER_PATH).getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, WebContainerDefinition.CONTAINER_ATTRIBUTES);

        final ResourceTransformationDescriptionBuilder connectorBuilder = subsystemRoot.addChildResource(CONNECTOR_PATH);
        connectorBuilder.getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, WebConnectorDefinition.CONNECTOR_ATTRIBUTES)
                .addRejectCheck(new RejectAttributeChecker.DefaultRejectAttributeChecker() {
                    @Override
                    protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
                        return attributeValue.isDefined();
                    }

                    @Override
                    public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
                        return WebMessages.MESSAGES.transformationVersion_1_1_0_JBPAPP_9314();
                    }
                }, Constants.VIRTUAL_SERVER)
                .setValueConverter(new AttributeConverter.DefaultAttributeConverter() {
                    @Override
                    protected void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
                        if (!attributeValue.isDefined()) {
                            attributeValue.set(defaultRedirectPort);
                        }
                    }
                }, WebConnectorDefinition.REDIRECT_PORT.getName())
                .end()
                .addOperationTransformationOverride(UNDEFINE_ATTRIBUTE_OPERATION)
                    .inheritResourceAttributeDefinitions() // although probably not necessary
                    .setCustomOperationTransformer(new OperationTransformer() {
                        @Override
                        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
                            final String attributeName = operation.require(NAME).asString();
                            if(WebConnectorDefinition.REDIRECT_PORT.getName().equals(attributeName)) {
                                final ModelNode transformed = new ModelNode();
                                transformed.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
                                transformed.get(OP_ADDR).set(address.toModelNode());
                                transformed.get(NAME).set(attributeName);
                                transformed.get(VALUE).set(defaultRedirectPort);
                                return new TransformedOperation(transformed, OperationResultTransformer.ORIGINAL_RESULT);
                            }
                            return new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
                        }
                    })
                ;

        //
        connectorBuilder.addChildRedirection(SSL_PATH, SSL_ALIAS).getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, WebSSLDefinition.SSL_ATTRIBUTES)
                .end();

        final ResourceTransformationDescriptionBuilder hostBuilder = subsystemRoot.addChildResource(HOST_PATH).getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, WebVirtualHostDefinition.DEFAULT_WEB_MODULE)
                .end();

        final ResourceTransformationDescriptionBuilder rewriteBuilder = hostBuilder.addChildResource(REWRITE_PATH).getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, WebReWriteDefinition.FLAGS, WebReWriteDefinition.PATTERN, WebReWriteDefinition.SUBSTITUTION)
                .end();

        rewriteBuilder.addChildResource(REWRITECOND_PATH).getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, WebReWriteConditionDefinition.ATTRIBUTES);

        hostBuilder.addChildRedirection(SSO_PATH, SSO_ALIAS).getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, WebSSODefinition.SSO_ATTRIBUTES)
                .end();

        final ResourceTransformationDescriptionBuilder accessLogBuilder = hostBuilder.addChildRedirection(ACCESS_LOG_PATH, ACCESS_LOG_ALIAS).getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, WebAccessLogDefinition.ACCESS_LOG_ATTRIBUTES)
                .end();

        accessLogBuilder.addChildRedirection(DIRECTORY_PATH, DIRECTORY_ALIAS);

        // Register
        TransformationDescription.Tools.register(subsystemRoot.build(), registration, ModelVersion.create(1, 1, 0));
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

}
