/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import org.jboss.as.controller.Extension;
import org.kohsuke.MetaInfServices;
import org.wildfly.subsystem.SubsystemConfiguration;
import org.wildfly.subsystem.SubsystemExtension;
import org.wildfly.subsystem.SubsystemPersistence;

/**
 * Extension point for singleton subsystem.
 * @author Paul Ferraro
 */
@MetaInfServices(Extension.class)
public class SingletonExtension extends SubsystemExtension<SingletonSubsystemSchema> {

    public SingletonExtension() {
        super(SubsystemConfiguration.of(SingletonSubsystemResourceDefinitionRegistrar.REGISTRATION.getName(), SingletonSubsystemModel.CURRENT, SingletonSubsystemResourceDefinitionRegistrar::new), SubsystemPersistence.of(SingletonSubsystemSchema.CURRENT));
    }
}
