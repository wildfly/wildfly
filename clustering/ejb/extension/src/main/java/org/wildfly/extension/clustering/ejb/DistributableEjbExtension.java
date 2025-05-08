/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import org.jboss.as.controller.Extension;
import org.kohsuke.MetaInfServices;
import org.wildfly.subsystem.SubsystemConfiguration;
import org.wildfly.subsystem.SubsystemExtension;
import org.wildfly.subsystem.SubsystemPersistence;

/**
 * An extension providing the distributable-ejb subsystem.
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
@MetaInfServices(Extension.class)
public class DistributableEjbExtension extends SubsystemExtension<DistributableEjbSubsystemSchema> {

    public DistributableEjbExtension() {
        super(SubsystemConfiguration.of(DistributableEjbSubsystemResourceDefinitionRegistrar.REGISTRATION.getName(), DistributableEjbSubsystemModel.CURRENT, DistributableEjbSubsystemResourceDefinitionRegistrar::new), SubsystemPersistence.of(DistributableEjbSubsystemSchema.CURRENT));
    }
}
