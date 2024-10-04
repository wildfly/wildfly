/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.subsystem;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.ee.component.deployers.DefaultBindingsConfigurationProcessor;
import org.jboss.as.ee.concurrent.ConcurrencyImplementation;

/**
 * JBossAS domain extension used to initialize the ee subsystem handlers and associated classes.
 *
 * @author Weston M. Price
 * @author Emanuel Muckenhuber
 */
public class EeExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "ee";
    private static final String RESOURCE_NAME = EeExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final ModelVersion CURRENT_MODEL_VERSION = EESubsystemModel.Version.v6_0_0;

    protected static final PathElement PATH_SUBSYSTEM = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, EeExtension.class.getClassLoader(), true, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);

        // Register the root subsystem resource.
        final ManagementResourceRegistration rootResource = subsystem.registerSubsystemModel(EeSubsystemRootResource.create());

        // Mandatory describe operation
        rootResource.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        // register submodels
        ConcurrencyImplementation.INSTANCE.registerRootResourceSubModels(rootResource, context);
        rootResource.registerSubModel(new DefaultBindingsResourceDefinition(new DefaultBindingsConfigurationProcessor()));
        rootResource.registerSubModel(new GlobalDirectoryResourceDefinition());

        subsystem.registerXMLElementWriter(EESubsystemXmlPersister.INSTANCE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.EE_1_0.getUriString(), EESubsystemParser10::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.EE_1_1.getUriString(), EESubsystemParser11::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.EE_1_2.getUriString(), EESubsystemParser12::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.EE_2_0.getUriString(), EESubsystemParser20::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.EE_3_0.getUriString(), EESubsystemParser20::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.EE_4_0.getUriString(), EESubsystemParser40::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.EE_5_0.getUriString(), EESubsystemParser50::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.EE_6_0.getUriString(), EESubsystemParser60::new);
        context.setProfileParsingCompletionHandler(new BeanValidationProfileParsingCompletionHandler());
    }


}
