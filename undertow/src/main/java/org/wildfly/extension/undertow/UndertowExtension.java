/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;


/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class UndertowExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "undertow";
    public static final PathElement PATH_ERROR_PAGES = PathElement.pathElement(Constants.CONFIGURATION, Constants.ERROR_PAGE);
    public static final PathElement PATH_HANDLERS = PathElement.pathElement(Constants.CONFIGURATION, Constants.HANDLER);
    public static final PathElement PATH_FILTERS = PathElement.pathElement(Constants.CONFIGURATION, Constants.FILTER);
    protected static final PathElement PATH_JSP = PathElement.pathElement(Constants.SETTING, Constants.JSP);
    protected static final PathElement PATH_SESSION_COOKIE = PathElement.pathElement(Constants.SETTING, Constants.SESSION_COOKIE);
    protected static final PathElement PATH_PERSISTENT_SESSIONS = PathElement.pathElement(Constants.SETTING, Constants.PERSISTENT_SESSIONS);
    protected static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    protected static final PathElement AJP_LISTENER_PATH = PathElement.pathElement(Constants.AJP_LISTENER);
    protected static final PathElement HOST_PATH = PathElement.pathElement(Constants.HOST);
    protected static final PathElement HTTP_LISTENER_PATH = PathElement.pathElement(Constants.HTTP_LISTENER);
    protected static final PathElement HTTPS_LISTENER_PATH = PathElement.pathElement(Constants.HTTPS_LISTENER);
    protected static final PathElement PATH_SERVLET_CONTAINER = PathElement.pathElement(Constants.SERVLET_CONTAINER);
    protected static final PathElement PATH_BUFFER_CACHE = PathElement.pathElement(Constants.BUFFER_CACHE);
    protected static final PathElement PATH_LOCATION = PathElement.pathElement(Constants.LOCATION);
    protected static final PathElement SERVER_PATH = PathElement.pathElement(Constants.SERVER);
    protected static final PathElement PATH_ACCESS_LOG = PathElement.pathElement(Constants.SETTING, Constants.ACCESS_LOG);
    protected static final PathElement PATH_SSO = PathElement.pathElement(Constants.SETTING, Constants.SINGLE_SIGN_ON);
    public static final PathElement PATH_FILTER_REF = PathElement.pathElement(Constants.FILTER_REF);
    private static final String RESOURCE_NAME = UndertowExtension.class.getPackage().getName() + ".LocalDescriptions";
    static final AccessConstraintDefinition LISTENER_CONSTRAINT = new SensitiveTargetAccessConstraintDefinition(
                    new SensitivityClassification(SUBSYSTEM_NAME, "web-connector", false, false, false));

    public static StandardResourceDescriptionResolver getResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, UndertowExtension.class.getClassLoader(), true, false);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.UNDERTOW_1_0.getUriString(), UndertowSubsystemParser_1_0.INSTANCE);
    }

    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, 1, 0, 0);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(UndertowRootDefinition.INSTANCE);
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE, false);

        final ManagementResourceRegistration deployments = subsystem.registerDeploymentModel(DeploymentDefinition.INSTANCE);
        deployments.registerSubModel(DeploymentServletDefinition.INSTANCE);

        subsystem.registerXMLElementWriter(UndertowSubsystemParser_1_0.INSTANCE);
    }


}
