/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.clustering.controller.SubsystemExtension;
import org.jboss.as.clustering.jgroups.LogFactory;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.kohsuke.MetaInfServices;

/**
 * Registers the JGroups subsystem.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
@MetaInfServices(Extension.class)
public class JGroupsExtension extends SubsystemExtension<JGroupsSubsystemSchema> {

    static final String SUBSYSTEM_NAME = "jgroups";
    static final ParentResourceDescriptionResolver SUBSYSTEM_RESOLVER = new SubsystemResourceDescriptionResolver(SUBSYSTEM_NAME, JGroupsExtension.class);

    public JGroupsExtension() {
        super(SUBSYSTEM_NAME, JGroupsSubsystemModel.CURRENT, JGroupsSubsystemResourceDefinition::new, JGroupsSubsystemSchema.CURRENT, new JGroupsSubsystemXMLWriter());

        // Workaround for JGRP-1475
        // Configure JGroups to use jboss-logging.
        org.jgroups.logging.LogFactory.setCustomLogFactory(new LogFactory());
    }
}
