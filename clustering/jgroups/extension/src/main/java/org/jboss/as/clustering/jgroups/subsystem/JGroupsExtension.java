/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.clustering.jgroups.LogFactory;
import org.jboss.as.controller.Extension;
import org.kohsuke.MetaInfServices;
import org.wildfly.subsystem.SubsystemConfiguration;
import org.wildfly.subsystem.SubsystemExtension;
import org.wildfly.subsystem.SubsystemPersistence;

/**
 * Registers the JGroups subsystem.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
@MetaInfServices(Extension.class)
public class JGroupsExtension extends SubsystemExtension<JGroupsSubsystemSchema> {

    public JGroupsExtension() {
        super(SubsystemConfiguration.of(JGroupsSubsystemResourceDefinitionRegistrar.REGISTRATION.getName(), JGroupsSubsystemModel.CURRENT, JGroupsSubsystemResourceDefinitionRegistrar::new), SubsystemPersistence.of(JGroupsSubsystemSchema.CURRENT));

        // Workaround for JGRP-1475
        // Configure JGroups to use jboss-logging.
        org.jgroups.logging.LogFactory.setCustomLogFactory(new LogFactory());
    }
}
