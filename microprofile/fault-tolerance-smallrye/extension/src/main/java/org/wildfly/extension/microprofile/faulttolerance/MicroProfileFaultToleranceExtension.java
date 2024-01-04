/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.faulttolerance;

import org.jboss.as.clustering.controller.PersistentSubsystemExtension;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;

/**
 * Extension that registers the microprofile fault tolerance subsystem
 * @author Radoslav Husar
 */
public class MicroProfileFaultToleranceExtension extends PersistentSubsystemExtension<MicroProfileFaultToleranceSchema> {

    static final String SUBSYSTEM_NAME = "microprofile-fault-tolerance-smallrye";
    static final ParentResourceDescriptionResolver SUBSYSTEM_RESOLVER = new SubsystemResourceDescriptionResolver(SUBSYSTEM_NAME, MicroProfileFaultToleranceExtension.class);

    public MicroProfileFaultToleranceExtension() {
        super(SUBSYSTEM_NAME, MicroProfileFaultToleranceModel.CURRENT, MicroProfileFaultToleranceResourceDefinition::new, MicroProfileFaultToleranceSchema.CURRENT);
    }
}
