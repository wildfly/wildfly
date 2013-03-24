/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.threads;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.threads.CommonAttributes.THREADS;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

/**
 * Extension for thread management.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ThreadsExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "threads";
    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);

    static final String RESOURCE_NAME = ThreadsExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 1;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix, boolean useUnprefixedChildTypes) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, ThreadsExtension.class.getClassLoader(), true, useUnprefixedChildTypes);
    }

    @Override
    public void initialize(final ExtensionContext context) {

        ThreadsLogger.ROOT_LOGGER.debugf("Initializing Threading Extension");

        final boolean registerRuntimeOnly = context.isRuntimeOnlyRegistrationValid();

        // Register the remoting subsystem
        final SubsystemRegistration registration = context.registerSubsystem(THREADS, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);
        registration.registerXMLElementWriter(ThreadsParser.INSTANCE);

        // Remoting subsystem description and operation handlers
        final ManagementResourceRegistration subsystem = registration.registerSubsystemModel(new ThreadSubsystemResourceDefinition(registerRuntimeOnly));
        subsystem.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        if (context.isRegisterTransformers()) {
            registerTransformers1_0(registration);
        }
    }

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.CURRENT.getUriString(), ThreadsParser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.THREADS_1_0.getUriString(), ThreadsParser.INSTANCE);
    }

    // Transformation

    /**
     * Register the transformers for older model versions.
     *
     * @param subsystem the subsystems registration
     */
    private static void registerTransformers1_0(final SubsystemRegistration subsystem) {
        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        BoundedQueueThreadPoolResourceDefinition.registerTransformers1_0(builder);
        QueuelessThreadPoolResourceDefinition.registerTransformers1_0(builder);
        ScheduledThreadPoolResourceDefinition.registerTransformers1_0(builder);
        UnboundedQueueThreadPoolResourceDefinition.registerTransformers1_0(builder);
        ThreadFactoryResourceDefinition.registerTransformers1_0(builder);
        TransformationDescription.Tools.register(builder.build(), subsystem, ModelVersion.create(1, 0, 0));

    }


}
