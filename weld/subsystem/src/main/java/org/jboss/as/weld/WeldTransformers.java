package org.jboss.as.weld;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;

public class WeldTransformers implements ExtensionTransformerRegistration {

    @Override
    public String getSubsystemName() {
        return WeldExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystem) {
        ModelVersion version4_0_0 = ModelVersion.create(4, 0, 0);

        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory
                .createChainedSubystemInstance(subsystem.getCurrentSubsystemVersion());

        // Differences between the current version and 4.0.0
        ResourceTransformationDescriptionBuilder builder400 = chainedBuilder.createBuilder(subsystem.getCurrentSubsystemVersion(), version4_0_0);
        builder400.getAttributeBuilder()
                // Discard an explicit 'true' as that's the legacy behavior. Reject otherwise.
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, ModelNode.TRUE),
                        WeldResourceDefinition.LEGACY_EMPTY_BEANS_XML_TREATMENT_ATTRIBUTE)
                .addRejectCheck(RejectAttributeChecker.ALL, WeldResourceDefinition.LEGACY_EMPTY_BEANS_XML_TREATMENT_ATTRIBUTE)
                .end();

        chainedBuilder.buildAndRegister(subsystem, new ModelVersion[]{version4_0_0});
    }
}
