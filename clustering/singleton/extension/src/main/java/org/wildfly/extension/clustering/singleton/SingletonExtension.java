/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import org.jboss.as.clustering.controller.SubsystemExtension;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.kohsuke.MetaInfServices;

/**
 * Extension point for singleton subsystem.
 * @author Paul Ferraro
 */
@MetaInfServices(Extension.class)
public class SingletonExtension extends SubsystemExtension<SingletonSubsystemSchema> {

    static final String SUBSYSTEM_NAME = "singleton";
    static final ParentResourceDescriptionResolver SUBSYSTEM_RESOLVER = new SubsystemResourceDescriptionResolver(SUBSYSTEM_NAME, SingletonExtension.class);

    public SingletonExtension() {
        super(SUBSYSTEM_NAME, SingletonSubsystemModel.CURRENT, SingletonResourceDefinition::new, SingletonSubsystemSchema.CURRENT, new SingletonXMLWriter());
    }
}
