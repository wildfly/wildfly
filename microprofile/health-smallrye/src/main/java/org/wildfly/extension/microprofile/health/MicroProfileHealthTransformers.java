package org.wildfly.extension.microprofile.health;

import static org.jboss.as.controller.transform.description.RejectAttributeChecker.DEFINED;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.kohsuke.MetaInfServices;

@MetaInfServices
public class MicroProfileHealthTransformers implements ExtensionTransformerRegistration {
    @Override
    public String getSubsystemName() {
        return MicroProfileHealthExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration registration) {
        ChainedTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(registration.getCurrentSubsystemVersion());

        registerTransformers_WildFly_17(builder.createBuilder(MicroProfileHealthExtension.VERSION_2_0_0, MicroProfileHealthExtension.VERSION_1_0_0));

        builder.buildAndRegister(registration, new ModelVersion[] { MicroProfileHealthExtension.VERSION_1_0_0});
    }

    private void registerTransformers_WildFly_17(ResourceTransformationDescriptionBuilder subsystem) {
        rejectDefinedAttributeWithDefaultValue(subsystem, MicroProfileHealthSubsystemDefinition.EMPTY_LIVENESS_CHECKS_STATUS);
        rejectDefinedAttributeWithDefaultValue(subsystem, MicroProfileHealthSubsystemDefinition.EMPTY_READINESS_CHECKS_STATUS);
    }

    /**
     * Reject the attributes if they are defined or discard them if they are undefined or set to their default value.
     */
    private static void rejectDefinedAttributeWithDefaultValue(ResourceTransformationDescriptionBuilder builder, AttributeDefinition... attrs) {
        for (AttributeDefinition attr : attrs) {
            builder.getAttributeBuilder()
                    .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(attr.getDefaultValue()), attr)
                    .addRejectCheck(DEFINED, attr);
        }
    }

}
