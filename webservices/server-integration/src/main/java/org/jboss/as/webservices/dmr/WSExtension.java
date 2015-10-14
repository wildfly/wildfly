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
package org.jboss.as.webservices.dmr;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.webservices.dmr.Constants.CLIENT_CONFIG;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_CONFIG;
import static org.jboss.as.webservices.dmr.Constants.HANDLER;
import static org.jboss.as.webservices.dmr.Constants.POST_HANDLER_CHAIN;
import static org.jboss.as.webservices.dmr.Constants.PRE_HANDLER_CHAIN;
import static org.jboss.as.webservices.dmr.Constants.PROPERTY;
import static org.jboss.as.webservices.dmr.Constants.WSDL_URI_SCHEME;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ResourceBuilder;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelType;

/**
 * The webservices extension.
 *
 * @author alessio.soldano@jboss.com
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="ema@redhat.com">Jim Ma</a>
 */
public final class WSExtension implements Extension {

    static final PathElement ENDPOINT_PATH = PathElement.pathElement(ENDPOINT);
    static final PathElement CLIENT_CONFIG_PATH = PathElement.pathElement(CLIENT_CONFIG);
    static final PathElement WSDL_URI_SCHEME_PATH = PathElement.pathElement(WSDL_URI_SCHEME);
    static final PathElement ENDPOINT_CONFIG_PATH = PathElement.pathElement(ENDPOINT_CONFIG);
    private static final PathElement PROPERTY_PATH = PathElement.pathElement(PROPERTY);
    static final PathElement PRE_HANDLER_CHAIN_PATH = PathElement.pathElement(PRE_HANDLER_CHAIN);
    static final PathElement POST_HANDLER_CHAIN_PATH = PathElement.pathElement(POST_HANDLER_CHAIN);
    static final PathElement HANDLER_PATH = PathElement.pathElement(HANDLER);
    public static final String SUBSYSTEM_NAME = "webservices";
    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    private static final String RESOURCE_NAME = WSExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(2, 0, 0);

