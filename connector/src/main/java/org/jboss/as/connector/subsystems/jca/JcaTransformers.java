/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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