/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.mod_cluster;

import org.jboss.as.clustering.controller.SubsystemExtension;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.kohsuke.MetaInfServices;

/**
 * Defines the mod_cluster subsystem and its resources.
 *
 * @author Jean-Frederic Clere
 * @author Tomaz Cerar
 * @author Radoslav Husar
 */
@MetaInfServices(Extension.class)
public class ModClusterExtension extends SubsystemExtension<ModClusterSubsystemSchema> {

    public static final String SUBSYSTEM_NAME = "modcluster";

    static final ParentResourceDescriptionResolver SUBSYSTEM_RESOLVER = new SubsystemResourceDescriptionResolver(SUBSYSTEM_NAME, ModClusterExtension.class);

    public static final SensitiveTargetAccessConstraintDefinition MOD_CLUSTER_SECURITY_DEF = new SensitiveTargetAccessConstraintDefinition(
            new SensitivityClassification(SUBSYSTEM_NAME, "mod_cluster-security", false, true, true));
    public static final SensitiveTargetAccessConstraintDefinition MOD_CLUSTER_PROXIES_DEF = new SensitiveTargetAccessConstraintDefinition(
            new SensitivityClassification(SUBSYSTEM_NAME, "mod_cluster-proxies", false, false, false));

    public ModClusterExtension() {
        super(SUBSYSTEM_NAME, ModClusterSubsystemModel.CURRENT, ModClusterSubsystemResourceDefinition::new, ModClusterSubsystemSchema.CURRENT, new ModClusterSubsystemXMLWriter());
    }
}
