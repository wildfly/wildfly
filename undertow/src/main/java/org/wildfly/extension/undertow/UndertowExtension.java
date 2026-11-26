/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import org.jboss.as.controller.Extension;
import org.kohsuke.MetaInfServices;
import org.wildfly.subsystem.SubsystemConfiguration;
import org.wildfly.subsystem.SubsystemExtension;
import org.wildfly.subsystem.SubsystemPersistence;


/**
 * An extension that registers the undertow subsystem.
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 * @author Paul Ferraro
 */
@MetaInfServices(Extension.class)
public class UndertowExtension extends SubsystemExtension<UndertowSubsystemSchema> {
    public static final String SUBSYSTEM_NAME = UndertowRootDefinition.REGISTRATION.getName();

    public UndertowExtension() {
        super(SubsystemConfiguration.of(SUBSYSTEM_NAME, UndertowSubsystemModel.CURRENT, UndertowRootDefinition::new), SubsystemPersistence.of(UndertowSubsystemSchema.CURRENT));
    }
}