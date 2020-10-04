/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.reactive.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

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
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.wildfly.security.manager.WildFlySecurityManager;

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.JdkLoggerFactory;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MicroProfileReactiveMessagingExtension implements Extension {

    /**
     * The name of our subsystem within the model.
     */
    public static final String SUBSYSTEM_NAME = "microprofile-reactive-messaging-smallrye";

    protected static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    private static final String RESOURCE_NAME = MicroProfileReactiveMessagingExtension.class.getPackage().getName() + ".LocalDescriptions";

    static final String WELD_CAPABILITY_NAME = "org.wildfly.weld";
    static final String REACTIVE_STREAMS_OPERATORS_CAPABILITY_NAME = "org.wildfly.microprofile.reactive-streams-operators";
    static final String CONFIG_CAPABILITY_NAME = "org.wildfly.microprofile.config";

    protected static final ModelVersion VERSION_1_0_0 = ModelVersion.create(1, 0, 0);
    private static final ModelVersion CURRENT_MODEL_VERSION = VERSION_1_0_0;

    private static final MicroProfileReactiveMessagingParser_1_0 CURRENT_PARSER = new MicroProfileReactiveMessagingParser_1_0();

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        return getResourceDescriptionResolver(true, keyPrefix);

    }

    static ResourceDescriptionResolver getResourceDescriptionResolver(final boolean useUnprefixedChildTypes, final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder();
        for (String kp : keyPrefix) {
            if (prefix.length() > 0){
                prefix.append('.');
            }
            prefix.append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, MicroProfileReactiveMessagingExtension.class.getClassLoader(), true, useUnprefixedChildTypes);
    }


    @Override
    public void initialize(ExtensionContext extensionContext) {
        // Initialize the Netty logger factory or we get horrible stack traces
        ClassLoader cl = WildFlySecurityManager.getClassLoaderPrivileged(this.getClass());
        if (cl instanceof ModuleClassLoader) {
            ModuleLoader loader = ((ModuleClassLoader) cl).getModule().getModuleLoader();
            try {
                Module module = loader.loadModule("io.netty");
                InternalLoggerFactory.setDefaultFactory(JdkLoggerFactory.INSTANCE);
            } catch (ModuleLoadException e) {
                // The netty module is not there so don't do anything
            }
        }

        final SubsystemRegistration sr =  extensionContext.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        sr.registerXMLElementWriter(CURRENT_PARSER);
        final ManagementResourceRegistration root = sr.registerSubsystemModel(new MicroProfileReactiveMessagingSubsystemDefinition());
        root.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE, false);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext extensionParsingContext) {
        extensionParsingContext.setSubsystemXmlMapping(SUBSYSTEM_NAME, MicroProfileReactiveMessagingParser_1_0.NAMESPACE, CURRENT_PARSER);
    }
}
