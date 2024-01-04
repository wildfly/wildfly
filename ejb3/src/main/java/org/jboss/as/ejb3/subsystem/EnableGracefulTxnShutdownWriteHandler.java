/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.subsystem;

        import org.jboss.as.controller.AbstractWriteAttributeHandler;
        import org.jboss.as.controller.AttributeDefinition;
        import org.jboss.as.controller.OperationContext;
        import org.jboss.as.controller.OperationFailedException;
        import org.jboss.as.controller.PathAddress;
        import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
        import org.jboss.as.ejb3.suspend.EJBSuspendHandlerService;
        import org.jboss.dmr.ModelNode;
        import org.jboss.msc.service.ServiceRegistry;

/**
 * Write handler for enable graceful shutdown.
 *
 * @author Flavia Rainone
 */
public class EnableGracefulTxnShutdownWriteHandler extends AbstractWriteAttributeHandler<Void> {

    public static final EnableGracefulTxnShutdownWriteHandler INSTANCE = new EnableGracefulTxnShutdownWriteHandler(
            EJB3SubsystemRootResourceDefinition.ENABLE_GRACEFUL_TXN_SHUTDOWN);

    private final AttributeDefinition gracefulTxnShutdownAttribute;

    /**
     * @param attributes the attributes associated with the passivation-store resource, starting with max-size
     */
    EnableGracefulTxnShutdownWriteHandler(AttributeDefinition... attributes) {
        super(attributes);
        // enable graceful
        this.gracefulTxnShutdownAttribute = attributes[0];
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        this.applyModelToRuntime(context, operation, attributeName, model);
        return false;
    }

    private void applyModelToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode model) throws OperationFailedException {
        String name = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        ServiceRegistry registry = context.getServiceRegistry(true);
        EJBSuspendHandlerService service = (EJBSuspendHandlerService) registry.getRequiredService(EJBSuspendHandlerService.SERVICE_NAME).getValue();
        if (service!= null && this.gracefulTxnShutdownAttribute.getName().equals(attributeName)) {
            boolean enableGracefulTxnShutdown = this.gracefulTxnShutdownAttribute.resolveModelAttribute(context, model)
                    .asBoolean();
            service.enableGracefulTxnShutdown(enableGracefulTxnShutdown);
        }
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
        restored.get(attributeName).set(valueToRestore);
        this.applyModelToRuntime(context, operation, attributeName, restored);
    }
}