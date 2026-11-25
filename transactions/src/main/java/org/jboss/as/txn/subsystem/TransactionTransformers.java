/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.subsystem;

import static org.jboss.as.txn.subsystem.TransactionExtension.CURRENT_MODEL_VERSION;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.TRANSACTIONS_RECOVERY_GRACEFUL_SHUTDOWN;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

public class TransactionTransformers implements ExtensionTransformerRegistration {

    static final ModelVersion MODEL_VERSION_EAP81 = ModelVersion.create(6, 0);

    @Override
    public String getSubsystemName() {
        return TransactionExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {
        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(CURRENT_MODEL_VERSION);

        // 7.0.0 --> 6.0.0
        ResourceTransformationDescriptionBuilder builder81 = chainedBuilder.createBuilder(CURRENT_MODEL_VERSION, MODEL_VERSION_EAP81);
        builder81.getAttributeBuilder()
            .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, TRANSACTIONS_RECOVERY_GRACEFUL_SHUTDOWN)
            .addRejectCheck(RejectAttributeChecker.DEFINED, TRANSACTIONS_RECOVERY_GRACEFUL_SHUTDOWN)
            .end();

        chainedBuilder.buildAndRegister(subsystemRegistration, new ModelVersion[]{MODEL_VERSION_EAP81});
    }
}
