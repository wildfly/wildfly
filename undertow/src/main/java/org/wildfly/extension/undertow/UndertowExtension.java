/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;


/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class UndertowExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "undertow";
    private static final String RESOURCE_NAME = UndertowExtension.class.getPackage().getName() + ".LocalDescriptions";

    public static StandardResourceDescriptionResolver getResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, UndertowExtension.class.getClassLoader(), true, false);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.UNDERTOW_3_1.getUriString(), UndertowSubsystemParser_3_1::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.UNDERTOW_4_0.getUriString(), UndertowSubsystemParser_4_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.UNDERTOW_5_0.getUriString(), UndertowSubsystemParser_5_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.UNDERTOW_6_0.getUriString(), UndertowSubsystemParser_6_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.UNDERTOW_7_0.getUriString(), UndertowSubsystemParser_7_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.UNDERTOW_8_0.getUriString(), UndertowSubsystemParser_8_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.UNDERTOW_9_0.getUriString(), UndertowSubsystemParser_9_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.UNDERTOW_10_0.getUriString(), UndertowSubsystemParser_10_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.UNDERTOW_11_0.getUriString(), UndertowSubsystemParser_11_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.UNDERTOW_12_0.getUriString(), UndertowSubsystemParser_12_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.UNDERTOW_13_0.getUriString(), UndertowSubsystemParser_13_0::new);
    }

    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, UndertowModel.CURRENT.getVersion());
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new UndertowRootDefinition());
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE, false);

        final ManagementResourceRegistration deployments = subsystem.registerDeploymentModel(new DeploymentDefinition());
        deployments.registerSubModel(new DeploymentServletDefinition());
        deployments.registerSubModel(new DeploymentWebSocketDefinition());

        subsystem.registerXMLElementWriter(UndertowSubsystemParser_13_0::new);
    }

}
