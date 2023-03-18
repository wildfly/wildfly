/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
