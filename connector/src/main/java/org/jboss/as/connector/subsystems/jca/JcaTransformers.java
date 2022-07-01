/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
package org.jboss.as.connector.subsystems.jca;

import static org.jboss.as.connector.subsystems.jca.JcaDistributedWorkManagerDefinition.PATH_DISTRIBUTED_WORK_MANAGER;
import static org.jboss.as.connector.subsystems.jca.JcaExtension.SUBSYSTEM_NAME;
import static org.jboss.as.connector.subsystems.jca.JcaWorkManagerDefinition.PATH_WORK_MANAGER;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

public class JcaTransformers implements ExtensionTransformerRegistration {

    private static final ModelVersion EAP_7_4 = ModelVersion.create(5, 0, 0);

    @Override
    public String getSubsystemName() {
        return SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {
        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(subsystemRegistration.getCurrentSubsystemVersion());
        get500TransformationDescription(chainedBuilder.createBuilder(subsystemRegistration.getCurrentSubsystemVersion(), EAP_7_4));

        chainedBuilder.buildAndRegister(subsystemRegistration, new ModelVersion[]{
                EAP_7_4
        });
    }

    private static void get500TransformationDescription(ResourceTransformationDescriptionBuilder parentBuilder) {
        parentBuilder.addChildResource(PATH_WORK_MANAGER)
            .getAttributeBuilder()
                .setValueConverter(AttributeConverter.DEFAULT_VALUE,
                        JcaWorkManagerDefinition.WmParameters.ELYTRON_ENABLED.getAttribute())
                .end();
        parentBuilder.addChildResource(PATH_DISTRIBUTED_WORK_MANAGER)
            .getAttributeBuilder()
                .setValueConverter(AttributeConverter.DEFAULT_VALUE,
                        JcaDistributedWorkManagerDefinition.DWmParameters.ELYTRON_ENABLED.getAttribute())
                .end();
    }

}