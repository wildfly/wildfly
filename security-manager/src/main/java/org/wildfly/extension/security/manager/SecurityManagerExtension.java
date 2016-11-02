/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.security.manager;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.Map;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.security.manager.logging.SecurityManagerLogger;

/*
 * This class implements the security manager extension.
 *
 * @author <a href="sguilhen@jboss.com">Stefan Guilhen</a>
 */
public class SecurityManagerExtension implements Extension {

    public static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, Constants.SUBSYSTEM_NAME);
    protected static final String RESOURCE_NAME = SecurityManagerExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final int MANAGEMENT_API_MAJOR_VERSION = 3;
    private static final int MANAGEMENT_API_MINOR_VERSION = 0;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;
    private static final ModelVersion CURRENT_MODEL_VERSION =
            ModelVersion.create(MANAGEMENT_API_MAJOR_VERSION, MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);
    private static final ModelVersion EAP_7_0_0_MODEL_VERSION = ModelVersion.create(2, 0, 0);

    public static StandardResourceDescriptionResolver getResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(Constants.SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, SecurityManagerExtension.class.getClassLoader(), true, false);
    }

        @Override
    public void initialize(final ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(Constants.SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(SecurityManagerRootDefinition.INSTANCE);
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE, false);
        subsystem.registerXMLElementWriter(SecurityManagerSubsystemParser_3_0.INSTANCE);

        // register transformers for released EAP versions that include the security manager subsystem.
        registerTransformers_EAP_7_0_0(subsystem);
    }

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Constants.SUBSYSTEM_NAME, Namespace.SECURITY_MANAGER_1_0.getUriString(), SecurityManagerSubsystemParser_1_0.INSTANCE);
        context.setSubsystemXmlMapping(Constants.SUBSYSTEM_NAME, Namespace.SECURITY_MANAGER_3_0.getUriString(), SecurityManagerSubsystemParser_3_0.INSTANCE);
    }

    /**
     * Registers the transformers for JBoss EAP 7.0.0.
     *
     * @param subsystemRegistration a reference to the {@link SubsystemRegistration}.
     */
    private void registerTransformers_EAP_7_0_0(final SubsystemRegistration subsystemRegistration) {
        ResourceTransformationDescriptionBuilder builder = ResourceTransformationDescriptionBuilder.Factory.createSubsystemInstance();
        builder.addChildResource(DeploymentPermissionsResourceDefinition.DEPLOYMENT_PERMISSIONS_PATH).
                getAttributeBuilder().addRejectCheck(new RejectAttributeChecker.DefaultRejectAttributeChecker() {

            @Override
            protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode value, TransformationContext context) {
                // reject the maximum set if it is defined and empty as that would result in complete incompatible policies
                // being used in nodes running earlier versions of the subsystem.
                if (value.isDefined() && value.asList().isEmpty())
                    return true;
                return false;
            }

            @Override
            public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
                return SecurityManagerLogger.ROOT_LOGGER.rejectedEmptyMaximumSet();
            }
        }, DeploymentPermissionsResourceDefinition.MAXIMUM_PERMISSIONS);
        TransformationDescription.Tools.register(builder.build(), subsystemRegistration, EAP_7_0_0_MODEL_VERSION);
    }
}
