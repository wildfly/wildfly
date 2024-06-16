/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.iiop.openjdk;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * <p>
 * The IIOP extension implementation.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class IIOPExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "iiop-openjdk";

    protected static final PathElement PATH_SUBSYSTEM = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);

    static final ParentResourceDescriptionResolver SUBSYSTEM_RESOLVER = new SubsystemResourceDescriptionResolver(SUBSYSTEM_NAME, IIOPExtension.class);

    static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(4);
    static final ModelVersion VERSION_3_0 = ModelVersion.create(3);
    static final ModelVersion VERSION_2_1 = ModelVersion.create(2, 1);
    static final ModelVersion VERSION_2 = ModelVersion.create(2, 0, 0);
    static final ModelVersion VERSION_1 = ModelVersion.create(1);

    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        final ManagementResourceRegistration subsystemRegistration = subsystem.registerSubsystemModel(new IIOPRootDefinition());
        subsystemRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
        subsystem.registerXMLElementWriter(new IIOPSubsystemParser_3_0());

    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME,Namespace.IIOP_OPENJDK_1_0.getUriString(), IIOPSubsystemParser_1::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME,Namespace.IIOP_OPENJDK_2_0.getUriString(), IIOPSubsystemParser_2_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME,Namespace.IIOP_OPENJDK_2_1.getUriString(), IIOPSubsystemParser_2_1::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME,Namespace.IIOP_OPENJDK_3_0.getUriString(), IIOPSubsystemParser_3_0::new);
    }

}
