/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.JdkLoggerFactory;

import org.jboss.as.clustering.controller.SubsystemExtension;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.kohsuke.MetaInfServices;

/**
 * Extension that registers the Infinispan subsystem.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
@MetaInfServices(Extension.class)
public class InfinispanExtension extends SubsystemExtension<InfinispanSubsystemSchema> {

    public static final String SUBSYSTEM_NAME = "infinispan";
    public static final ParentResourceDescriptionResolver SUBSYSTEM_RESOLVER = new SubsystemResourceDescriptionResolver(SUBSYSTEM_NAME, InfinispanExtension.class);

    public InfinispanExtension() {
        super(SUBSYSTEM_NAME, InfinispanSubsystemModel.CURRENT, InfinispanSubsystemResourceDefinition::new, InfinispanSubsystemSchema.CURRENT, new InfinispanSubsystemXMLWriter());

        // Initialize the Netty logger factory
        InternalLoggerFactory.setDefaultFactory(JdkLoggerFactory.INSTANCE);
    }
}
