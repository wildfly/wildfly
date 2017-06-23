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
package org.jboss.as.txn.subsystem;

import static org.jboss.as.txn.subsystem.TransactionExtension.CURRENT_MODEL_VERSION;
import static org.jboss.as.txn.subsystem.TransactionExtension.MODEL_VERSION_EAP62;
import static org.jboss.as.txn.subsystem.TransactionExtension.MODEL_VERSION_EAP63;
import static org.jboss.as.txn.subsystem.TransactionExtension.MODEL_VERSION_EAP64;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

import static org.jboss.as.txn.subsystem.TransactionExtension.MODEL_VERSION_EAP70;

/**
 *
 * @author Emmanuel Hugonnet (c) 2017 Red Hat, inc.
 */
public class TransactionTransformers implements ExtensionTransformerRegistration{

    @Override
    public String getSubsystemName() {
        return TransactionExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {
        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(CURRENT_MODEL_VERSION);
        /*final ModelVersion v2_0_0 = ModelVersion.create(2, 0, 0);
        ResourceTransformationDescriptionBuilder builder_2_0 = chainedBuilder.createBuilder(subsystem.getSubsystemVersion(), v2_0_0);

        //Versions < 3.0.0 is not able to handle commit-markable-resource
        builder_2_0.rejectChildResource(CMResourceResourceDefinition.PATH_CM_RESOURCE);
        builder_2_0.getAttributeBuilder()
                .addRename(TransactionSubsystemRootResourceDefinition.USE_JOURNAL_STORE, CommonAttributes.USE_HORNETQ_STORE)
                .addRename(TransactionSubsystemRootResourceDefinition.JOURNAL_STORE_ENABLE_ASYNC_IO, CommonAttributes.HORNETQ_STORE_ENABLE_ASYNC_IO);*/

         // 4.0.0 --> 3.0.0
          ResourceTransformationDescriptionBuilder builderEap7 = chainedBuilder.createBuilder(CURRENT_MODEL_VERSION, MODEL_VERSION_EAP70);
          builderEap7.getAttributeBuilder()
                  .addRejectCheck(RejectAttributeChecker.UNDEFINED, PROCESS_ID_SOCKET_BINDING)
                  .end();
        // 3.0.0 --> 1.5.0
        ResourceTransformationDescriptionBuilder builderEap64 = chainedBuilder.createBuilder(MODEL_VERSION_EAP70, MODEL_VERSION_EAP64);
        builderEap64.getAttributeBuilder()
                .addRename(TransactionSubsystemRootResourceDefinition.USE_JOURNAL_STORE, CommonAttributes.USE_HORNETQ_STORE)
                .addRename(TransactionSubsystemRootResourceDefinition.JOURNAL_STORE_ENABLE_ASYNC_IO, CommonAttributes.HORNETQ_STORE_ENABLE_ASYNC_IO)
                .addRename(TransactionSubsystemRootResourceDefinition.STATISTICS_ENABLED, CommonAttributes.ENABLE_STATISTICS)
                .end();

        // 1.5.0 --> 1.4.0
        ResourceTransformationDescriptionBuilder builderEap63 = chainedBuilder.createBuilder(MODEL_VERSION_EAP64, MODEL_VERSION_EAP63);
        builderEap63.rejectChildResource(CMResourceResourceDefinition.PATH_CM_RESOURCE);

        //1.4.0 --> 1.3.0
        chainedBuilder.createBuilder(MODEL_VERSION_EAP63, MODEL_VERSION_EAP62);

        chainedBuilder.buildAndRegister(subsystemRegistration, new ModelVersion[]{
                MODEL_VERSION_EAP62,
                MODEL_VERSION_EAP63,
                MODEL_VERSION_EAP64,
                MODEL_VERSION_EAP70,
                // v4_0_0
        });
    }
}
