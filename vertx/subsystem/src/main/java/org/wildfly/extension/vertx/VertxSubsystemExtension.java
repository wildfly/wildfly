/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.vertx;

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.JdkLoggerFactory;
import io.vertx.core.logging.LoggerFactory;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemModel;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.version.Stability;
import org.wildfly.subsystem.SubsystemConfiguration;
import org.wildfly.subsystem.SubsystemExtension;
import org.wildfly.subsystem.SubsystemPersistence;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.extension.vertx.VertxConstants.EXTENSION_STABILITY;

/**
 * @author <a href="aoingl@gmail.com">Lin Gao</a>
 */
public class VertxSubsystemExtension extends SubsystemExtension<VertxSubsystemSchema> {
    public static final String SUBSYSTEM_NAME = "vertx";

    protected static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    private static final String RESOURCE_NAME = VertxSubsystemExtension.class.getPackage().getName() + ".LocalDescriptions";

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder();
        for (String kp : keyPrefix) {
            if (prefix.length() > 0){
                prefix.append('.');
            }
            prefix.append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, VertxSubsystemExtension.class.getClassLoader(), true, false);
    }

    public VertxSubsystemExtension() {
        super(SubsystemConfiguration.of(SUBSYSTEM_NAME, VertxSubsystemModel.CURRENT, VertxSubsystemRegistrar::new),
                SubsystemPersistence.of(VertxSubsystemSchema.CURRENT));
        // Initialize the Netty logger factory
        InternalLoggerFactory.setDefaultFactory(JdkLoggerFactory.INSTANCE);
        // Initialize the Vert.x logger factory
        //noinspection deprecation
        LoggerFactory.initialise();
    }

    @Override
    public Stability getStability() {
        return EXTENSION_STABILITY;
    }

    /**
     * Model for the vertx subsystem.
     */
    enum VertxSubsystemModel implements SubsystemModel {
        VERSION_1_0_0(1, 0, 0),
        ;

        static final VertxSubsystemModel CURRENT = VERSION_1_0_0;
        private final ModelVersion version;

        VertxSubsystemModel(int major, int minor, int micro) {
            this.version = ModelVersion.create(major, minor, micro);
        }

        @Override
        public ModelVersion getVersion() {
            return version;
        }
    }

}
