/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.reactive.messaging;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.EnumSet;
import java.util.List;

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.JdkLoggerFactory;

import io.vertx.core.logging.LoggerFactory;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLDescriptionReader;
import org.jboss.as.controller.PersistentResourceXMLDescriptionWriter;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.staxmapper.XMLElementReader;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MicroProfileReactiveMessagingExtension implements Extension {

    /**
     * The name of our subsystem within the model.
     */
    public static final String SUBSYSTEM_NAME = "microprofile-reactive-messaging-smallrye";

    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    static final ParentResourceDescriptionResolver SUBSYSTEM_RESOLVER = new SubsystemResourceDescriptionResolver(SUBSYSTEM_NAME, MicroProfileReactiveMessagingExtension.class);

    static final String WELD_CAPABILITY_NAME = "org.wildfly.weld";
    static final String REACTIVE_STREAMS_OPERATORS_CAPABILITY_NAME = "org.wildfly.microprofile.reactive-streams-operators";
    static final String CONFIG_CAPABILITY_NAME = "org.wildfly.microprofile.config";

    static final ModelVersion VERSION_1_0_0 = ModelVersion.create(1, 0, 0);
    static final ModelVersion VERSION_2_0_0 = ModelVersion.create(2, 0, 0);
    private static final ModelVersion CURRENT_MODEL_VERSION = VERSION_2_0_0;


    private final PersistentResourceXMLDescription currentDescription = MicroProfileReactiveMessagingSubsystemSchema.CURRENT.getXMLDescription();

    @Override
    public void initialize(ExtensionContext extensionContext) {
        // Initialize the Netty logger factory or we get horrible stack traces
        // Initialize the vert.x logger factory or we get SM failures if deployment code is on the stack when it initializes stack traces
        ClassLoader cl = WildFlySecurityManager.getClassLoaderPrivileged(this.getClass());
        if (cl instanceof ModuleClassLoader) {
            ModuleLoader loader = ((ModuleClassLoader) cl).getModule().getModuleLoader();
            try {
                loader.loadModule("io.netty");
                InternalLoggerFactory.setDefaultFactory(JdkLoggerFactory.INSTANCE);
            } catch (ModuleLoadException e) {
                // The netty module is not there so don't do anything
            }
            try {
                loader.loadModule("io.vertx.core");
                //noinspection deprecation
                LoggerFactory.initialise();
            } catch (ModuleLoadException e) {
                // The vert.x module is not there so don't do anything
            }
        }

        final SubsystemRegistration sr =  extensionContext.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        sr.registerXMLElementWriter(new PersistentResourceXMLDescriptionWriter(this.currentDescription));
        final ManagementResourceRegistration root = sr.registerSubsystemModel(new MicroProfileReactiveMessagingSubsystemDefinition());
        root.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE, false);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext extensionParsingContext) {
        for (MicroProfileReactiveMessagingSubsystemSchema schema : EnumSet.allOf(MicroProfileReactiveMessagingSubsystemSchema.class)) {
            XMLElementReader<List<ModelNode>> reader = (schema == MicroProfileReactiveMessagingSubsystemSchema.CURRENT) ? new PersistentResourceXMLDescriptionReader(this.currentDescription) : schema;
            extensionParsingContext.setSubsystemXmlMapping(SUBSYSTEM_NAME, schema.getNamespace().getUri(), reader);
        }
    }

}
