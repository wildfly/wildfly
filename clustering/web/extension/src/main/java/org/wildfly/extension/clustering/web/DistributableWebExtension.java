/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import org.jboss.as.controller.Extension;
import org.kohsuke.MetaInfServices;
import org.wildfly.subsystem.SubsystemConfiguration;
import org.wildfly.subsystem.SubsystemExtension;
import org.wildfly.subsystem.SubsystemPersistence;

/**
 * Extension that registers the distributable-web subsystem.
 * @author Paul Ferraro
 */
@MetaInfServices(Extension.class)
public class DistributableWebExtension extends SubsystemExtension<DistributableWebSubsystemSchema> {

    public DistributableWebExtension() {
        super(SubsystemConfiguration.of(DistributableWebSubsystemResourceDefinitionRegistrar.REGISTRATION.getName(), DistributableWebSubsystemModel.CURRENT, DistributableWebSubsystemResourceDefinitionRegistrar::new), SubsystemPersistence.of(DistributableWebSubsystemSchema.CURRENT));
    }
}
