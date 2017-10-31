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

import java.util.Collections;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DeprecatedResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.extension.AbstractLegacyExtension;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

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

    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(2, 0, 0);

    static final ModelVersion DEPRECATED_SINCE = ModelVersion.create(2, 0, 0);

    public JacORBExtension() {
        super("org.jboss.as.jacorb", SUBSYSTEM_NAME);
    }

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new DeprecatedResourceDescriptionResolver(SUBSYSTEM_NAME, prefix.toString(), RESOURCE_NAME, JacORBExtension.class.getClassLoader(), true, false);
    }

    @Override
    protected Set<ManagementResourceRegistration> initializeLegacyModel(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        final ManagementResourceRegistration subsystemRegistration = subsystem.registerSubsystemModel(JacORBSubsystemResource.INSTANCE);
        subsystem.registerXMLElementWriter(PARSER);

        if (context.isRegisterTransformers()) {
            // Register the model transformers
            registerTransformers(subsystem);
        }

        return Collections.singleton(subsystemRegistration);
    }

    @Override
    protected void initializeLegacyParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, JacORBSubsystemParser.Namespace.JacORB_1_0.getUriString(), () -> PARSER);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, JacORBSubsystemParser.Namespace.JacORB_1_1.getUriString(), () -> PARSER);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, JacORBSubsystemParser.Namespace.JacORB_1_2.getUriString(), () -> PARSER);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, JacORBSubsystemParser.Namespace.JacORB_1_3.getUriString(), () -> PARSER);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, JacORBSubsystemParser.Namespace.JacORB_1_4.getUriString(), () -> PARSER);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, JacORBSubsystemParser.Namespace.JacORB_2_0.getUriString(), () -> PARSER);
    }

    /**
     * Register the transformers for the 1.3.0 version.
     *
     * @param subsystem the subsystems registration
     */
    protected static void registerTransformers(final SubsystemRegistration subsystem) {
        ChainedTransformationDescriptionBuilder chained = ResourceTransformationDescriptionBuilder.Factory.createChainedSubystemInstance(CURRENT_MODEL_VERSION);
        ModelVersion MODEL_VERSION_EAP64 = ModelVersion.create(1, 4, 0);
        ModelVersion MODEL_VERSION_EAP63 = ModelVersion.create(1, 3, 0);//also EAP6.2

        ResourceTransformationDescriptionBuilder builder64 = chained.createBuilder(CURRENT_MODEL_VERSION, MODEL_VERSION_EAP64);
        builder64.getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, JacORBSubsystemDefinitions.PERSISTENT_SERVER_ID)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(JacORBSubsystemDefinitions.PERSISTENT_SERVER_ID.getDefaultValue()), JacORBSubsystemDefinitions.PERSISTENT_SERVER_ID)
                .setValueConverter(new AttributeConverter.DefaultValueAttributeConverter(JacORBSubsystemDefinitions.INTEROP_CHUNK_RMI_VALUETYPES),JacORBSubsystemDefinitions.INTEROP_CHUNK_RMI_VALUETYPES);


        ResourceTransformationDescriptionBuilder builder63 = chained.createBuilder(MODEL_VERSION_EAP64, MODEL_VERSION_EAP63);
        builder63.getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, IORTransportConfigDefinition.ATTRIBUTES.toArray(new AttributeDefinition[0]))
                .addRejectCheck(RejectAttributeChecker.DEFINED, IORASContextDefinition.ATTRIBUTES.toArray(new AttributeDefinition[0]))
                .addRejectCheck(RejectAttributeChecker.DEFINED, IORSASContextDefinition.ATTRIBUTES.toArray(new AttributeDefinition[0]))
                .end()
                .rejectChildResource(IORSettingsDefinition.INSTANCE.getPathElement());


        chained.buildAndRegister(subsystem, new ModelVersion[]{
                MODEL_VERSION_EAP64,
                MODEL_VERSION_EAP63
        });
    }

}