    // attributes on the endpoint element
     static final AttributeDefinition ENDPOINT_WSDL = new SimpleAttributeDefinitionBuilder(
            Constants.ENDPOINT_WSDL, ModelType.STRING, false)
            .setStorageRuntime()
            .build();
    static final AttributeDefinition ENDPOINT_CLASS = new SimpleAttributeDefinitionBuilder(
            Constants.ENDPOINT_CLASS, ModelType.STRING, false)
            .setStorageRuntime()
            .build();
    static final AttributeDefinition ENDPOINT_CONTEXT = new SimpleAttributeDefinitionBuilder(
            Constants.ENDPOINT_CONTEXT, ModelType.STRING, false)
            .setStorageRuntime()
            .build();
    static final AttributeDefinition ENDPOINT_TYPE = new SimpleAttributeDefinitionBuilder(
            Constants.ENDPOINT_TYPE, ModelType.STRING, true)
            .setStorageRuntime()
            .build();
    static final AttributeDefinition ENDPOINT_NAME = new SimpleAttributeDefinitionBuilder(
            Constants.ENDPOINT_NAME, ModelType.STRING, false)
            .setStorageRuntime()
            .build();


    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, WSExtension.class.getClassLoader(), true, false);
    }

    @Override
    public void initialize(final ExtensionContext context) {
        final boolean registerRuntimeOnly = context.isRuntimeOnlyRegistrationValid();
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        subsystem.registerXMLElementWriter(WSSubsystemWriter.getInstance());
        // ws subsystem
        ResourceBuilder propertyResource = ResourceBuilder.Factory.create(PROPERTY_PATH, getResourceDescriptionResolver(Constants.PROPERTY))
                .setAddOperation(PropertyAdd.INSTANCE)
                .setRemoveOperation(ReloadRequiredRemoveStepHandler.INSTANCE)
                .addReadWriteAttribute(Attributes.VALUE, null, new ReloadRequiredWriteAttributeHandler(Attributes.VALUE));


        ResourceBuilder handlerBuilder = ResourceBuilder.Factory.create(HANDLER_PATH, getResourceDescriptionResolver(HANDLER))
                .setAddOperation(HandlerAdd.INSTANCE)
                .setRemoveOperation(ReloadRequiredRemoveStepHandler.INSTANCE)
                .addReadWriteAttribute(Attributes.CLASS, null, new ReloadRequiredWriteAttributeHandler(Attributes.CLASS));

        ResourceBuilder preHandler = ResourceBuilder.Factory.create(PRE_HANDLER_CHAIN_PATH, getResourceDescriptionResolver(Constants.PRE_HANDLER_CHAIN))
                .setAddOperation(HandlerChainAdd.INSTANCE)
                .setRemoveOperation(ReloadRequiredRemoveStepHandler.INSTANCE)
                .addReadWriteAttribute(Attributes.PROTOCOL_BINDINGS, null, new ReloadRequiredWriteAttributeHandler(Attributes.PROTOCOL_BINDINGS))
                .pushChild(handlerBuilder).pop();

        ResourceBuilder postHandler = ResourceBuilder.Factory.create(POST_HANDLER_CHAIN_PATH, getResourceDescriptionResolver(Constants.POST_HANDLER_CHAIN))
                .setAddOperation(HandlerChainAdd.INSTANCE)
                .setRemoveOperation(ReloadRequiredRemoveStepHandler.INSTANCE)
                .addReadWriteAttribute(Attributes.PROTOCOL_BINDINGS, null, new ReloadRequiredWriteAttributeHandler(Attributes.PROTOCOL_BINDINGS))
                .pushChild(handlerBuilder).pop();

        ResourceDefinition epConfigsDef = ResourceBuilder.Factory.create(ENDPOINT_CONFIG_PATH, getResourceDescriptionResolver(ENDPOINT_CONFIG))
                .setAddOperation(EndpointConfigAdd.INSTANCE)
                .setRemoveOperation(ReloadRequiredRemoveStepHandler.INSTANCE)
                .pushChild(propertyResource).pop()
                .pushChild(preHandler).pop()
                .pushChild(postHandler).pop()
                .build();

        ResourceDefinition clConfigsDef = ResourceBuilder.Factory.create(CLIENT_CONFIG_PATH, getResourceDescriptionResolver(CLIENT_CONFIG))
              .setAddOperation(ClientConfigAdd.INSTANCE)
              .setRemoveOperation(ReloadRequiredRemoveStepHandler.INSTANCE)
              .pushChild(propertyResource).pop()
              .pushChild(preHandler).pop()
              .pushChild(postHandler).pop()
              .build();

        ResourceDefinition subsystemResource = ResourceBuilder.Factory.createSubsystemRoot(SUBSYSTEM_PATH, getResourceDescriptionResolver(), WSSubsystemAdd.INSTANCE, WSSubsystemRemove.INSTANCE)
                .addReadWriteAttribute(Attributes.WSDL_HOST, null, new WSServerConfigAttributeHandler(Attributes.WSDL_HOST))
                .addReadWriteAttribute(Attributes.WSDL_PORT, null, new WSServerConfigAttributeHandler(Attributes.WSDL_PORT))
                .addReadWriteAttribute(Attributes.WSDL_SECURE_PORT, null, new WSServerConfigAttributeHandler(Attributes.WSDL_SECURE_PORT))
                .addReadWriteAttribute(Attributes.WSDL_URI_SCHEME, null, new WSServerConfigAttributeHandler(Attributes.WSDL_URI_SCHEME))
                .addReadWriteAttribute(Attributes.WSDL_PATH_REWRITE_RULE, null, new WSServerConfigAttributeHandler(Attributes.WSDL_PATH_REWRITE_RULE))
                .addReadWriteAttribute(Attributes.MODIFY_WSDL_ADDRESS, null, new WSServerConfigAttributeHandler(Attributes.MODIFY_WSDL_ADDRESS))
                .addReadWriteAttribute(Attributes.STATISTICS_ENABLED, null, new WSServerConfigAttributeHandler(Attributes.STATISTICS_ENABLED))
                .build();
        ManagementResourceRegistration subsystemRegistration = subsystem.registerSubsystemModel(subsystemResource);
        subsystemRegistration.registerSubModel(epConfigsDef);
        subsystemRegistration.registerSubModel(clConfigsDef);

        if (registerRuntimeOnly) {
            subsystem.registerDeploymentModel(ResourceBuilder.Factory.create(SUBSYSTEM_PATH, getResourceDescriptionResolver("deployment"))
                    .pushChild(ENDPOINT_PATH)
                    .addMetrics(WSEndpointMetrics.INSTANCE, WSEndpointMetrics.ATTRIBUTES)
                    .addReadOnlyAttribute(ENDPOINT_CLASS)
                    .addReadOnlyAttribute(ENDPOINT_CONTEXT)
                    .addReadOnlyAttribute(ENDPOINT_NAME)
                    .addReadOnlyAttribute(ENDPOINT_TYPE)
                    .addReadOnlyAttribute(ENDPOINT_WSDL)
                    .build());
        }

        if (context.isRegisterTransformers()) {
            registerTransformers1_1_0(subsystem);
            registerTransformers1_2_0(subsystem);
        }
    }


    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.WEBSERVICES_1_0.getUriString(), WSSubsystemLegacyReader.getInstance());
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.WEBSERVICES_1_1.getUriString(), WSSubsystem11Reader.getInstance());
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.WEBSERVICES_1_2.getUriString(), WSSubSystem12Reader.getInstance());
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.WEBSERVICES_2_0.getUriString(), WSSubSystem20Reader.getInstance());
    }

    private void registerTransformers1_1_0(SubsystemRegistration registration) {
        ModelVersion version = ModelVersion.create(1, 1, 0);

        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        builder.getAttributeBuilder().setDiscard(DiscardAttributeChecker.ALWAYS, Attributes.STATISTICS_ENABLED);
        builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, Attributes.SUBSYSTEM_ATTRIBUTES).end();
        builder.getAttributeBuilder().setDiscard(DiscardAttributeChecker.ALWAYS, Attributes.WSDL_URI_SCHEME);
        builder.rejectChildResource(CLIENT_CONFIG_PATH);

        ResourceTransformationDescriptionBuilder endpoint = builder.addChildResource(ENDPOINT_CONFIG_PATH);
        endpoint.addChildResource(PRE_HANDLER_CHAIN_PATH).getAttributeBuilder().addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, Attributes.PROTOCOL_BINDINGS).end();
        endpoint.addChildResource(POST_HANDLER_CHAIN_PATH).getAttributeBuilder().addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, Attributes.PROTOCOL_BINDINGS).end();

        TransformationDescription.Tools.register(builder.build(), registration, version);
    }

    private void registerTransformers1_2_0(SubsystemRegistration registration) {
        ModelVersion version = ModelVersion.create(1, 2, 0);
        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        builder.getAttributeBuilder().setDiscard(DiscardAttributeChecker.ALWAYS, Attributes.STATISTICS_ENABLED);
        builder.getAttributeBuilder().setDiscard(DiscardAttributeChecker.ALWAYS, Attributes.WSDL_URI_SCHEME);
        builder.getAttributeBuilder().setDiscard(DiscardAttributeChecker.ALWAYS, Attributes.WSDL_PATH_REWRITE_RULE);
        TransformationDescription.Tools.register(builder.build(), registration, version);
    }
}
