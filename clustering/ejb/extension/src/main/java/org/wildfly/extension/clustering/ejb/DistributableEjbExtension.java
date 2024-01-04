/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import org.jboss.as.clustering.controller.PersistentSubsystemExtension;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.kohsuke.MetaInfServices;

/**
 * Defines the extension for the distributable-ejb subsystem.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
@MetaInfServices(Extension.class)
public class DistributableEjbExtension extends PersistentSubsystemExtension<DistributableEjbSubsystemSchema> {

    static final String SUBSYSTEM_NAME = "distributable-ejb";
    static final ParentResourceDescriptionResolver SUBSYSTEM_RESOLVER = new SubsystemResourceDescriptionResolver(SUBSYSTEM_NAME, DistributableEjbExtension.class);

    public DistributableEjbExtension() {
        super(SUBSYSTEM_NAME, DistributableEjbSubsystemModel.CURRENT, DistributableEjbResourceDefinition::new, DistributableEjbSubsystemSchema.CURRENT);
    }
}
