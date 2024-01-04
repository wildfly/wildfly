/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.datasources.agroal;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.msc.service.ServiceName;

/**
 * Defines an extension to provide DataSources based on the Agroal project
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class AgroalExtension implements Extension {

    /**
     * The name of the subsystem within the model
     */
    static final String SUBSYSTEM_NAME = "datasources-agroal";

    public static final ServiceName BASE_SERVICE_NAME = ServiceName.JBOSS.append(SUBSYSTEM_NAME);

    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(2, 0, 0);

    static final ParentResourceDescriptionResolver SUBSYSTEM_RESOLVER = new SubsystemResourceDescriptionResolver(SUBSYSTEM_NAME, AgroalExtension.class);

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, AgroalNamespace.AGROAL_1_0.getUriString(), AgroalSubsystemParser_1_0.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, AgroalNamespace.AGROAL_2_0.getUriString(), AgroalSubsystemParser_2_0.INSTANCE);
    }

    @Override
    public void initialize(ExtensionContext context) {
        SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new AgroalSubsystemDefinition());
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        subsystem.registerXMLElementWriter(AgroalSubsystemParser_2_0.INSTANCE);
    }
}
