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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

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
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.dmr.ModelNode;

/**
 * The web extension.
 *
 * @author Emanuel Muckenhuber
 * @author Tomaz Cerar
 */
public class WebExtension implements Extension {
    public static final String SUBSYSTEM_NAME = "web";
    protected static final PathElement CONNECTOR_PATH = PathElement.pathElement(Constants.CONNECTOR);
    protected static final PathElement SSL_PATH = PathElement.pathElement(Constants.SSL, Constants.CONFIGURATION);
    protected static final PathElement HOST_PATH = PathElement.pathElement(Constants.VIRTUAL_SERVER);

    protected static final PathElement JSP_CONFIGURATION_PATH = PathElement.pathElement(Constants.CONFIGURATION, Constants.JSP_CONFIGURATION);
    protected static final PathElement STATIC_RESOURCES_PATH = PathElement.pathElement(Constants.CONFIGURATION, Constants.STATIC_RESOURCES);
    protected static final PathElement CONTAINER_PATH = PathElement.pathElement(Constants.CONFIGURATION, Constants.CONTAINER);

    protected static final PathElement ACCESS_LOG_PATH = PathElement.pathElement(Constants.ACCESS_LOG, Constants.CONFIGURATION);

    protected static final PathElement REWRITE_PATH = PathElement.pathElement(Constants.REWRITE);
    protected static final PathElement SSO_PATH = PathElement.pathElement(Constants.SSO, Constants.CONFIGURATION);
    protected static final PathElement DIRECTORY_PATH = PathElement.pathElement(Constants.DIRECTORY,Constants.CONFIGURATION);
    protected static final PathElement REWRITECOND_PATH = PathElement.pathElement(Constants.CONDITION);

    private static final String RESOURCE_NAME = WebExtension.class.getPackage().getName() + ".LocalDescriptions";

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        String prefix = SUBSYSTEM_NAME + (keyPrefix == null ? "" : "." + keyPrefix);
        return new StandardResourceDescriptionResolver(prefix, RESOURCE_NAME, WebExtension.class.getClassLoader(), true, false);
    }

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 1;
    private static final int MANAGEMENT_API_MICRO_VERSION = 1;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ExtensionContext context) {

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(WebDefinition.INSTANCE);
        registration.registerOperationHandler(DESCRIBE, GenericSubsystemDescribeHandler.INSTANCE, GenericSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        subsystem.registerXMLElementWriter(WebSubsystemParser.getInstance());

        // connectors
        final ManagementResourceRegistration connectors = registration.registerSubModel(WebConnectorDefinition.INSTANCE);

        final ManagementResourceRegistration ssl = connectors.registerSubModel(WebSSLDefinition.INSTANCE);

        //hosts
        final ManagementResourceRegistration hosts = registration.registerSubModel(WebVirtualHostDefinition.INSTANCE);

        // access-log.
        final ManagementResourceRegistration accesslog = hosts.registerSubModel(WebAccessLogDefinition.INSTANCE);

        // access-log.
        // the directory needs one level more
        accesslog.registerSubModel(WebAccessLogDirectoryDefinition.INSTANCE);

        // sso valve.
        hosts.registerSubModel(WebSSODefinition.INSTANCE);

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

        registerTransformers_1_1_0(subsystem);
    }

    private void registerTransformers_1_1_0(SubsystemRegistration registration) {

        final TransformersSubRegistration transformers = registration.registerModelTransformers(ModelVersion.create(1, 1, 0), ResourceTransformer.DEFAULT);

        TransformersSubRegistration connectors = transformers.registerSubResource(CONNECTOR_PATH);
        connectors.registerOperationTransformer(ADD, new OperationTransformer() {
            @Override
            public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation)
                    throws OperationFailedException {

                //Don't error on the way out, it might be ignored on the slave
                final boolean hasDefinedVirtualServer = operation.hasDefined(Constants.VIRTUAL_SERVER);
                return new TransformedOperation(operation, new OperationResultTransformer() {

                    @Override
                    public ModelNode transformResult(ModelNode result) {
                        if (!hasDefinedVirtualServer) {
                            return result;
                        }
                        if (result.get(OUTCOME).asString().equals(FAILED)) {
                            result.get(FAILURE_DESCRIPTION).set(WebMessages.MESSAGES.transformationVersion_1_1_0_JBPAPP_9314());
                        }
                        return result;
                    }
                });
            }
        });
        connectors.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, new OperationTransformer() {

            @Override
            public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation)
                    throws OperationFailedException {

                //Don't error on the way out, it might be ignored on the slave
                final boolean isVirtualServer = operation.get(NAME).asString().equals(Constants.VIRTUAL_SERVER);
                return new TransformedOperation(operation, new OperationResultTransformer() {

                    @Override
                    public ModelNode transformResult(ModelNode result) {
                        if (!isVirtualServer) {
                            return result;
                        }
                        if (result.get(OUTCOME).asString().equals(FAILED)) {
                            result.get(FAILURE_DESCRIPTION).set(WebMessages.MESSAGES.transformationVersion_1_1_0_JBPAPP_9314());
                        }
                        return result;
                    }
                });
                }
            });
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
    }

}
