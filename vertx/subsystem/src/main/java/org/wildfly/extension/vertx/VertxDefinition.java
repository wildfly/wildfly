package org.wildfly.extension.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.DynamicNameMappers;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

import java.util.Collection;

import static org.wildfly.extension.vertx.logging.VertxLogger.VERTX_LOGGER;

/**
 * Each VertxDefinition represents a Vert.x instance.
 *
 * It has most of configurations as attributes, but some are children resources, like: EventBusOptions, MetricsOptions,
 * AddressResolverOptions, FileSystemOptions, etc.
 *
 * Each attribute / child update won't take effect until the operation: <code>refresh</code> is called.
 */
public class VertxDefinition extends PersistentResourceDefinition {

    static final String VERTX_CAPABILITY_NAME = "org.wildfly.extension.vertx";

    static final RuntimeCapability<Void> VERTX_RUNTIME_CAPABILITY =
            RuntimeCapability.Builder.of(VERTX_CAPABILITY_NAME, true, VertxProxy.class)
                    .setDynamicNameMapper(DynamicNameMappers.PARENT)
                    .build();

    static VertxDefinition INSTANCE = new VertxDefinition();

    VertxDefinition() {
        super(new SimpleResourceDefinition.Parameters(PathElement.pathElement("vertx"),
                VertxSubsystemExtension.getResourceDescriptionResolver(VertxSubsystemExtension.SUBSYSTEM_NAME))
                .setAddHandler(new VertxResourceAdd())
                .setRemoveHandler(new VertxResourceRemove())
                .setCapabilities(VERTX_RUNTIME_CAPABILITY)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return VertxAttributes.getSimpleAttributes();
    }

    static class VertxResourceAdd extends AbstractAddStepHandler {
        VertxResourceAdd() {
            super(new Parameters()
                    .addAttribute(VertxAttributes.getSimpleAttributes())
            );
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            final String name = context.getCurrentAddressValue();
            final String jndiName = VertxAttributes.JNDI_NAME.resolveModelAttribute(context, operation).asString();
            final String vertxOptionsFile = VertxAttributes.VERTX_OPTIONS_FILE.resolveModelAttribute(context, operation).asString();
            final VertxProxy vertxProxy = new VertxProxy();
            vertxProxy.setJndiName(jndiName);
            vertxProxy.setName(name);
            final VertxOptions vertxOptions;
            if (vertxOptionsFile == null) {
                vertxOptions = new VertxOptions();
            } else {
                JsonObject json = readJsonFromFile(vertxOptionsFile);
                vertxProxy.setVertxOptionsFile(vertxOptionsFile);
                vertxOptions = new VertxOptions(json);
            }
            Vertx coreVertx = Vertx.vertx(vertxOptions);
            VertxDelegate vertxDelegate = new VertxDelegate(coreVertx);
            vertxProxy.setVertx(vertxDelegate);
            VertxRegistry.INSTANCE.registerVertx(vertxProxy);
        }

        private JsonObject readJsonFromFile(String vertxOptionsFile) throws OperationFailedException {
            //TODO compose it using the specified options file
            return new JsonObject();
        }
    }

    static class VertxResourceRemove extends ReloadRequiredRemoveStepHandler {
        @Override
        protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            final String vertxName = context.getCurrentAddressValue();
            VertxRegistry.INSTANCE.unRegister(vertxName);
            super.performRemove(context, operation, model);
        }

        @Override
        protected boolean requiresRuntime(OperationContext context) {
            return context.getProcessType() != ProcessType.EMBEDDED_SERVER || context.getRunningMode() != RunningMode.ADMIN_ONLY;
        }

    }

}
