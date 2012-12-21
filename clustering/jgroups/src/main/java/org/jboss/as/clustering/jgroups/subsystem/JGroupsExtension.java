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
package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import org.jboss.as.clustering.jgroups.LogFactory;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.RejectExpressionValuesTransformer;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jgroups.Global;

/**
 * Registers the JGroups subsystem.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class JGroupsExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "jgroups";
    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);
    public static final String RESOURCE_NAME = JGroupsExtension.class.getPackage().getName() + "." +"LocalDescriptions";

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 2;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    // Temporary workaround for JGRP-1475
    // Configure JGroups to use jboss-logging.
    static {
        PrivilegedAction<Void> action = new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                if (System.getProperty(Global.CUSTOM_LOG_FACTORY) == null) {
                    System.setProperty(Global.CUSTOM_LOG_FACTORY, LogFactory.class.getName());
                }
                return null;
            }
        };
        AccessController.doPrivileged(action);
    }

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
           StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
           for (String kp : keyPrefix) {
               prefix.append('.').append(kp);
           }
           return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, JGroupsExtension.class.getClassLoader(), true, false);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.Extension#initialize(org.jboss.as.controller.ExtensionContext)
     */
    @Override
    public void initialize(ExtensionContext context) {

        // IMPORTANT: Management API version != xsd version! Not all Management API changes result in XSD changes
        SubsystemRegistration registration = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);

        final boolean registerRuntimeOnly = context.isRuntimeOnlyRegistrationValid();
        final ManagementResourceRegistration subsystem = registration.registerSubsystemModel(new JGroupsSubsystemRootResource());
        subsystem.registerOperationHandler(JGroupsSubsystemDescribe.DEFINITON,JGroupsSubsystemDescribe.INSTANCE);

        subsystem.registerSubModel(new StackResource(registerRuntimeOnly));
        registration.registerXMLElementWriter(new JGroupsSubsystemXMLWriter());
        // Register the model transformers
        registerTransformers(registration);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.Extension#initializeParsers(org.jboss.as.controller.parsing.ExtensionParsingContext)
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (Namespace namespace: Namespace.values()) {
            XMLElementReader<List<ModelNode>> reader = namespace.getXMLReader();
            if (reader != null) {
                context.setSubsystemXmlMapping(SUBSYSTEM_NAME, namespace.getUri(), reader);
            }
        }
    }

    // Transformation

    /**
     * Register the transformers for older model versions.
     *
     * @param subsystem the subsystems registration
     */
    private static void registerTransformers(final SubsystemRegistration subsystem) {
        // Transformations to the 1.1.0 Model
        // - we need to reject expressions for transport (and similarly for protocol properties) for these operations
        //   transport=TRANSPORT/property=<name>:add(value=<value>)
        //   transport=TRANSPORT/property=<name>:write-attribute(name=value, value=<value>)
        //   transport=TRANSPORT:add(...,properties=<list of properties>)

        final ModelVersion version110 = ModelVersion.create(1, 1, 0);
        final RejectExpressionValuesTransformer TRANSFORMER = new RejectExpressionValuesTransformer(PropertyResource.VALUE,
                TransportResource.PROPERTIES, ProtocolResource.PROPERTIES);

        final TransformersSubRegistration registration = subsystem.registerModelTransformers(version110, ResourceTransformer.DEFAULT);
        final TransformersSubRegistration stack = registration.registerSubResource(StackResource.STACK_PATH);

        // reject expressions for transport properties, for the add and write-attribute op
        final TransformersSubRegistration transport = stack.registerSubResource(TransportResource.TRANSPORT_PATH) ;
        transport.registerOperationTransformer(ADD, TRANSFORMER);
        final TransformersSubRegistration transport_property = transport.registerSubResource(PropertyResource.PROPERTY_PATH) ;
        transport_property.registerOperationTransformer(ADD, TRANSFORMER);
        transport_property.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, TRANSFORMER.getWriteAttributeTransformer());

        // reject expressions for transport properties, for the add and write-attribute op
        final TransformersSubRegistration protocol = stack.registerSubResource(ProtocolResource.PROTOCOL_PATH) ;
        protocol.registerOperationTransformer(ADD, TRANSFORMER);
        final TransformersSubRegistration protocol_property = protocol.registerSubResource(PropertyResource.PROPERTY_PATH) ;
        protocol_property.registerOperationTransformer(ADD, TRANSFORMER);
        protocol_property.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, TRANSFORMER.getWriteAttributeTransformer());

    }

}
