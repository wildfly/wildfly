/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.openapi;

import org.jboss.as.controller.Extension;
import org.kohsuke.MetaInfServices;
import org.wildfly.subsystem.SubsystemConfiguration;
import org.wildfly.subsystem.SubsystemExtension;
import org.wildfly.subsystem.SubsystemPersistence;

/**
 * Extension that provides the MicroProfile OpenAPI subsystem.
 * @author Michael Edgar
 * @author Paul Ferraro
 */
@MetaInfServices(Extension.class)
public class MicroProfileOpenAPIExtension extends SubsystemExtension<MicroProfileOpenAPISubsystemSchema> {

    public MicroProfileOpenAPIExtension() {
        super(SubsystemConfiguration.of(MicroProfileOpenAPISubsystemRegistrar.REGISTRATION, MicroProfileOpenAPISubsystemModel.CURRENT, MicroProfileOpenAPISubsystemRegistrar::new), SubsystemPersistence.of(MicroProfileOpenAPISubsystemSchema.CURRENT));
    }
}
