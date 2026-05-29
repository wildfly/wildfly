/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.util.EnumSet;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.Feature;
import org.jboss.as.controller.PersistentResourceXMLDescriptionWriter;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;


/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class UndertowExtension implements Extension {

    public static final String SUBSYSTEM_NAME = UndertowRootDefinition.REGISTRATION.getName();

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMappings(SUBSYSTEM_NAME, EnumSet.allOf(UndertowSubsystemSchema.class));
    }

    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, UndertowSubsystemModel.CURRENT.getVersion());
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new UndertowRootDefinition());
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE, false);

        final ManagementResourceRegistration deployments = subsystem.registerDeploymentModel(new DeploymentDefinition());
        deployments.registerSubModel(new DeploymentServletDefinition());
        deployments.registerSubModel(new DeploymentWebSocketDefinition());

        subsystem.registerXMLElementWriter(new PersistentResourceXMLDescriptionWriter(Feature.map(UndertowSubsystemSchema.CURRENT).get(context.getStability())));
    }
}