/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import org.jboss.as.clustering.controller.PersistentSubsystemExtension;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.kohsuke.MetaInfServices;

/**
 * Extension that registers the distributable-web subsystem.
 * @author Paul Ferraro
 */
@MetaInfServices(Extension.class)
public class DistributableWebExtension extends PersistentSubsystemExtension<DistributableWebSubsystemSchema> {

    static final String SUBSYSTEM_NAME = "distributable-web";
    static final ParentResourceDescriptionResolver SUBSYSTEM_RESOLVER = new SubsystemResourceDescriptionResolver(SUBSYSTEM_NAME, DistributableWebExtension.class);

    public DistributableWebExtension() {
        super(SUBSYSTEM_NAME, DistributableWebSubsystemModel.CURRENT, DistributableWebResourceDefinition::new, DistributableWebSubsystemSchema.CURRENT);
    }
}
