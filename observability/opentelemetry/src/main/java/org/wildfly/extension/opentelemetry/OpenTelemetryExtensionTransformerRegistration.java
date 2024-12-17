package org.wildfly.extension.opentelemetry;

import static org.wildfly.extension.opentelemetry.OpenTelemetrySubsystemModel.VERSION_1_0_0;

import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.kohsuke.MetaInfServices;

@MetaInfServices
public class OpenTelemetryExtensionTransformerRegistration implements ExtensionTransformerRegistration {
    @Override
    public String getSubsystemName() {
        return OpenTelemetryConfigurationConstants.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration registration) {
        ChainedTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(registration.getCurrentSubsystemVersion());

        registerV_1_1_Transformers(builder.createBuilder(OpenTelemetrySubsystemModel.VERSION_1_1_0.getVersion(), VERSION_1_0_0.getVersion()));
    }

    private void registerV_1_1_Transformers(ResourceTransformationDescriptionBuilder builder) {
        builder.getAttributeBuilder()
                .setValueConverter(AttributeConverter.DEFAULT_VALUE, OpenTelemetrySubsystemRegistrar.EXPORTER)
                .end();
    }
}
