/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jacorb;

import static org.jboss.as.jacorb.JacORBSubsystemConstants.IDENTITY;
import static org.jboss.as.jacorb.JacORBSubsystemConstants.SECURITY;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DeprecatedResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.extension.AbstractLegacyExtension;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.as.jacorb.logging.JacORBLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * <p>
 * The JacORB legacy subsystem extension implementation.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class JacORBExtension extends AbstractLegacyExtension {

    private static final JacORBSubsystemParser PARSER = JacORBSubsystemParser.INSTANCE;

    public static final String SUBSYSTEM_NAME = "jacorb";

    private static final String RESOURCE_NAME = JacORBExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(1, 4, 0);
    static final ModelVersion DEPRECATED_SINCE = ModelVersion.create(1, 4, 0);

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new DeprecatedResourceDescriptionResolver(SUBSYSTEM_NAME, prefix.toString(), RESOURCE_NAME, JacORBExtension.class.getClassLoader(), true, false);
    }

    private static final String extensionName = "org.jboss.as.jacorb";

    public JacORBExtension() {
        super(extensionName, SUBSYSTEM_NAME);
    }

    @Override
    protected Set<ManagementResourceRegistration> initializeLegacyModel(final ExtensionContext context) {
        final SubsystemRegistration subsystemRegistration = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);

        subsystemRegistration.registerXMLElementWriter(JacORBSubsystemParser.INSTANCE);

        final ManagementResourceRegistration subsystem =
                subsystemRegistration.registerSubsystemModel(JacORBSubsystemResource.INSTANCE);

        if (context.isRegisterTransformers()) {
            // Register the model transformers
            registerTransformers_1_1(subsystemRegistration);
            registerTransformers_1_2(subsystemRegistration);
            registerTransformers_1_3(subsystemRegistration);
        }

        return Collections.singleton(subsystem);
    }

    @Override
    public void initializeLegacyParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, JacORBSubsystemParser.Namespace.JacORB_1_0.getUriString(), PARSER);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, JacORBSubsystemParser.Namespace.JacORB_1_1.getUriString(), PARSER);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, JacORBSubsystemParser.Namespace.JacORB_1_2.getUriString(), PARSER);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, JacORBSubsystemParser.Namespace.JacORB_1_3.getUriString(), PARSER);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, JacORBSubsystemParser.Namespace.JacORB_1_4.getUriString(), PARSER);
    }

    /**
     * Register the transformers for the 1.1.0 version.
     *
     * @param subsystem the subsystems registration
     */
    protected static void registerTransformers_1_1(final SubsystemRegistration subsystem) {
        final Set<String> expressionKeys = new HashSet<String>();
        for(final AttributeDefinition def : JacORBSubsystemDefinitions.ATTRIBUTES_BY_NAME.values()) {
            if(def.isAllowExpression()) {
                expressionKeys.add(def.getName());
            }
        }
        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.DEFINED,
                JacORBSubsystemDefinitions.IOR_TRANSPORT_ATTRIBUTES.toArray(new AttributeDefinition[0]));
        builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.DEFINED,
                JacORBSubsystemDefinitions.IOR_AS_CONTEXT_ATTRIBUTES.toArray(new AttributeDefinition[0]));
        builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.DEFINED,
                JacORBSubsystemDefinitions.IOR_SAS_CONTEXT_ATTRIBUTES.toArray(new AttributeDefinition[0]));
        builder.getAttributeBuilder()
            .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, expressionKeys.toArray(new String[expressionKeys.size()]))
            .addRejectCheck(new RejectAttributeChecker.DefaultRejectAttributeChecker() {

                @Override
                public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
                    return JacORBLogger.ROOT_LOGGER.cannotUseSecurityClient();
                }

                @Override
                protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue,
                        TransformationContext context) {
                    return attributeValue.getType() == ModelType.STRING && attributeValue.asString().equals(JacORBSubsystemConstants.CLIENT);
                }
            }, SECURITY)
            .setValueConverter(new AttributeConverter.DefaultAttributeConverter() {

                @Override
                protected void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue,
                        TransformationContext context) {
                    final String security = attributeValue.asString();
                    //security=IDENTITY in the new model == security=ON in the old model
                    if (security.equals(IDENTITY)) {
                        attributeValue.set("on");
                    }
                }
            }, SECURITY)
            .end();
        TransformationDescription.Tools.register(builder.build(), subsystem, ModelVersion.create(1, 1, 0));
    }

    /**
     * Register the transformers for the 1.2.0 version.
     *
     * @param subsystem the subsystems registration
     */
    protected static void registerTransformers_1_2(final SubsystemRegistration subsystem) {
        ResourceTransformationDescriptionBuilder builder = ResourceTransformationDescriptionBuilder.Factory.createSubsystemInstance();
        builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.DEFINED,
                JacORBSubsystemDefinitions.IOR_TRANSPORT_ATTRIBUTES.toArray(new AttributeDefinition[0]));
        builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.DEFINED,
                JacORBSubsystemDefinitions.IOR_AS_CONTEXT_ATTRIBUTES.toArray(new AttributeDefinition[0]));
        builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.DEFINED,
                JacORBSubsystemDefinitions.IOR_SAS_CONTEXT_ATTRIBUTES.toArray(new AttributeDefinition[0]));
        TransformationDescription.Tools.register(builder.build(), subsystem, ModelVersion.create(1, 2, 0));
    }

    /**
     * Register the transformers for the 1.3.0 version.
     *
     * @param subsystem the subsystems registration
     */
    protected static void registerTransformers_1_3(final SubsystemRegistration subsystem) {
        ResourceTransformationDescriptionBuilder builder = ResourceTransformationDescriptionBuilder.Factory
                .createSubsystemInstance();
        builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.DEFINED,
                JacORBSubsystemDefinitions.IOR_TRANSPORT_ATTRIBUTES.toArray(new AttributeDefinition[0]));
        builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.DEFINED,
                JacORBSubsystemDefinitions.IOR_AS_CONTEXT_ATTRIBUTES.toArray(new AttributeDefinition[0]));
        builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.DEFINED,
                JacORBSubsystemDefinitions.IOR_SAS_CONTEXT_ATTRIBUTES.toArray(new AttributeDefinition[0]));
        TransformationDescription.Tools.register(builder.build(), subsystem, ModelVersion.create(1, 3, 0));
    }
}