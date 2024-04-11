package org.wildfly.extension.micrometer.prometheus;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.micrometer.registry.WildFlyCompositeRegistry;

public class PrometheusRegistryAddHandler extends AbstractAddStepHandler {
    private final WildFlyCompositeRegistry wildFlyRegistry;

    // Does this need to be AtomicReference<T>, or can we just pass the Supplier<T>
    public PrometheusRegistryAddHandler(WildFlyCompositeRegistry wildFlyRegistry) {
        this.wildFlyRegistry = wildFlyRegistry;
    }

    @Override
    protected void performRuntime(OperationContext operationContext, ModelNode operation, ModelNode model)
            throws OperationFailedException {
        WildFlyPrometheusRegistry registry = new WildFlyPrometheusRegistry();

        String context = PrometheusRegistryDefinitionRegistrar.CONTEXT.resolveModelAttribute(operationContext, model).asStringOrNull();
        Boolean securityEnabled = PrometheusRegistryDefinitionRegistrar.SECURITY_ENABLED.resolveModelAttribute(operationContext, model).asBooleanOrNull();

        PrometheusContextService.install(operationContext, registry, context, securityEnabled);

        wildFlyRegistry.addRegistry(registry);
    }
}
