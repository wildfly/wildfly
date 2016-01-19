/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.insights.extension;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 * @author <a href="jkinlaw@redhat.com">Josh Kinlaw</a>
 */
public class InsightsExtension implements Extension {

    public static final String ENABLED = "enabled";
    public static final String SCHEDULE_INTERVAL = "schedule-interval";
    public static final String RHNPW = "rhn-pw";
    public static final String RHNUID = "rhn-uid";
    public static final String PROXY_USER = "proxy-user";
    public static final String PROXY_PASSWORD = "proxy-password";
    public static final String PROXY_URL = "proxy-url";
    public static final String PROXY_PORT = "proxy-port";
    public static final String TYPE = "insights-type";
    public static final String INSIGHTS_ENDPOINT = "insights-endpoint";
    public static final String SYSTEM_ENDPOINT = "system-endpoint";
    public static final String URL = "url";
    public static final String USER_AGENT = "user-agent";
    public static final PathElement TYPE_PATH = PathElement.pathElement(TYPE);

    /**
     * The name space used for the {@code subsystem} element
     */
    public static final String NAMESPACE = "urn:org.jboss.as.insights:1.0";

    /**
     * The name of our subsystem within the model.
     */
    public static final String SUBSYSTEM_NAME = "insights";

    /**
     * Version of the subsystem used for registering
     */
    private static final int MAJOR_VERSION = 1;

    /**
     * The parser used for parsing our subsystem
     */
    private final InsightsSubsystemParser parser = new InsightsSubsystemParser();

    protected static final PathElement SUBSYSTEM_PATH = PathElement
            .pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    private static final String RESOURCE_NAME = InsightsExtension.class
            .getPackage().getName() + ".LocalDescriptions";

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        String prefix = SUBSYSTEM_NAME+ (keyPrefix == null ? "" : "." + keyPrefix);
        return new StandardResourceDescriptionResolver(prefix, RESOURCE_NAME, InsightsExtension.class.getClassLoader(), true, true);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE, parser);
    }

    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(
                SUBSYSTEM_NAME, ModelVersion.create(MAJOR_VERSION));
        final ManagementResourceRegistration registration = subsystem
                .registerSubsystemModel(InsightsSubsystemDefinition.INSTANCE);
        registration.registerOperationHandler(
                GenericSubsystemDescribeHandler.DEFINITION,
                GenericSubsystemDescribeHandler.INSTANCE);
        if (context.isRuntimeOnlyRegistrationValid()) {
            registration.registerOperationHandler(
                    InsightsRequestHandler.DEFINITION,
                    InsightsRequestHandler.INSTANCE);
        }
        subsystem.registerXMLElementWriter(parser);
    }
}
