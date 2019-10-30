/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming.subsystem;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

/**
 * @author Tomaz Cerar (c) 2017 Red Hat Inc.
 */
public class NamingTransformers implements ExtensionTransformerRegistration {
    @Override
    public String getSubsystemName() {
        return NamingExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystem) {
        final ModelVersion v2_0_0 = ModelVersion.create(2, 0, 0);
        final ModelVersion v1_3_0 = ModelVersion.create(1, 3, 0); //eap 6.2 - 6.4

        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(subsystem.getCurrentSubsystemVersion());

        /*====== Comparing subsystem models ======
        --- Problems for relative address to root []:
        ====== Resource root address: ["subsystem" => "naming"] - Current version: 2.1.0; legacy version: 2.0.0 =======
        --- Problems for relative address to root ["binding" => "*"]:
        Missing operations in current: []; missing in legacy [rebind]
        */
        ResourceTransformationDescriptionBuilder builder_2_0 = chainedBuilder.createBuilder(subsystem.getCurrentSubsystemVersion(), v2_0_0);

        builder_2_0.addChildResource(NamingSubsystemModel.BINDING_PATH)
                .addOperationTransformationOverride(NamingSubsystemModel.REBIND).setReject();

        chainedBuilder.createBuilder(v2_0_0, v1_3_0);

        chainedBuilder.buildAndRegister(subsystem, new ModelVersion[]{
                v1_3_0,
                v2_0_0,
        });
    }
}
