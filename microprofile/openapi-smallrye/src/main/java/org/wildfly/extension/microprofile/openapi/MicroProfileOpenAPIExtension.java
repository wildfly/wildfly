/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.openapi;

import org.jboss.as.clustering.controller.PersistentSubsystemExtension;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.kohsuke.MetaInfServices;

/**
 * Extension that provides the MicroProfile OpenAPI subsystem.
 * @author Michael Edgar
 * @author Paul Ferraro
 */
@MetaInfServices(Extension.class)
public class MicroProfileOpenAPIExtension extends PersistentSubsystemExtension<MicroProfileOpenAPISubsystemSchema> {

    static final String SUBSYSTEM_NAME = "microprofile-openapi-smallrye";
    static final ParentResourceDescriptionResolver SUBSYSTEM_RESOLVER = new SubsystemResourceDescriptionResolver(SUBSYSTEM_NAME, MicroProfileOpenAPIExtension.class);

    public MicroProfileOpenAPIExtension() {
        super(SUBSYSTEM_NAME, MicroProfileOpenAPISubsystemModel.CURRENT, MicroProfileOpenAPISubsystemDefinition::new, MicroProfileOpenAPISubsystemSchema.CURRENT);
    }
}
