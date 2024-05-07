package org.wildfly.extension.micrometer.otlp;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.micrometer.registry.WildFlyCompositeRegistry;

public class OtlpRegistryAddHandler extends AbstractAddStepHandler {
    private final WildFlyCompositeRegistry wildFlyRegistry;

    // Does this need to be AtomicReference<T>, or can we just pass the Supplier<T>
    public OtlpRegistryAddHandler(WildFlyCompositeRegistry wildFlyRegistry) {
        this.wildFlyRegistry = wildFlyRegistry;
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
            throws OperationFailedException {
        String endpoint = OtlpRegistryDefinitionRegistrar.ENDPOINT.resolveModelAttribute(context, model).asStringOrNull();
        Long step = OtlpRegistryDefinitionRegistrar.STEP.resolveModelAttribute(context, model).asLong();

        wildFlyRegistry.addRegistry(new WildFlyOtlpRegistry(new WildFlyOtlpConfig(endpoint, step)));
    }
}
