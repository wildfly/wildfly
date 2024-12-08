/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.JdkLoggerFactory;

import org.jboss.as.clustering.controller.persistence.xml.SubsystemResourceXMLSchema;
import org.jboss.as.controller.Extension;
import org.kohsuke.MetaInfServices;
import org.wildfly.subsystem.SubsystemConfiguration;
import org.wildfly.subsystem.SubsystemExtension;

/**
 * Extension that registers the Infinispan subsystem.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
@MetaInfServices(Extension.class)
public class InfinispanExtension extends SubsystemExtension<InfinispanSubsystemSchema> {

    public InfinispanExtension() {
        super(SubsystemConfiguration.of(InfinispanSubsystemResourceDescription.INSTANCE.getName(), InfinispanSubsystemModel.CURRENT, InfinispanSubsystemResourceDefinitionRegistrar::new),  SubsystemResourceXMLSchema.persistence(InfinispanSubsystemSchema.CURRENT));

        // Initialize the Netty logger factory
        InternalLoggerFactory.setDefaultFactory(JdkLoggerFactory.INSTANCE);
    }
}
