/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.subsystem;

import static org.jboss.as.txn.subsystem.TransactionExtension.CURRENT_MODEL_VERSION;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.TRANSACTIONS_RECOVERY_GRACEFUL_SHUTDOWN;

import java.util.Map;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

public class TransactionTransformers implements ExtensionTransformerRegistration {

    static final ModelVersion MODEL_VERSION_WILDFLY41 = ModelVersion.create(7, 0);
    static final ModelVersion MODEL_VERSION_EAP81 = ModelVersion.create(6, 0);

    @Override
    public String getSubsystemName() {
        return TransactionExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {
        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(CURRENT_MODEL_VERSION);

        // 7.1.0 --> 7.0.0:
        // transactions-recovery-graceful-shutdown changed type from enum to int.
        //   0  (skip, default) → discard; old slave uses its default "ignore"
        //   -1 (wait indefinitely) → convert int to string "wait" (same attribute name)
        //   >0 (timed wait) → reject; no timeout concept in 7.0 model
        ResourceTransformationDescriptionBuilder builder41 = chainedBuilder.createBuilder(CURRENT_MODEL_VERSION, MODEL_VERSION_WILDFLY41);
        builder41.getAttributeBuilder()
            .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, TRANSACTIONS_RECOVERY_GRACEFUL_SHUTDOWN)
            .addRejectCheck(new RejectAttributeChecker.DefaultRejectAttributeChecker() {
                @Override
                public boolean rejectAttribute(PathAddress address, String name, ModelNode value, TransformationContext context) {
                    if (!value.isDefined()) return false;
                    // expressions cannot be evaluated at transform time — reject them
                    if (value.getType() == ModelType.EXPRESSION) return true;
                    return value.asInt() > 0;
                }
                @Override
                public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
                    return "transactions-recovery-graceful-shutdown with a positive value is not supported in older management model versions (7.0). Use 0 (skip) or -1 (wait indefinitely).";
                }
            }, TRANSACTIONS_RECOVERY_GRACEFUL_SHUTDOWN)
            .setValueConverter(new AttributeConverter.DefaultAttributeConverter() {
                @Override
                protected void convertAttribute(PathAddress address, String name, ModelNode value, TransformationContext context) {
                    // Only -1 reaches here (0 was discarded, >0 was rejected); convert int to the old enum string
                    value.set("wait");
                }
            }, TRANSACTIONS_RECOVERY_GRACEFUL_SHUTDOWN)
            .end();

        // 7.0.0 --> 6.0.0:
        // transactions-recovery-graceful-shutdown did not exist in EAP 8.1 (6.0 model).
        // After the 7.1→7.0 step, the attribute is either absent (0 was discarded) or "wait" (-1 was converted).
        // Reject if present (only "wait" can appear here).
        ResourceTransformationDescriptionBuilder builder81 = chainedBuilder.createBuilder(MODEL_VERSION_WILDFLY41, MODEL_VERSION_EAP81);
        builder81.getAttributeBuilder()
            .addRejectCheck(RejectAttributeChecker.DEFINED, TRANSACTIONS_RECOVERY_GRACEFUL_SHUTDOWN)
            .end();

        chainedBuilder.buildAndRegister(subsystemRegistration, new ModelVersion[]{MODEL_VERSION_WILDFLY41, MODEL_VERSION_EAP81});
    }
}
