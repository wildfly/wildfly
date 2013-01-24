package org.jboss.as.web;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.web.deployment.WarMetaDataProcessor;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * Attribute handler that modifies the subsystem without requiring any restart.
 *
 * @author navssurtani
 */
public class SymlinkingEnabledWriteAttributeHandler extends AbstractWriteAttributeHandler<Void> {

    private static final Logger logger = Logger.getLogger(SymlinkingEnabledWriteAttributeHandler.class);

    private final WarMetaDataProcessor processor;

    public SymlinkingEnabledWriteAttributeHandler (final WarMetaDataProcessor processor) {
        super(WebDefinition.SYMLINKING_ENABLED);
        this.processor = processor;
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                           ModelNode resolvedValue, ModelNode currentValue,
                                           HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        applyUpdateToDeploymentProcessor(resolvedValue, attributeName);
        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                         ModelNode valueToRestore, ModelNode valueToRevert,
                                         Void handback) throws OperationFailedException {
        applyUpdateToDeploymentProcessor(valueToRestore, attributeName);
    }

    private void applyUpdateToDeploymentProcessor (ModelNode newValue, String attribute) {
        if (ModelDescriptionConstants.SYMLINKING_ENABLED.equals(attribute)) {
            processor.setSymbolicEnabled(newValue.asBoolean());
        }
    }
}
