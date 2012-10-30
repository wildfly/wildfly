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

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ResourceBuilder;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;

/**
 * The webservices extension.
 *
 * @author alessio.soldano@jboss.com
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="ema@redhat.com">Jim Ma</a>
 */
public final class WSExtension implements Extension {

    private static final PathElement ENDPOINT_PATH = PathElement.pathElement(ENDPOINT);
    private static final PathElement CLIENT_CONFIG_PATH = PathElement.pathElement(CLIENT_CONFIG);
    private static final PathElement ENDPOINT_CONFIG_PATH = PathElement.pathElement(ENDPOINT_CONFIG);
    private static final PathElement PROPERTY_PATH = PathElement.pathElement(PROPERTY);
    private static final PathElement PRE_HANDLER_CHAIN_PATH = PathElement.pathElement(PRE_HANDLER_CHAIN);
    private static final PathElement POST_HANDLER_CHAIN_PATH = PathElement.pathElement(POST_HANDLER_CHAIN);
    private static final PathElement HANDLER_PATH = PathElement.pathElement(HANDLER);
    public static final String SUBSYSTEM_NAME = "webservices";
    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    private static final String RESOURCE_NAME = WSExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 2;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

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
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);
        subsystem.registerXMLElementWriter(WSSubsystemWriter.getInstance());
        // ws subsystem
        ResourceBuilder propertyResource = ResourceBuilder.Factory.create(PROPERTY_PATH, getResourceDescriptionResolver(Constants.PROPERTY))
                .setAddOperation(PropertyAdd.INSTANCE)
                .setRemoveOperation(PropertyRemove.INSTANCE)
                .addReadWriteAttribute(Attributes.VALUE, null, new ReloadRequiredWriteAttributeHandler(Attributes.VALUE));


        ResourceBuilder handlerBuilder = ResourceBuilder.Factory.create(HANDLER_PATH, getResourceDescriptionResolver("handler"))
                .setAddOperation(HandlerAdd.INSTANCE)
                .setRemoveOperation(HandlerRemove.INSTANCE)
                .addReadWriteAttribute(Attributes.CLASS, null, new ReloadRequiredWriteAttributeHandler(Attributes.CLASS));

        ResourceBuilder preHandler = ResourceBuilder.Factory.create(PRE_HANDLER_CHAIN_PATH, getResourceDescriptionResolver(Constants.PRE_HANDLER_CHAIN))
                .setAddOperation(HandlerChainAdd.INSTANCE)
                .setRemoveOperation(HandlerChainRemove.INSTANCE)
                .addReadWriteAttribute(Attributes.PROTOCOL_BINDINGS, null, new ReloadRequiredWriteAttributeHandler(Attributes.PROTOCOL_BINDINGS))
                .pushChild(handlerBuilder).pop();

        ResourceBuilder postHandler = ResourceBuilder.Factory.create(POST_HANDLER_CHAIN_PATH, getResourceDescriptionResolver(Constants.POST_HANDLER_CHAIN))
                .setAddOperation(HandlerChainAdd.INSTANCE)
                .setRemoveOperation(HandlerChainRemove.INSTANCE)
                .addReadWriteAttribute(Attributes.PROTOCOL_BINDINGS, null, new ReloadRequiredWriteAttributeHandler(Attributes.PROTOCOL_BINDINGS))
                .pushChild(handlerBuilder).pop();

        ResourceDefinition subsystemResource = ResourceBuilder.Factory.createSubsystemRoot(SUBSYSTEM_PATH, getResourceDescriptionResolver(), WSSubsystemAdd.INSTANCE, WSSubsystemRemove.INSTANCE)
                .addReadWriteAttribute(Attributes.WSDL_HOST, null, new WSSubsystemAttributeChangeHandler(Attributes.WSDL_HOST))
                .addReadWriteAttribute(Attributes.WSDL_PORT, null, new WSSubsystemAttributeChangeHandler(Attributes.WSDL_PORT))
                .addReadWriteAttribute(Attributes.WSDL_SECURE_PORT, null, new WSSubsystemAttributeChangeHandler(Attributes.WSDL_SECURE_PORT))
                .addReadWriteAttribute(Attributes.MODIFY_WSDL_ADDRESS, null, new WSSubsystemAttributeChangeHandler(Attributes.MODIFY_WSDL_ADDRESS))
                .pushChild(ENDPOINT_CONFIG_PATH, EndpointConfigAdd.INSTANCE, EndpointConfigRemove.INSTANCE)
                    .pushChild(propertyResource).pop()
                    .pushChild(preHandler).pop()
                    .pushChild(postHandler).pop()
                .pop()
                .pushChild(CLIENT_CONFIG_PATH, ClientConfigAdd.INSTANCE, ClientConfigRemove.INSTANCE)
                    .pushChild(propertyResource).pop()
                    .pushChild(preHandler).pop()
                    .pushChild(postHandler).pop()
                .pop()
                .build();
        subsystem.registerSubsystemModel(subsystemResource);

        if (registerRuntimeOnly) {
            subsystem.registerDeploymentModel(ResourceBuilder.Factory.create(SUBSYSTEM_PATH, getResourceDescriptionResolver("deployment"))
                    .pushChild(ENDPOINT_PATH)
                    .addMetrics(WSEndpointMetrics.INSTANCE, WSEndpointMetrics.ATTRIBUTES).build());
        }
    }


    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.WEBSERVICES_1_0.getUriString(), WSSubsystemLegacyReader.getInstance());
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.WEBSERVICES_1_1.getUriString(), WSSubsystemReader.getInstance());
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.WEBSERVICES_1_2.getUriString(), WSSubsystemReader.getInstance());
    }

}
