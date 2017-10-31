/*
 * Copyright 2017 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.connector.subsystems.jca;

import static org.jboss.as.connector.subsystems.jca.JcaDistributedWorkManagerDefinition.PATH_DISTRIBUTED_WORK_MANAGER;
import static org.jboss.as.connector.subsystems.jca.JcaWorkManagerDefinition.PATH_WORK_MANAGER;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Emmanuel Hugonnet (c) 2017 Red Hat, inc.
 */
public class JcaTransformers implements ExtensionTransformerRegistration{

    private static final ModelVersion EAP_6_2 = ModelVersion.create(1, 2, 0);
    private static final ModelVersion EAP_7_0 = ModelVersion.create(4, 0, 0);

    @Override
    public String getSubsystemName() {
        return JcaExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {
        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(subsystemRegistration.getCurrentSubsystemVersion());
        ResourceTransformationDescriptionBuilder parentBuilder = chainedBuilder.createBuilder(subsystemRegistration.getCurrentSubsystemVersion(), EAP_7_0);
        ResourceTransformationDescriptionBuilder builder = parentBuilder.addChildResource(PATH_DISTRIBUTED_WORK_MANAGER);
        builder.getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, true, new ModelNode(false)),
                        JcaDistributedWorkManagerDefinition.DWmParameters.ELYTRON_ENABLED.getAttribute())
                .addRejectCheck(RejectAttributeChecker.DEFINED, JcaDistributedWorkManagerDefinition.DWmParameters.ELYTRON_ENABLED.getAttribute())
                .end();
        builder = parentBuilder.addChildResource(PATH_WORK_MANAGER);
        builder.getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, true, new ModelNode(false)),
                        JcaWorkManagerDefinition.WmParameters.ELYTRON_ENABLED.getAttribute())
                .addRejectCheck(RejectAttributeChecker.DEFINED, JcaWorkManagerDefinition.WmParameters.ELYTRON_ENABLED.getAttribute())
                .end();

        parentBuilder = chainedBuilder.createBuilder(EAP_7_0, EAP_6_2);
        parentBuilder.rejectChildResource(JcaDistributedWorkManagerDefinition.PATH_DISTRIBUTED_WORK_MANAGER);
        parentBuilder.discardChildResource(TracerDefinition.PATH_TRACER);

        chainedBuilder.buildAndRegister(subsystemRegistration, new ModelVersion[]{
                EAP_6_2,
                EAP_7_0,
        });
    }
}
