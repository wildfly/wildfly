/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat Middleware LLC, and individual contributors
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

import static org.jboss.as.controller.transform.description.TransformationDescription.Tools.register;

import java.util.EnumSet;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.kohsuke.MetaInfServices;

/**
 * @author Radoslav Husar
 */
@MetaInfServices(ExtensionTransformerRegistration.class)
public class ModClusterExtensionTransformerRegistration implements ExtensionTransformerRegistration {

    @Override
    public String getSubsystemName() {
        return ModClusterExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration registration) {
        // Register transformers for all but the current model
        for (ModClusterModel modClusterModel : EnumSet.complementOf(EnumSet.of(ModClusterModel.CURRENT))) {
            ModelVersion version = modClusterModel.getVersion();
            register(ModClusterSubsystemResourceDefinition.buildTransformation(version), registration, version);
        }
    }
}